package cm.kamerbiz.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import cm.kamerbiz.business.BusinessMembershipAuthorizationService;
import cm.kamerbiz.business.BusinessRole;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ResponseStatusException;

class ProductServiceTest {
  private ProductRepository repository;
  private BusinessMembershipAuthorizationService authorization;
  private ProductImportBatchRepository importBatches;
  private ProductImageCleanupRepository imageCleanups;
  private ProductImageService images;
  private ProductService service;
  private final UUID businessId = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    repository = mock(ProductRepository.class);
    authorization = mock(BusinessMembershipAuthorizationService.class);
    importBatches = mock(ProductImportBatchRepository.class);
    imageCleanups = mock(ProductImageCleanupRepository.class);
    images = mock(ProductImageService.class);
    service = new ProductService(repository, importBatches, imageCleanups, authorization, images);
  }

  @Test
  void generatesProductCodeWhenVendorDoesNotProvideOne() {
    when(repository.saveAndFlush(any(Product.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    var response =
        service.create(
            jwt(),
            businessId,
            new ProductService.ProductRequest(
                null, "Blue Jeans", null, null, new BigDecimal("12000"), 5, null));
    assertThat(response.productCode()).startsWith("KB-");
    assertThat(response.name()).isEqualTo("Blue Jeans");
  }

  @Test
  void rejectsDuplicateBusinessProductCode() {
    when(repository.existsByBusinessIdAndProductCode(businessId, "JEAN-1")).thenReturn(true);
    assertThatThrownBy(() ->
            service.create(
                jwt(),
                businessId,
                new ProductService.ProductRequest(
                    "JEAN-1", "Blue Jeans", null, null, new BigDecimal("12000"), 5, null)))
        .isInstanceOf(ResponseStatusException.class)
        .extracting("statusCode")
        .isEqualTo(HttpStatus.CONFLICT);
  }

  @Test
  void rejectsImageUrlOutsideTheBusinessStorageOnCreate() {
    String externalUrl = "https://other.example/image.jpg";
    doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST))
        .when(images)
        .validateOwnedUrl(jwt(), businessId, externalUrl);

    assertThatThrownBy(() ->
            service.create(
                jwt(),
                businessId,
                new ProductService.ProductRequest(
                    null, "Blue Jeans", null, null, new BigDecimal("12000"), 5, externalUrl)))
        .isInstanceOf(ResponseStatusException.class);
  }

  @Test
  void deniesStaffFromCreatingProducts() {
    doThrow(new org.springframework.security.access.AccessDeniedException("denied"))
        .when(authorization)
        .requireMembership(
            any(Jwt.class), eq(businessId), eq(Set.of(BusinessRole.OWNER, BusinessRole.MANAGER)));
    assertThatThrownBy(() ->
            service.create(
                jwt(),
                businessId,
                new ProductService.ProductRequest(
                    null, "Blue Jeans", null, null, new BigDecimal("12000"), 5, null)))
        .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
  }

  @Test
  void previewsCsvAndReportsInvalidRowsWithoutCreatingProducts() {
    when(repository.existsByBusinessIdAndProductCode(businessId, "JEAN-1")).thenReturn(false);
    var preview =
        service.previewCsv(
            jwt(),
            businessId,
            "product_code,name,price,stock_quantity\nJEAN-1,Blue Jeans,12000,5\n,Bad Product,-1,2");
    assertThat(preview.totalRows()).isEqualTo(2);
    assertThat(preview.validRows()).isEqualTo(1);
    assertThat(preview.rejectedRows()).isEqualTo(1);
    assertThat(preview.errors()).hasSize(1);
    verify(repository, never()).save(any(Product.class));
  }

  @Test
  void rejectsOversizedCsvBeforeParsing() {
    assertThatThrownBy(() -> service.previewCsv(jwt(), businessId, "x".repeat(2_000_001)))
        .isInstanceOf(ResponseStatusException.class)
        .extracting("statusCode")
        .isEqualTo(HttpStatus.BAD_REQUEST);
  }

  @Test
  void importsProductsAndStoresImageFilenameMapping() {
    ProductImportBatch batch = new ProductImportBatch(businessId, "batch-1", 0, 0);
    when(importBatches.findByBusinessIdAndBatchKey(businessId, "batch-1"))
        .thenReturn(Optional.empty(), Optional.of(batch));
    when(importBatches.claimBatch(any(UUID.class), eq(businessId), eq("batch-1"))).thenReturn(1);
    when(repository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

    var response =
        service.importCsv(
            jwt(),
            businessId,
            "batch-1",
            "product_code,name,price,stock_quantity,image_filename\n"
                + "JEAN-2,Blue Jeans,12000,5,jeans-blue.jpg");

    assertThat(response.createdCount()).isEqualTo(1);
    assertThat(batch.getCreatedCount()).isEqualTo(1);
    assertThat(batch.getSkippedCount()).isZero();
    verify(importBatches).claimBatch(any(UUID.class), eq(businessId), eq("batch-1"));
    org.mockito.ArgumentCaptor<Product> saved = org.mockito.ArgumentCaptor.forClass(Product.class);
    verify(repository).save(saved.capture());
    assertThat(saved.getValue().getImageFilename()).isEqualTo("jeans-blue.jpg");
  }

  @Test
  void returnsExistingImportResultForRepeatedBatchKey() {
    ProductImportBatch previous = new ProductImportBatch(businessId, "batch-2", 3, 1);
    when(importBatches.findByBusinessIdAndBatchKey(businessId, "batch-2"))
        .thenReturn(Optional.of(previous));

    var response = service.importCsv(jwt(), businessId, "batch-2", "ignored");

    assertThat(response.replayed()).isTrue();
    assertThat(response.createdCount()).isEqualTo(3);
    assertThat(response.skippedCount()).isEqualTo(1);
    verify(importBatches, never()).claimBatch(any(UUID.class), eq(businessId), eq("batch-2"));
  }

  @Test
  void rejectsUnsafeImageFilenameDuringCsvPreview() {
    var preview =
        service.previewCsv(
            jwt(), businessId, "name,price,image_filename\nBlue Jeans,12000,../outside.jpg");

    assertThat(preview.validRows()).isZero();
    assertThat(preview.rejectedRows()).isEqualTo(1);
  }

  @Test
  void reportsConflictWhenProductCodeCollidesDuringFlush() {
    when(repository.saveAndFlush(any(Product.class)))
        .thenThrow(new DataIntegrityViolationException("duplicate key"));

    assertThatThrownBy(() ->
            service.create(
                jwt(),
                businessId,
                new ProductService.ProductRequest(
                    "JEAN-3", "Blue Jeans", null, null, new BigDecimal("12000"), 5, null)))
        .isInstanceOf(ResponseStatusException.class)
        .extracting("statusCode")
        .isEqualTo(HttpStatus.CONFLICT);
  }

  @Test
  void rejectsCsvRowsThatExceedDatabaseLimits() {
    var preview =
        service.previewCsv(
            jwt(), businessId, "name,price\n" + "x".repeat(300) + ",12000\nBlue Jeans,0.001\n");

    assertThat(preview.validRows()).isZero();
    assertThat(preview.rejectedRows()).isEqualTo(2);
    assertThat(preview.errors())
        .anyMatch(error -> error.contains("255"))
        .anyMatch(error -> error.contains("2 decimal places"));
  }

  private Jwt jwt() {
    return Jwt.withTokenValue("test")
        .header("alg", "none")
        .issuer("https://identity.example")
        .subject("user")
        .build();
  }
}
