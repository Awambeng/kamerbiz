package cm.kamerbiz.catalog;

import cm.kamerbiz.catalog.ProductService.ProductResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/businesses/{businessId}/product-images")
public class ProductImageController {
  private final ProductImageService images;
  private final ProductService products;

  public ProductImageController(ProductImageService images, ProductService products) {
    this.images = images;
    this.products = products;
  }

  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ProductImageService.UploadedImage upload(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID businessId,
      @RequestPart("file") MultipartFile file) {
    return images.upload(jwt, businessId, file);
  }

  @DeleteMapping("/uploads")
  public ResponseEntity<Void> deleteUpload(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID businessId,
      @RequestParam String imageUrl) {
    if (!products.isImageInUse(businessId, imageUrl)) {
      images.deleteOwnedUrl(jwt, businessId, imageUrl);
    } else {
      images.validateOwnedUrl(jwt, businessId, imageUrl);
    }
    return ResponseEntity.noContent().build();
  }

  @PatchMapping("/{productId}")
  public ProductResponse assign(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID businessId,
      @PathVariable UUID productId,
      @RequestBody ImageRequest request) {
    images.validateOwnedUrl(jwt, businessId, request.imageUrl());
    return products.setImageUrl(jwt, businessId, productId, request.imageUrl());
  }

  public record ImageRequest(String imageUrl) {}

  @PostMapping(value = "/bulk", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public List<ProductImageService.UploadedImage> uploadMany(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID businessId,
      @RequestPart("files") List<MultipartFile> files) {
    return images.uploadMany(jwt, businessId, files);
  }
}
