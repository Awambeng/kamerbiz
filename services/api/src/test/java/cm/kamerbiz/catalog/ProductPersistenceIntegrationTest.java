package cm.kamerbiz.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cm.kamerbiz.business.BusinessService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest
@Transactional
class ProductPersistenceIntegrationTest {
  private static final String ISSUER = "http://localhost:8081/realms/kamerbiz";

  @Container
  static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:17.12-bookworm");

  @DynamicPropertySource
  static void databaseProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
    registry.add("spring.datasource.username", POSTGRES::getUsername);
    registry.add("spring.datasource.password", POSTGRES::getPassword);
  }

  @Autowired private BusinessService businessService;
  @Autowired private ProductService productService;
  @Autowired private ProductRepository productRepository;
  @Autowired private ProductImportBatchRepository importBatches;
  @Autowired private PlatformTransactionManager transactionManager;

  @Test
  void persistsProductAndKeepsItIsolatedToItsBusiness() {
    var firstBusiness =
        businessService.create(
            token("owner-one"), new BusinessService.CreateBusinessRequest("First Product Shop"));
    var secondBusiness =
        businessService.create(
            token("owner-two"), new BusinessService.CreateBusinessRequest("Second Product Shop"));

    var created =
        productService.create(
            token("owner-one"),
            firstBusiness.id(),
            new ProductService.ProductRequest(
                null, "Blue Jeans", "Classic fit", "Fashion", new BigDecimal("12000"), 8, null));

    assertThat(productRepository.findByIdAndBusinessId(created.id(), firstBusiness.id()))
        .isPresent();
    assertThat(created.productCode()).startsWith("KB-");
    assertThatThrownBy(() -> productService.list(token("owner-one"), secondBusiness.id(), false))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void claimsAnImportBatchOnlyOnceForTheSameBusinessAndKey() {
    var business =
        businessService.create(
            token("owner-one"), new BusinessService.CreateBusinessRequest("Import Batch Shop"));

    int firstClaim = importBatches.claimBatch(UUID.randomUUID(), business.id(), "same-key");
    int repeatedClaim = importBatches.claimBatch(UUID.randomUUID(), business.id(), "same-key");

    assertThat(firstClaim).isEqualTo(1);
    assertThat(repeatedClaim).isZero();
    assertThat(importBatches.findByBusinessIdAndBatchKey(business.id(), "same-key")).isPresent();
  }

  @Test
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  void allowsOnlyOneConcurrentClaimForTheSameImportBatch() throws Exception {
    var business =
        businessService.create(
            token("concurrent-owner"),
            new BusinessService.CreateBusinessRequest("Concurrent Import Shop"));
    var ready = new CountDownLatch(2);
    var start = new CountDownLatch(1);
    var transaction = new TransactionTemplate(transactionManager);
    transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

    try (var executor = Executors.newFixedThreadPool(2)) {
      var claim =
          (java.util.concurrent.Callable<Integer>)
              () -> {
                ready.countDown();
                if (!start.await(5, TimeUnit.SECONDS)) {
                  throw new IllegalStateException("Timed out waiting for concurrent import start.");
                }
                return transaction.execute(status ->
                    importBatches.claimBatch(UUID.randomUUID(), business.id(), "concurrent-batch"));
              };
      var first = executor.submit(claim);
      var second = executor.submit(claim);

      assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
      start.countDown();

      assertThat(List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS)))
          .containsExactlyInAnyOrder(0, 1);
    }
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
