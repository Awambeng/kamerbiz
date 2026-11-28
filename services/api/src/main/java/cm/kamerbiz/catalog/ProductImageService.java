package cm.kamerbiz.catalog;

import cm.kamerbiz.business.BusinessMembershipAuthorizationService;
import cm.kamerbiz.business.BusinessRole;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
public class ProductImageService {
  private static final Logger LOGGER = LoggerFactory.getLogger(ProductImageService.class);
  private final ProductImageValidator validator = new ProductImageValidator();
  private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;
  private static final int MAX_FILES_PER_BATCH = 20;
  private static final Set<BusinessRole> EDITORS = Set.of(BusinessRole.OWNER, BusinessRole.MANAGER);

  private final S3Client storage;
  private final String bucket;
  private final String publicBaseUrl;
  private final BusinessMembershipAuthorizationService authorization;
  private final ProductImageCleanupRepository cleanups;

  public ProductImageService(
      S3Client storage,
      @Value("${app.storage.bucket}") String bucket,
      @Value("${app.storage.public-base-url}") String publicBaseUrl,
      BusinessMembershipAuthorizationService authorization,
      ProductImageCleanupRepository cleanups) {
    this.storage = storage;
    this.bucket = bucket;
    this.publicBaseUrl = publicBaseUrl.replaceAll("/+$", "");
    this.authorization = authorization;
    this.cleanups = cleanups;
  }

  public UploadedImage upload(Jwt jwt, UUID businessId, MultipartFile file) {
    authorization.requireMembership(jwt, businessId, EDITORS);
    validateFile(file);
    return store(businessId, file);
  }

  private UploadedImage store(UUID businessId, MultipartFile file) {
    String contentType = file.getContentType();
    String extension = extensionFor(contentType);
    byte[] bytes;
    try {
      bytes = file.getBytes();
    } catch (IOException exception) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Image could not be read.");
    }

    String key = "businesses/" + businessId + "/products/" + UUID.randomUUID() + "." + extension;
    storage.putObject(
        PutObjectRequest.builder()
            .bucket(bucket)
            .key(key)
            .contentType(contentType)
            .cacheControl("public, max-age=31536000, immutable")
            .metadata(java.util.Map.of("business-id", businessId.toString()))
            .build(),
        RequestBody.fromBytes(bytes));
    String filename =
        file.getOriginalFilename() == null
            ? key.substring(key.lastIndexOf('/') + 1)
            : file.getOriginalFilename();
    return new UploadedImage(filename, publicBaseUrl + "/" + key);
  }

  public void validateOwnedUrl(Jwt jwt, UUID businessId, String imageUrl) {
    authorization.requireMembership(jwt, businessId, EDITORS);
    objectKey(businessId, imageUrl);
  }

  public void deleteOwnedUrl(Jwt jwt, UUID businessId, String imageUrl) {
    authorization.requireMembership(jwt, businessId, EDITORS);
    deleteOrEnqueue(businessId, imageUrl);
  }

  private void deleteOrEnqueue(UUID businessId, String imageUrl) {
    String key = objectKey(businessId, imageUrl);
    try {
      deleteStoredObject(key);
    } catch (RuntimeException exception) {
      LOGGER.warn("Unable to delete product image immediately; scheduling retry", exception);
      cleanups.enqueue(UUID.randomUUID(), businessId, imageUrl);
    }
  }

  void deleteStoredImage(UUID businessId, String imageUrl) {
    deleteStoredObject(objectKey(businessId, imageUrl));
  }

  private void deleteStoredObject(String key) {
    storage.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
  }

  private String objectKey(UUID businessId, String imageUrl) {
    String expectedPrefix = publicBaseUrl + "/businesses/" + businessId + "/products/";
    if (imageUrl == null || !imageUrl.startsWith(expectedPrefix)) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Product images must belong to this business.");
    }
    String objectName = imageUrl.substring(expectedPrefix.length());
    if (!objectName.matches("[0-9a-fA-F-]{36}\\.(jpg|png|webp)")) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Product image reference is invalid.");
    }
    return "businesses/" + businessId + "/products/" + objectName;
  }

  public List<UploadedImage> uploadMany(Jwt jwt, UUID businessId, List<MultipartFile> files) {
    authorization.requireMembership(jwt, businessId, EDITORS);
    if (files == null || files.isEmpty() || files.size() > MAX_FILES_PER_BATCH) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Select between 1 and 20 images.");
    }
    files.forEach(this::validateFile);
    List<UploadedImage> uploaded = new java.util.ArrayList<>();
    try {
      for (MultipartFile file : files) {
        uploaded.add(store(businessId, file));
      }
      return List.copyOf(uploaded);
    } catch (RuntimeException exception) {
      uploaded.forEach(image -> {
        try {
          deleteOrEnqueue(businessId, image.imageUrl());
        } catch (RuntimeException cleanupException) {
          LOGGER.warn("Unable to clean up partially uploaded product image", cleanupException);
        }
      });
      throw exception;
    }
  }

  private void validateFile(MultipartFile file) {
    if (file == null || file.isEmpty() || file.getSize() > MAX_FILE_SIZE) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Each image must be between 1 byte and 5 MB.");
    }
    String contentType = file.getContentType();
    extensionFor(contentType);
    try {
      validator.validate(file.getBytes(), contentType);
    } catch (IOException | IllegalArgumentException exception) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Image could not be read.");
    }
  }

  private String extensionFor(String contentType) {
    if ("image/jpeg".equalsIgnoreCase(contentType)) return "jpg";
    if ("image/png".equalsIgnoreCase(contentType)) return "png";
    if ("image/webp".equalsIgnoreCase(contentType)) return "webp";
    throw new ResponseStatusException(
        HttpStatus.BAD_REQUEST, "Only JPEG, PNG and WebP images are supported.");
  }

  public record UploadedImage(String filename, String imageUrl) {}
}
