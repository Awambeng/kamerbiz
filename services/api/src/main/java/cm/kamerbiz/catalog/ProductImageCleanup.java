package cm.kamerbiz.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "product_image_cleanup",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uq_product_image_cleanup_business_url",
            columnNames = {"business_id", "image_url"}))
public class ProductImageCleanup {
  @Id private UUID id;

  @Column(name = "business_id", nullable = false)
  private UUID businessId;

  @Column(name = "image_url", nullable = false, length = 1000)
  private String imageUrl;

  @Column(nullable = false)
  private int attempts;

  @Column(name = "next_attempt_at", nullable = false)
  private Instant nextAttemptAt;

  @Column(name = "last_error", length = 1000)
  private String lastError;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected ProductImageCleanup() {}

  public ProductImageCleanup(UUID businessId, String imageUrl) {
    this.id = UUID.randomUUID();
    this.businessId = businessId;
    this.imageUrl = imageUrl;
    this.nextAttemptAt = Instant.now();
    this.createdAt = Instant.now();
  }

  public UUID getBusinessId() {
    return businessId;
  }

  public String getImageUrl() {
    return imageUrl;
  }

  public int getAttempts() {
    return attempts;
  }

  public Instant getNextAttemptAt() {
    return nextAttemptAt;
  }

  public void scheduleRetry(String error, Instant nextAttemptAt) {
    attempts++;
    lastError = error.length() > 1000 ? error.substring(0, 1000) : error;
    this.nextAttemptAt = nextAttemptAt;
  }
}
