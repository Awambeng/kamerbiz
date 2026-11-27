package cm.kamerbiz.business;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import cm.kamerbiz.identity.UserAccount;
import cm.kamerbiz.identity.UserAccountRepository;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;

@ExtendWith(MockitoExtension.class)
class BusinessMembershipAuthorizationServiceTest {
  private static final String ISSUER = "http://localhost:8081/realms/kamerbiz";
  private static final String SUBJECT = "subject-123";

  @Mock private UserAccountRepository userAccountRepository;
  @Mock private BusinessMembershipRepository membershipRepository;
  @InjectMocks private BusinessMembershipAuthorizationService authorizationService;

  @Test
  void createsMembershipWithRequiredPersistenceFields() {
    UUID businessId = UUID.randomUUID();
    UserAccount user = user();

    BusinessMembership membership = new BusinessMembership(businessId, user, BusinessRole.OWNER);

    assertThat(membership.getId()).isNotNull();
    assertThat(membership.getBusinessId()).isEqualTo(businessId);
    assertThat(membership.getUser()).isSameAs(user);
    assertThat(membership.getRole()).isEqualTo(BusinessRole.OWNER);
    assertThat(membership.getCreatedAt()).isNotNull();
  }

  @Test
  void deniesUserWithoutMembershipInRequestedBusiness() {
    UUID businessId = UUID.randomUUID();
    UserAccount user = user();
    when(userAccountRepository.findByIdentityIssuerAndIdentitySubject(ISSUER, SUBJECT))
        .thenReturn(Optional.of(user));
    when(membershipRepository.findByBusinessIdAndUserId(businessId, user.getId()))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() ->
            authorizationService.requireMembership(
                token(), businessId, Set.of(BusinessRole.OWNER, BusinessRole.MANAGER)))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void deniesMemberOfAnotherBusiness() {
    UUID businessA = UUID.randomUUID();
    UUID businessB = UUID.randomUUID();
    UserAccount user = user();
    BusinessMembership membershipInA = new BusinessMembership(businessA, user, BusinessRole.OWNER);
    Map<UUID, BusinessMembership> memberships = Map.of(businessA, membershipInA);
    when(userAccountRepository.findByIdentityIssuerAndIdentitySubject(ISSUER, SUBJECT))
        .thenReturn(Optional.of(user));
    when(membershipRepository.findByBusinessIdAndUserId(any(UUID.class), eq(user.getId())))
        .thenAnswer(invocation -> Optional.ofNullable(memberships.get(invocation.getArgument(0))));

    assertThatThrownBy(() ->
            authorizationService.requireMembership(
                token(), businessB, Set.of(BusinessRole.OWNER, BusinessRole.MANAGER)))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void deniesMembershipWithInsufficientRole() {
    UUID businessId = UUID.randomUUID();
    UserAccount user = user();
    BusinessMembership staffMembership = mock(BusinessMembership.class);
    when(userAccountRepository.findByIdentityIssuerAndIdentitySubject(ISSUER, SUBJECT))
        .thenReturn(Optional.of(user));
    when(membershipRepository.findByBusinessIdAndUserId(businessId, user.getId()))
        .thenReturn(Optional.of(staffMembership));
    when(staffMembership.getRole()).thenReturn(BusinessRole.STAFF);

    assertThatThrownBy(() ->
            authorizationService.requireMembership(
                token(), businessId, Set.of(BusinessRole.OWNER, BusinessRole.MANAGER)))
        .isInstanceOf(AccessDeniedException.class);
  }

  private UserAccount user() {
    return new UserAccount(ISSUER, SUBJECT);
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
