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
    name = "product_import_batch",
    uniqueConstraints = @UniqueConstraint(columnNames = {"business_id", "batch_key"}))
public class ProductImportBatch {
  @Id private UUID id;

  @Column(name = "business_id", nullable = false)
  private UUID businessId;

  @Column(name = "batch_key", nullable = false, length = 100)
  private String batchKey;

  @Column(name = "created_count", nullable = false)
  private int createdCount;

  @Column(name = "skipped_count", nullable = false)
  private int skippedCount;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected ProductImportBatch() {}

  public ProductImportBatch(UUID businessId, String batchKey, int createdCount, int skippedCount) {
    this.id = UUID.randomUUID();
    this.businessId = businessId;
    this.batchKey = batchKey;
    this.createdCount = createdCount;
    this.skippedCount = skippedCount;
    this.createdAt = Instant.now();
  }

  public void setCounts(int createdCount, int skippedCount) {
    this.createdCount = createdCount;
    this.skippedCount = skippedCount;
  }

  public int getCreatedCount() {
    return createdCount;
  }

  public int getSkippedCount() {
    return skippedCount;
  }
}
