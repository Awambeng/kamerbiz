package cm.kamerbiz.catalog;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface ProductImageCleanupRepository extends JpaRepository<ProductImageCleanup, UUID> {
  Optional<ProductImageCleanup> findByBusinessIdAndImageUrl(UUID businessId, String imageUrl);

  List<ProductImageCleanup> findTop20ByNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
      Instant nextAttemptAt);

  @Transactional
  @Modifying
  @Query(
      value =
          """
          INSERT INTO product_image_cleanup (
              id, business_id, image_url, attempts, next_attempt_at, created_at
          ) VALUES (:id, :businessId, :imageUrl, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
          ON CONFLICT (business_id, image_url) DO NOTHING
          """,
      nativeQuery = true)
  int enqueue(
      @Param("id") UUID id,
      @Param("businessId") UUID businessId,
      @Param("imageUrl") String imageUrl);
}
