package cm.kamerbiz.business;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cm.kamerbiz.identity.IdentityService;
import cm.kamerbiz.identity.UserAccount;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;

@ExtendWith(MockitoExtension.class)
class BusinessServiceTest {
  private static final String ISSUER = "http://localhost:8081/realms/kamerbiz";
  private static final String SUBJECT = "user-123";

  @Mock private IdentityService identityService;
  @Mock private BusinessAccountRepository businessRepository;
  @Mock private BusinessMembershipRepository membershipRepository;
  @Mock private BusinessMembershipAuthorizationService authorizationService;

  @InjectMocks private BusinessService businessService;

  @Test
  void createsBusinessAndAssignsCreatorAsOwner() {
    UserAccount user = new UserAccount(ISSUER, SUBJECT);
    when(identityService.synchronize(any(Jwt.class))).thenReturn(user);
    when(businessRepository.existsBySlug("my-shop")).thenReturn(false);
    when(businessRepository.save(any(BusinessAccount.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    BusinessService.BusinessResponse response =
        businessService.create(token(), new BusinessService.CreateBusinessRequest("My Shop"));

    assertThat(response.name()).isEqualTo("My Shop");
    assertThat(response.slug()).isEqualTo("my-shop");
    assertThat(response.role()).isEqualTo(BusinessRole.OWNER);
    verify(membershipRepository).save(any(BusinessMembership.class));
  }

  @Test
  void choosesAvailableSlugWhenNameAlreadyExists() {
    UserAccount user = new UserAccount(ISSUER, SUBJECT);
    when(identityService.synchronize(any(Jwt.class))).thenReturn(user);
    when(businessRepository.existsBySlug("my-shop")).thenReturn(true);
    when(businessRepository.existsBySlug("my-shop-2")).thenReturn(false);
    when(businessRepository.save(any(BusinessAccount.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    BusinessService.BusinessResponse response =
        businessService.create(token(), new BusinessService.CreateBusinessRequest("My Shop"));

    assertThat(response.slug()).isEqualTo("my-shop-2");
  }

  @Test
  void deniesProfileUpdateWhenUserIsNotOwnerOrManager() {
    UUID businessId = UUID.randomUUID();
    when(authorizationService.requireMembership(any(), any(), any()))
        .thenThrow(new AccessDeniedException("Business access denied."));

    assertThatThrownBy(() ->
            businessService.update(
                token(),
                businessId,
                new BusinessService.UpdateBusinessRequest(
                    null, null, "Changed", null, null, null, null, null, null, null, null)))
        .isInstanceOf(AccessDeniedException.class);
  }

  private Jwt token() {
    Instant now = Instant.now();
    return Jwt.withTokenValue("test-token")
        .header("alg", "none")
        .issuer(ISSUER)
        .subject(SUBJECT)
        .issuedAt(now)
        .expiresAt(now.plusSeconds(300))
        .build();
  }
}
