package cm.kamerbiz.catalog;

import cm.kamerbiz.business.BusinessMembershipAuthorizationService;
import cm.kamerbiz.business.BusinessRole;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ProductService {
  private static final Logger LOGGER = LoggerFactory.getLogger(ProductService.class);
  private static final Set<BusinessRole> VIEWERS = Set.of(BusinessRole.values());
  private static final Set<BusinessRole> EDITORS = Set.of(BusinessRole.OWNER, BusinessRole.MANAGER);

  private final ProductRepository products;
  private final ProductImportBatchRepository importBatches;
  private final ProductImageCleanupRepository imageCleanups;
  private final BusinessMembershipAuthorizationService authorization;
  private final ProductImageService images;

  public ProductService(
      ProductRepository products,
      ProductImportBatchRepository importBatches,
      ProductImageCleanupRepository imageCleanups,
      BusinessMembershipAuthorizationService authorization,
      ProductImageService images) {
    this.products = products;
    this.importBatches = importBatches;
    this.imageCleanups = imageCleanups;
    this.authorization = authorization;
    this.images = images;
  }

  @Transactional(readOnly = true)
  public List<ProductResponse> list(Jwt jwt, UUID businessId, boolean includeArchived) {
    authorization.requireMembership(jwt, businessId, VIEWERS);
    List<Product> result =
        includeArchived
            ? products.findAllByBusinessIdOrderByUpdatedAtDesc(businessId)
            : products.findAllByBusinessIdAndArchivedOrderByUpdatedAtDesc(businessId, false);
    return result.stream().map(ProductResponse::from).toList();
  }

  @Transactional(readOnly = true)
  public ImportPreviewResponse previewCsv(Jwt jwt, UUID businessId, String csv) {
    authorization.requireMembership(jwt, businessId, EDITORS);
    if (csv == null || csv.isBlank() || csv.length() > 2_000_000) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "CSV must be nonempty and smaller than 2 MB.");
    }

    List<List<String>> rows;
    try {
      rows = ProductCsvParser.parse(csv);
    } catch (IllegalArgumentException exception) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage());
    }
    if (rows.size() < 2 || rows.size() > 5001) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "CSV must contain a header and between 1 and 5000 product rows.");
    }

    Map<String, Integer> columns = new HashMap<>();
    for (int i = 0; i < rows.get(0).size(); i++) {
      String header = rows.get(0).get(i).trim().toLowerCase(Locale.ROOT);
      if (header.isBlank() || columns.putIfAbsent(header, i) != null) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST, "CSV contains an empty or duplicate column name.");
      }
    }
    if (!columns.containsKey("name") || !columns.containsKey("price")) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "CSV requires name and price columns.");
    }

    List<String> errors = new ArrayList<>();
    Set<String> codesInFile = new HashSet<>();
    int valid = 0;
    for (int index = 1; index < rows.size(); index++) {
      List<String> row = rows.get(index);
      try {
        String name = cell(row, columns, "name").trim();
        BigDecimal price = new BigDecimal(cell(row, columns, "price"));
        String stockValue = optionalCell(row, columns, "stock_quantity");
        int stock = stockValue.isBlank() ? 0 : Integer.parseInt(stockValue);
        if (name.isBlank() || price.compareTo(BigDecimal.ZERO) <= 0 || stock < 0) {
          throw new IllegalArgumentException(
              "Name, positive price and nonnegative stock are required.");
        }
        String code = normalizeCode(optionalCell(row, columns, "product_code"));
        imageFilename(row, columns);
        rejectExternalImageUrl(row, columns);
        if (!code.isBlank()
            && (!codesInFile.add(code)
                || products.existsByBusinessIdAndProductCode(businessId, code))) {
          throw new IllegalArgumentException("Product code already exists.");
        }
        valid++;
      } catch (IllegalArgumentException exception) {
        errors.add("Row " + (index + 1) + ": " + exception.getMessage());
      }
    }
    return new ImportPreviewResponse(rows.size() - 1, valid, errors.size(), errors);
  }

  @Transactional
  public ImportResponse importCsv(Jwt jwt, UUID businessId, String batchKey, String csv) {
    authorization.requireMembership(jwt, businessId, EDITORS);
    if (csv == null || csv.isBlank() || csv.length() > 2_000_000) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "CSV must be nonempty and smaller than 2 MB.");
    }
    if (batchKey == null || batchKey.isBlank() || batchKey.length() > 100) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "A valid import batch key is required.");
    }

    var previous = importBatches.findByBusinessIdAndBatchKey(businessId, batchKey);
    if (previous.isPresent()) {
      return new ImportResponse(
          previous.get().getCreatedCount(), previous.get().getSkippedCount(), true, List.of());
    }
    int claimed = importBatches.claimBatch(UUID.randomUUID(), businessId, batchKey);
    if (claimed == 0) {
      var completed = importBatches.findByBusinessIdAndBatchKey(businessId, batchKey).orElseThrow();
      return new ImportResponse(
          completed.getCreatedCount(), completed.getSkippedCount(), true, List.of());
    }

    List<List<String>> rows;
    try {
      rows = ProductCsvParser.parse(csv);
    } catch (IllegalArgumentException exception) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage());
    }
    if (rows.size() < 2 || rows.size() > 5001) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "CSV must contain a header and between 1 and 5000 product rows.");
    }

    Map<String, Integer> columns = new HashMap<>();
    for (int i = 0; i < rows.get(0).size(); i++) {
      String header = rows.get(0).get(i).trim().toLowerCase(Locale.ROOT);
      if (header.isBlank() || columns.putIfAbsent(header, i) != null) {
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST, "CSV contains an empty or duplicate column name.");
      }
    }
    if (!columns.containsKey("name") || !columns.containsKey("price")) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "CSV requires name and price columns.");
    }

    List<String> errors = new ArrayList<>();
    Set<String> codesInFile = new HashSet<>();
    int created = 0;
    int skipped = 0;
    for (int index = 1; index < rows.size(); index++) {
      List<String> row = rows.get(index);
      Product product = null;
      String rowError = null;
      try {
        String name = cell(row, columns, "name");
        BigDecimal price = new BigDecimal(cell(row, columns, "price"));
        String stockValue = optionalCell(row, columns, "stock_quantity");
        int stock = stockValue.isBlank() ? 0 : Integer.parseInt(stockValue);
        if (name.isBlank() || price.compareTo(BigDecimal.ZERO) <= 0 || stock < 0) {
          throw new IllegalArgumentException(
              "Name, positive price and nonnegative stock are required.");
        }

        String code = normalizeCode(optionalCell(row, columns, "product_code"));
        code = code.isBlank() ? generatedCode() : code;
        if (!codesInFile.add(code) || products.existsByBusinessIdAndProductCode(businessId, code)) {
          throw new IllegalArgumentException("Product code already exists.");
        }
        rejectExternalImageUrl(row, columns);
        product =
            new Product(
                businessId,
                code,
                name,
                optionalCell(row, columns, "description"),
                optionalCell(row, columns, "category"),
                price,
                stock,
                null);
        String imageFilename = imageFilename(row, columns);
        if (imageFilename != null) {
          product.setImageFilename(imageFilename);
        }
      } catch (IllegalArgumentException exception) {
        rowError = exception.getMessage();
      }

      if (product == null) {
        skipped++;
        errors.add("Row " + (index + 1) + ": " + rowError);
      } else {
        products.save(product);
        created++;
      }
    }

    ProductImportBatch batch =
        importBatches.findByBusinessIdAndBatchKey(businessId, batchKey).orElseThrow();
    batch.setCounts(created, skipped);
    return new ImportResponse(created, skipped, false, errors);
  }

  private String cell(List<String> row, Map<String, Integer> columns, String name) {
    Integer index = columns.get(name);
    return index == null || index >= row.size() ? "" : row.get(index);
  }

  private String optionalCell(List<String> row, Map<String, Integer> columns, String name) {
    return cell(row, columns, name).trim();
  }

  @Transactional
  public ProductResponse create(Jwt jwt, UUID businessId, ProductRequest request) {
    authorization.requireMembership(jwt, businessId, EDITORS);
    validate(request);
    validateImageReference(jwt, businessId, request.imageUrl());
    String code =
        request.productCode() == null || request.productCode().isBlank()
            ? generatedCode()
            : normalizeCode(request.productCode());
    if (products.existsByBusinessIdAndProductCode(businessId, code)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Product code already exists.");
    }
    Product product =
        products.save(
            new Product(
                businessId,
                code,
                request.name().trim(),
                clean(request.description()),
                clean(request.category()),
                request.price(),
                request.stockQuantity(),
                clean(request.imageUrl())));
    return ProductResponse.from(product);
  }

  @Transactional
  public ProductResponse update(Jwt jwt, UUID businessId, UUID productId, ProductRequest request) {
    authorization.requireMembership(jwt, businessId, EDITORS);
    validate(request);
    validateImageReference(jwt, businessId, request.imageUrl());
    Product product = findProduct(businessId, productId);
    if (request.productCode() != null
        && !request.productCode().isBlank()
        && !normalizeCode(request.productCode()).equals(product.getProductCode())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Product code cannot be changed.");
    }
    String previousImageUrl = product.getImageUrl();
    String nextImageUrl = clean(request.imageUrl());
    product.update(
        request.name().trim(),
        clean(request.description()),
        clean(request.category()),
        request.price(),
        request.stockQuantity(),
        nextImageUrl);
    Product saved = products.save(product);
    enqueueReplacedImageCleanup(businessId, previousImageUrl, nextImageUrl);
    return ProductResponse.from(saved);
  }

  @Transactional
  public ProductResponse setImageUrl(Jwt jwt, UUID businessId, UUID productId, String imageUrl) {
    authorization.requireMembership(jwt, businessId, EDITORS);
    if (imageUrl == null || imageUrl.isBlank() || imageUrl.length() > 1000) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A valid image URL is required.");
    }
    images.validateOwnedUrl(jwt, businessId, imageUrl);
    Product product = findProduct(businessId, productId);
    String previousImageUrl = product.getImageUrl();
    product.update(
        product.getName(),
        product.getDescription(),
        product.getCategory(),
        product.getPrice(),
        product.getStockQuantity(),
        imageUrl);
    Product saved = products.save(product);
    enqueueReplacedImageCleanup(businessId, previousImageUrl, imageUrl);
    return ProductResponse.from(saved);
  }

  @Transactional
  public ProductResponse setArchived(Jwt jwt, UUID businessId, UUID productId, boolean archived) {
    authorization.requireMembership(jwt, businessId, EDITORS);
    Product product = findProduct(businessId, productId);
    product.setArchived(archived);
    return ProductResponse.from(products.save(product));
  }

  private Product findProduct(UUID businessId, UUID productId) {
    return products
        .findByIdAndBusinessId(productId, businessId)
        .orElseThrow(() -> new AccessDeniedException("Product access denied."));
  }

  private void validate(ProductRequest request) {
    if (request.price() == null || request.price().compareTo(BigDecimal.ZERO) <= 0) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Price must be greater than zero.");
    }
    if (request.stockQuantity() < 0) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Stock cannot be negative.");
    }
  }

  private String normalizeCode(String code) {
    return code == null ? "" : code.trim().toUpperCase(Locale.ROOT);
  }

  public boolean isImageInUse(UUID businessId, String imageUrl) {
    return products.existsByBusinessIdAndImageUrl(businessId, imageUrl);
  }

  private void enqueueReplacedImageCleanup(
      UUID businessId, String previousImageUrl, String nextImageUrl) {
    if (previousImageUrl == null
        || previousImageUrl.isBlank()
        || previousImageUrl.equals(nextImageUrl)) {
      return;
    }
    imageCleanups.enqueue(UUID.randomUUID(), businessId, previousImageUrl);
  }

  private void validateImageReference(Jwt jwt, UUID businessId, String imageUrl) {
    if (imageUrl != null && !imageUrl.isBlank()) {
      images.validateOwnedUrl(jwt, businessId, imageUrl);
    }
  }

  private void rejectExternalImageUrl(List<String> row, Map<String, Integer> columns) {
    if (!optionalCell(row, columns, "image_url").isBlank()) {
      throw new IllegalArgumentException(
          "Upload product images after importing products. Use image_filename to match files.");
    }
  }

  private String imageFilename(List<String> row, Map<String, Integer> columns) {
    String filename = optionalCell(row, columns, "image_filename");
    if (filename.isBlank()) return null;
    if (filename.length() > 255
        || filename.contains("/")
        || filename.contains("\\")
        || filename.equals(".")
        || filename.equals("..")) {
      throw new IllegalArgumentException(
          "Image filename must be a simple filename under 255 characters.");
    }
    return filename;
  }

  private String generatedCode() {
    return "KB-" + UUID.randomUUID().toString().toUpperCase(Locale.ROOT);
  }

  private String clean(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  public record ImportPreviewResponse(
      int totalRows, int validRows, int rejectedRows, List<String> errors) {}

  public record ImportResponse(
      int createdCount, int skippedCount, boolean replayed, List<String> errors) {}

  public record ProductRequest(
      @Size(max = 64) String productCode,
      @NotBlank @Size(max = 255) String name,
      @Size(max = 2000) String description,
      @Size(max = 120) String category,
      @NotNull @DecimalMin(value = "0.00", inclusive = false) BigDecimal price,
      @PositiveOrZero int stockQuantity,
      @Size(max = 1000) String imageUrl) {}

  public record ProductResponse(
      UUID id,
      String productCode,
      String name,
      String description,
      String category,
      BigDecimal price,
      int stockQuantity,
      String imageUrl,
      String imageFilename,
      boolean archived,
      Instant createdAt,
      Instant updatedAt) {
    static ProductResponse from(Product product) {
      return new ProductResponse(
          product.getId(),
          product.getProductCode(),
          product.getName(),
          product.getDescription(),
          product.getCategory(),
          product.getPrice(),
          product.getStockQuantity(),
          product.getImageUrl(),
          product.getImageFilename(),
          product.isArchived(),
          product.getCreatedAt(),
          product.getUpdatedAt());
    }
  }
}
