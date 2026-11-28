package cm.kamerbiz.catalog;

import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ProductImageCleanupWorkerTest {
  private final ProductImageCleanupRepository cleanups = mock(ProductImageCleanupRepository.class);
  private final ProductRepository products = mock(ProductRepository.class);
  private final ProductImageService images = mock(ProductImageService.class);
  private final ProductImageCleanupWorker worker =
      new ProductImageCleanupWorker(cleanups, products, images);

  @Test
  void deletesUnreferencedImagesAndRemovesCompletedJobs() {
    UUID businessId = UUID.randomUUID();
    String imageUrl = "https://cdn.example.com/image.png";
    ProductImageCleanup cleanup = new ProductImageCleanup(businessId, imageUrl);
    when(cleanups.findTop20ByNextAttemptAtLessThanEqualOrderByCreatedAtAsc(any()))
        .thenReturn(List.of(cleanup));
    when(products.existsByBusinessIdAndImageUrl(businessId, imageUrl)).thenReturn(false);

    worker.processDueCleanups();

    verify(images).deleteStoredImage(businessId, imageUrl);
    verify(cleanups).delete(cleanup);
  }

  @Test
  void keepsImagesThatAreStillReferenced() {
    UUID businessId = UUID.randomUUID();
    String imageUrl = "https://cdn.example.com/image.png";
    ProductImageCleanup cleanup = new ProductImageCleanup(businessId, imageUrl);
    when(cleanups.findTop20ByNextAttemptAtLessThanEqualOrderByCreatedAtAsc(any()))
        .thenReturn(List.of(cleanup));
    when(products.existsByBusinessIdAndImageUrl(businessId, imageUrl)).thenReturn(true);

    worker.processDueCleanups();

    verify(images, never()).deleteStoredImage(any(), any());
    verify(cleanups).delete(cleanup);
  }

  @Test
  void schedulesRetryWhenObjectStorageDeletionFails() {
    UUID businessId = UUID.randomUUID();
    String imageUrl = "https://cdn.example.com/image.png";
    ProductImageCleanup cleanup = new ProductImageCleanup(businessId, imageUrl);
    when(cleanups.findTop20ByNextAttemptAtLessThanEqualOrderByCreatedAtAsc(any()))
        .thenReturn(List.of(cleanup));
    when(products.existsByBusinessIdAndImageUrl(businessId, imageUrl)).thenReturn(false);
    doThrow(new IllegalStateException("Storage unavailable"))
        .when(images)
        .deleteStoredImage(businessId, imageUrl);

    worker.processDueCleanups();

    org.assertj.core.api.Assertions.assertThat(cleanup.getAttempts()).isEqualTo(1);
    org.assertj.core.api.Assertions.assertThat(cleanup.getNextAttemptAt()).isAfter(Instant.now());
    verify(cleanups, never()).delete(cleanup);
  }
}
