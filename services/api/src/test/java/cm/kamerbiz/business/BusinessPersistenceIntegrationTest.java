package cm.kamerbiz.business;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cm.kamerbiz.identity.UserAccount;
import cm.kamerbiz.identity.UserAccountRepository;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class BusinessPersistenceIntegrationTest {
  private static final String ISSUER = "http://localhost:8081/realms/kamerbiz";

  @Autowired private BusinessService businessService;
  @Autowired private BusinessAccountRepository businessRepository;
  @Autowired private BusinessMembershipRepository membershipRepository;
  @Autowired private UserAccountRepository userRepository;
  @Autowired private BusinessMembershipAuthorizationService authorizationService;

  @Test
  void persistsBusinessAndOwnerMembership() {
    BusinessService.BusinessResponse created =
        businessService.create(
            token("owner-one"), new BusinessService.CreateBusinessRequest("Grace Fashion"));

    assertThat(businessRepository.findById(created.id())).isPresent();
    UserAccount owner =
        userRepository.findByIdentityIssuerAndIdentitySubject(ISSUER, "owner-one").orElseThrow();
    BusinessMembership membership =
        membershipRepository.findByBusinessIdAndUserId(created.id(), owner.getId()).orElseThrow();

    assertThat(created.slug()).isEqualTo("grace-fashion");
    assertThat(membership.getRole()).isEqualTo(BusinessRole.OWNER);
  }

  @Test
  void preventsOneBusinessOwnerFromAccessingAnotherBusiness() {
    BusinessService.BusinessResponse first =
        businessService.create(
            token("owner-one"), new BusinessService.CreateBusinessRequest("First Shop"));
    BusinessService.BusinessResponse second =
        businessService.create(
            token("owner-two"), new BusinessService.CreateBusinessRequest("Second Shop"));

    assertThatThrownBy(() ->
            authorizationService.requireMembership(
                token("owner-one"), second.id(), Set.of(BusinessRole.OWNER)))
        .isInstanceOf(AccessDeniedException.class);
    assertThat(first.id()).isNotEqualTo(second.id());
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
