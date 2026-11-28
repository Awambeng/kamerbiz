package cm.kamerbiz.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import cm.kamerbiz.business.BusinessMembershipAuthorizationService;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

class ProductImageServiceTest {
  private final S3Client storage = mock(S3Client.class);
  private final BusinessMembershipAuthorizationService authorization =
      mock(BusinessMembershipAuthorizationService.class);
  private final ProductImageCleanupRepository cleanups = mock(ProductImageCleanupRepository.class);
  private final UUID businessId = UUID.randomUUID();
  private ProductImageService service;

  @BeforeEach
  void setUp() {
    service =
        new ProductImageService(
            storage, "products", "https://cdn.example.com/products", authorization, cleanups);
  }

  @Test
  void rejectsUnsupportedImageContentTypeBeforeStorage() {
    MockMultipartFile file =
        new MockMultipartFile("file", "script.svg", "image/svg+xml", "<svg/>".getBytes());

    assertThatThrownBy(() -> service.upload(jwt(), businessId, file))
        .isInstanceOf(ResponseStatusException.class);
    verify(storage, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
  }

  @Test
  void rejectsImageWithInvalidSignature() {
    MockMultipartFile file =
        new MockMultipartFile("file", "fake.png", "image/png", "not an image".getBytes());

    assertThatThrownBy(() -> service.upload(jwt(), businessId, file))
        .isInstanceOf(ResponseStatusException.class);
    verify(storage, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
  }

  @Test
  void rejectsFilesLargerThanFiveMegabytes() {
    MockMultipartFile file =
        new MockMultipartFile("file", "large.jpg", "image/jpeg", new byte[5 * 1024 * 1024 + 1]);

    assertThatThrownBy(() -> service.upload(jwt(), businessId, file))
        .isInstanceOf(ResponseStatusException.class);
    verify(storage, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
  }

  @Test
  void uploadsValidPngToBusinessScopedObjectKey() {
    byte[] pngSignature =
        Base64.getDecoder()
            .decode(
                "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+/pWQAAAAASUVORK5CYII=");
    MockMultipartFile file =
        new MockMultipartFile("file", "product.png", "image/png", pngSignature);

    var uploaded = service.upload(jwt(), businessId, file);

    assertThat(uploaded.filename()).isEqualTo("product.png");
    assertThat(uploaded.imageUrl()).contains("/businesses/" + businessId + "/products/");
    verify(storage).putObject(any(PutObjectRequest.class), any(RequestBody.class));
  }

  @Test
  void validatesEveryBulkImageBeforeWritingAnyObject() {
    byte[] validPng =
        Base64.getDecoder()
            .decode(
                "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+/pWQAAAAASUVORK5CYII=");
    MockMultipartFile valid = new MockMultipartFile("files", "product.png", "image/png", validPng);
    MockMultipartFile invalid =
        new MockMultipartFile("files", "bad.png", "image/png", "not an image".getBytes());

    assertThatThrownBy(() -> service.uploadMany(jwt(), businessId, List.of(valid, invalid)))
        .isInstanceOf(ResponseStatusException.class);
    verify(storage, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
  }

  @Test
  void rejectsImageUrlOwnedByAnotherBusiness() {
    UUID otherBusiness = UUID.randomUUID();
    String foreignUrl =
        "https://cdn.example.com/products/businesses/" + otherBusiness + "/products/image.png";

    assertThatThrownBy(() -> service.validateOwnedUrl(jwt(), businessId, foreignUrl))
        .isInstanceOf(ResponseStatusException.class);
  }

  @Test
  void queuesImageCleanupWhenObjectStorageDeletionFails() {
    UUID imageId = UUID.randomUUID();
    String imageUrl =
        "https://cdn.example.com/products/businesses/"
            + businessId
            + "/products/"
            + imageId
            + ".png";
    doThrow(new IllegalStateException("Storage unavailable"))
        .when(storage)
        .deleteObject(any(DeleteObjectRequest.class));

    service.deleteOwnedUrl(jwt(), businessId, imageUrl);

    verify(cleanups)
        .enqueue(
            any(UUID.class),
            org.mockito.ArgumentMatchers.eq(businessId),
            org.mockito.ArgumentMatchers.eq(imageUrl));
  }

  private Jwt jwt() {
    Instant now = Instant.now();
    return Jwt.withTokenValue("test-token")
        .header("alg", "none")
        .issuer("http://localhost:8081/realms/kamerbiz")
        .subject("owner-one")
        .issuedAt(now)
        .expiresAt(now.plusSeconds(300))
        .build();
  }
}
