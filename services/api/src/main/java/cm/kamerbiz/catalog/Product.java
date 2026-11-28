package cm.kamerbiz.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "product",
    uniqueConstraints = @UniqueConstraint(columnNames = {"business_id", "product_code"}))
public class Product {
  @Id private UUID id;

  @Column(name = "business_id", nullable = false)
  private UUID businessId;

  @Column(name = "product_code", nullable = false, length = 64)
  private String productCode;

  @Column(nullable = false, length = 255)
  private String name;

  @Column(length = 2000)
  private String description;

  @Column(length = 120)
  private String category;

  @Column(nullable = false, precision = 14, scale = 2)
  private BigDecimal price;

  @Column(name = "stock_quantity", nullable = false)
  private int stockQuantity;

  @Column(name = "image_url", length = 1000)
  private String imageUrl;

  @Column(name = "image_filename", length = 255)
  private String imageFilename;

  @Column(nullable = false)
  private boolean archived;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected Product() {}

  public Product(
      UUID businessId,
      String productCode,
      String name,
      String description,
      String category,
      BigDecimal price,
      int stockQuantity,
      String imageUrl) {
    this.id = UUID.randomUUID();
    this.businessId = businessId;
    this.productCode = productCode;
    this.name = name;
    this.description = description;
    this.category = category;
    this.price = price;
    this.stockQuantity = stockQuantity;
    this.imageUrl = imageUrl;
    this.createdAt = Instant.now();
    this.updatedAt = this.createdAt;
  }

  public void update(
      String name,
      String description,
      String category,
      BigDecimal price,
      int stockQuantity,
      String imageUrl) {
    this.name = name;
    this.description = description;
    this.category = category;
    this.price = price;
    this.stockQuantity = stockQuantity;
    this.imageUrl = imageUrl;
    this.updatedAt = Instant.now();
  }

  public void setArchived(boolean archived) {
    this.archived = archived;
    this.updatedAt = Instant.now();
  }

  public UUID getId() {
    return id;
  }

  public UUID getBusinessId() {
    return businessId;
  }

  public String getProductCode() {
    return productCode;
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  public String getCategory() {
    return category;
  }

  public BigDecimal getPrice() {
    return price;
  }

  public int getStockQuantity() {
    return stockQuantity;
  }

  public String getImageUrl() {
    return imageUrl;
  }

  public String getImageFilename() {
    return imageFilename;
  }

  public void setImageFilename(String imageFilename) {
    this.imageFilename = imageFilename;
  }

  public boolean isArchived() {
    return archived;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
