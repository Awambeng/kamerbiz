package cm.kamerbiz.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;

@ExtendWith(MockitoExtension.class)
class IdentityServiceTest {
  @Mock private UserAccountRepository userAccountRepository;
  @InjectMocks private IdentityService identityService;

  @Test
  void createsLocalProfileUsingIssuerAndSubject() {
    Jwt jwt = token();
    when(userAccountRepository.findByIdentityIssuerAndIdentitySubject(
            "http://localhost:8081/realms/kamerbiz", "subject-123"))
        .thenReturn(Optional.empty());
    when(userAccountRepository.save(any(UserAccount.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    UserAccount account = identityService.synchronize(jwt);

    assertThat(account.getIdentityIssuer()).isEqualTo("http://localhost:8081/realms/kamerbiz");
    assertThat(account.getIdentitySubject()).isEqualTo("subject-123");
    assertThat(account.getEmail()).isEqualTo("agent@example.com");
    assertThat(account.getDisplayName()).isEqualTo("KamerBiz Agent");
    assertThat(account.isEmailVerified()).isTrue();
  }

  @Test
  void preservesStoredProfileWhenOptionalClaimsAreMissing() {
    UserAccount existing = new UserAccount("http://localhost:8081/realms/kamerbiz", "subject-123");
    existing.updateProfile("existing@example.com", "Existing Name", true);
    when(userAccountRepository.findByIdentityIssuerAndIdentitySubject(
            "http://localhost:8081/realms/kamerbiz", "subject-123"))
        .thenReturn(Optional.of(existing));
    when(userAccountRepository.save(any(UserAccount.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    UserAccount account = identityService.synchronize(tokenWithoutProfileClaims());

    assertThat(account.getEmail()).isEqualTo("existing@example.com");
    assertThat(account.getDisplayName()).isEqualTo("Existing Name");
    assertThat(account.isEmailVerified()).isTrue();
  }

  private Jwt tokenWithoutProfileClaims() {
    Instant now = Instant.now();
    return Jwt.withTokenValue("test-token")
        .header("alg", "none")
        .issuer("http://localhost:8081/realms/kamerbiz")
        .subject("subject-123")
        .issuedAt(now)
        .expiresAt(now.plusSeconds(300))
        .build();
  }

  private Jwt token() {
    Instant now = Instant.now();
    return Jwt.withTokenValue("test-token")
        .header("alg", "none")
        .issuer("http://localhost:8081/realms/kamerbiz")
        .subject("subject-123")
        .claim("email", "agent@example.com")
        .claim("name", "KamerBiz Agent")
        .claim("email_verified", true)
        .issuedAt(now)
        .expiresAt(now.plusSeconds(300))
        .build();
  }
}
