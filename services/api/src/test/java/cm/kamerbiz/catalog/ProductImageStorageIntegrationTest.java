package cm.kamerbiz.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cm.kamerbiz.business.BusinessService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Base64;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;

@Testcontainers
@SpringBootTest
class ProductImageStorageIntegrationTest {
  private static final String ISSUER = "http://localhost:8081/realms/kamerbiz";
  private static final String BUCKET = "kamerbiz-product-integration";

  @Container
  static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:17.12-bookworm");

  @Container
  static final GenericContainer<?> MINIO =
      new GenericContainer<>(
              DockerImageName.parse("docker.io/minio/minio:RELEASE.2025-09-07T16-13-09Z"))
          .withCommand("server", "/data")
          .withEnv("MINIO_ROOT_USER", "kamerbiz-test")
          .withEnv("MINIO_ROOT_PASSWORD", "kamerbiz-test-secret")
          .withExposedPorts(9000)
          .waitingFor(Wait.forHttp("/minio/health/ready").forPort(9000));

  @DynamicPropertySource
  static void containerProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
    registry.add("app.storage.endpoint", ProductImageStorageIntegrationTest::minioEndpoint);
    registry.add("app.storage.public-base-url", ProductImageStorageIntegrationTest::publicBaseUrl);
    registry.add("app.storage.access-key", () -> "kamerbiz-test");
    registry.add("app.storage.secret-key", () -> "kamerbiz-test-secret");
    registry.add("app.storage.bucket", () -> BUCKET);
  }

  @Autowired private S3Client storage;
  @Autowired private ProductImageService imageService;
  @Autowired private ProductImageController imageController;
  @Autowired private ProductService productService;
  @Autowired private ProductImageCleanupWorker cleanupWorker;
  @Autowired private BusinessService businessService;

  @BeforeEach
  void createBucket() {
    try {
      storage.createBucket(CreateBucketRequest.builder().bucket(BUCKET).build());
    } catch (software.amazon.awssdk.services.s3.model.S3Exception exception) {
      if (exception.statusCode() != 409) throw exception;
    }
  }

  @Test
  void uploadsReadsAndDeletesImageFromRealMinio() {
    var business =
        businessService.create(
            token("image-owner"), new BusinessService.CreateBusinessRequest("Image Storage Shop"));
    var uploaded = imageService.upload(token("image-owner"), business.id(), png("shirt.png"));
    String key = objectKey(uploaded.imageUrl());

    var stored =
        storage.getObjectAsBytes(GetObjectRequest.builder().bucket(BUCKET).key(key).build());
    assertThat(stored.asByteArray()).containsExactly(pngBytes());

    imageService.deleteOwnedUrl(token("image-owner"), business.id(), uploaded.imageUrl());

    assertThatThrownBy(
            () -> storage.headObject(HeadObjectRequest.builder().bucket(BUCKET).key(key).build()))
        .isInstanceOf(SdkException.class);
  }

  @Test
  void temporaryCleanupDoesNotDeleteAnImageAlreadyAssignedToAProduct() {
    var jwt = token("assigned-image-owner");
    var business =
        businessService.create(
            jwt, new BusinessService.CreateBusinessRequest("Assigned Image Shop"));
    var uploaded = imageService.upload(jwt, business.id(), png("assigned.png"));
    productService.create(
        jwt,
        business.id(),
        new ProductService.ProductRequest(
            null, "Red Dress", null, null, new BigDecimal("9000"), 2, uploaded.imageUrl()));

    imageController.deleteUpload(jwt, business.id(), uploaded.imageUrl());

    assertThat(
            storage
                .getObjectAsBytes(
                    GetObjectRequest.builder()
                        .bucket(BUCKET)
                        .key(objectKey(uploaded.imageUrl()))
                        .build())
                .asByteArray())
        .containsExactly(pngBytes());
  }

  @Test
  void deletesReplacedImageOnlyAfterProductUpdateCommits() {
    var business =
        businessService.create(
            token("replace-owner"),
            new BusinessService.CreateBusinessRequest("Image Replacement Shop"));
    var first = imageService.upload(token("replace-owner"), business.id(), png("first.png"));
    var second = imageService.upload(token("replace-owner"), business.id(), png("second.png"));

    var product =
        productService.create(
            token("replace-owner"),
            business.id(),
            new ProductService.ProductRequest(
                null, "Blue Shirt", null, null, new BigDecimal("5000"), 3, first.imageUrl()));

    productService.update(
        token("replace-owner"),
        business.id(),
        product.id(),
        new ProductService.ProductRequest(
            product.productCode(),
            "Blue Shirt",
            null,
            null,
            new BigDecimal("5000"),
            3,
            second.imageUrl()));
    cleanupWorker.processDueCleanups();

    assertThatThrownBy(() ->
            storage.headObject(
                HeadObjectRequest.builder()
                    .bucket(BUCKET)
                    .key(objectKey(first.imageUrl()))
                    .build()))
        .isInstanceOf(SdkException.class);
    assertThat(
            storage
                .getObjectAsBytes(
                    GetObjectRequest.builder()
                        .bucket(BUCKET)
                        .key(objectKey(second.imageUrl()))
                        .build())
                .asByteArray())
        .containsExactly(pngBytes());
  }

  @Test
  void keepsAnImageUntilNoProductReferencesIt() {
    var jwt = token("shared-image-owner");
    var business =
        businessService.create(jwt, new BusinessService.CreateBusinessRequest("Shared Image Shop"));
    var shared = imageService.upload(jwt, business.id(), png("shared.png"));
    var replacement = imageService.upload(jwt, business.id(), png("replacement.png"));
    var first =
        productService.create(
            jwt,
            business.id(),
            new ProductService.ProductRequest(
                null, "First Shirt", null, null, new BigDecimal("5000"), 2, shared.imageUrl()));
    var second =
        productService.create(
            jwt,
            business.id(),
            new ProductService.ProductRequest(
                null, "Second Shirt", null, null, new BigDecimal("6000"), 1, shared.imageUrl()));

    productService.update(
        jwt,
        business.id(),
        first.id(),
        new ProductService.ProductRequest(
            first.productCode(),
            "First Shirt",
            null,
            null,
            new BigDecimal("5000"),
            2,
            replacement.imageUrl()));

    assertThat(
            storage.headObject(
                HeadObjectRequest.builder()
                    .bucket(BUCKET)
                    .key(objectKey(shared.imageUrl()))
                    .build()))
        .isNotNull();

    productService.update(
        jwt,
        business.id(),
        second.id(),
        new ProductService.ProductRequest(
            second.productCode(),
            "Second Shirt",
            null,
            null,
            new BigDecimal("6000"),
            1,
            replacement.imageUrl()));
    cleanupWorker.processDueCleanups();

    assertThatThrownBy(() ->
            storage.headObject(
                HeadObjectRequest.builder()
                    .bucket(BUCKET)
                    .key(objectKey(shared.imageUrl()))
                    .build()))
        .isInstanceOf(SdkException.class);
  }

  private static String minioEndpoint() {
    return "http://" + MINIO.getHost() + ":" + MINIO.getMappedPort(9000);
  }

  private static String publicBaseUrl() {
    return minioEndpoint() + "/" + BUCKET;
  }

  private static String objectKey(String imageUrl) {
    return imageUrl.substring((publicBaseUrl() + "/").length());
  }

  private MockMultipartFile png(String filename) {
    return new MockMultipartFile("file", filename, "image/png", pngBytes());
  }

  private static byte[] pngBytes() {
    return Base64.getDecoder()
        .decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+/pWQAAAAASUVORK5CYII=");
  }

  private Jwt token(String subject) {
    Instant now = Instant.now();
    return Jwt.withTokenValue("test-token")
        .header("alg", "none")
        .issuer(ISSUER)
        .subject(subject)
        .issuedAt(now)
        .expiresAt(now.plusSeconds(300))
        .build();
  }
}
