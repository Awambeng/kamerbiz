package cm.kamerbiz.business;

import cm.kamerbiz.identity.UserAccount;
import cm.kamerbiz.identity.UserAccountRepository;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BusinessMembershipAuthorizationService {
  private final UserAccountRepository userAccountRepository;
  private final BusinessMembershipRepository membershipRepository;

  public BusinessMembershipAuthorizationService(
      UserAccountRepository userAccountRepository,
      BusinessMembershipRepository membershipRepository) {
    this.userAccountRepository = userAccountRepository;
    this.membershipRepository = membershipRepository;
  }

  @Transactional(readOnly = true)
  public BusinessMembership requireMembership(
      Jwt jwt, UUID requestedBusinessId, Set<BusinessRole> allowedRoles) {
    UserAccount user =
        userAccountRepository
            .findByIdentityIssuerAndIdentitySubject(jwt.getIssuer().toString(), jwt.getSubject())
            .orElseThrow(() -> new AccessDeniedException("Business access denied."));

    BusinessMembership membership =
        membershipRepository
            .findByBusinessIdAndUserId(requestedBusinessId, user.getId())
            .orElseThrow(() -> new AccessDeniedException("Business access denied."));

    if (!allowedRoles.contains(membership.getRole())) {
      throw new AccessDeniedException("Business access denied.");
    }
    return membership;
  }
}
