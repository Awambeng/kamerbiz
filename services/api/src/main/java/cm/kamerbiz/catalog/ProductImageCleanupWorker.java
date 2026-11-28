package cm.kamerbiz.catalog;

import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ProductImageCleanupWorker {
  private static final Logger LOGGER = LoggerFactory.getLogger(ProductImageCleanupWorker.class);

  private final ProductImageCleanupRepository cleanups;
  private final ProductRepository products;
  private final ProductImageService images;

  public ProductImageCleanupWorker(
      ProductImageCleanupRepository cleanups,
      ProductRepository products,
      ProductImageService images) {
    this.cleanups = cleanups;
    this.products = products;
    this.images = images;
  }

  @Scheduled(fixedDelayString = "${app.storage.cleanupIntervalMs:30000}")
  @Transactional
  public void processDueCleanups() {
    List<ProductImageCleanup> due =
        cleanups.findTop20ByNextAttemptAtLessThanEqualOrderByCreatedAtAsc(Instant.now());
    for (ProductImageCleanup cleanup : due) {
      try {
        if (!products.existsByBusinessIdAndImageUrl(
            cleanup.getBusinessId(), cleanup.getImageUrl())) {
          images.deleteStoredImage(cleanup.getBusinessId(), cleanup.getImageUrl());
        }
        cleanups.delete(cleanup);
      } catch (RuntimeException exception) {
        long delaySeconds = Math.min(86400L, 30L << Math.min(cleanup.getAttempts(), 10));
        cleanup.scheduleRetry(
            exception.getClass().getSimpleName() + ": " + exception.getMessage(),
            Instant.now().plusSeconds(delaySeconds));
        LOGGER.warn(
            "Product image cleanup failed for business {}; retry scheduled",
            cleanup.getBusinessId(),
            exception);
      }
    }
  }
}
