package cm.kamerbiz.platform;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

class AudienceValidatorTest {
  private final AudienceValidator validator = new AudienceValidator("kamerbiz-api");

  @Test
  void acceptsTokenForKamerBizApi() {
    assertThat(validator.validate(token(List.of("kamerbiz-api", "account"))).hasErrors()).isFalse();
  }

  @Test
  void rejectsTokenForAnotherAudience() {
    assertThat(validator.validate(token(List.of("another-api"))).hasErrors()).isTrue();
  }

  @Test
  void rejectsTokenWithoutAudienceClaim() {
    assertThat(validator.validate(token(null)).hasErrors()).isTrue();
  }

  private Jwt token(List<String> audience) {
    Instant now = Instant.now();
    Jwt.Builder builder =
        Jwt.withTokenValue("test-token")
            .header("alg", "none")
            .issuer("http://localhost:8081/realms/kamerbiz")
            .subject("user-subject")
            .issuedAt(now)
            .expiresAt(now.plusSeconds(300));
    if (audience != null) {
      builder.audience(audience);
    }
    return builder.build();
  }
}
