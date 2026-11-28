package cm.kamerbiz.catalog;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductImportBatchRepository extends JpaRepository<ProductImportBatch, UUID> {
  Optional<ProductImportBatch> findByBusinessIdAndBatchKey(UUID businessId, String batchKey);

  @Modifying
  @Query(
      value =
          """
          INSERT INTO product_import_batch (
              id, business_id, batch_key, created_count, skipped_count, created_at
          ) VALUES (
              :id, :businessId, :batchKey, 0, 0, CURRENT_TIMESTAMP
          )
          ON CONFLICT (business_id, batch_key) DO NOTHING
          """,
      nativeQuery = true)
  int claimBatch(
      @Param("id") UUID id,
      @Param("businessId") UUID businessId,
      @Param("batchKey") String batchKey);
}
