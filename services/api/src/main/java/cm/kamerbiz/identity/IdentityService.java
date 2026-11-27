package cm.kamerbiz.identity;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IdentityService {
  private final UserAccountRepository userAccountRepository;

  public IdentityService(UserAccountRepository userAccountRepository) {
    this.userAccountRepository = userAccountRepository;
  }

  @Transactional
  public UserAccount synchronize(Jwt jwt) {
    String issuer = jwt.getIssuer().toString();
    String subject = jwt.getSubject();
    if (subject == null || subject.isBlank()) {
      throw new IllegalArgumentException("The access token has no stable identity.");
    }

    UserAccount account =
        userAccountRepository
            .findByIdentityIssuerAndIdentitySubject(issuer, subject)
            .orElseGet(() -> new UserAccount(issuer, subject));

    String email = firstNonBlank(jwt.getClaimAsString("email"), account.getEmail());
    String displayName =
        firstNonBlank(
            firstNonBlank(jwt.getClaimAsString("name"), jwt.getClaimAsString("preferred_username")),
            account.getDisplayName());
    Boolean emailVerifiedClaim = jwt.getClaim("email_verified");
    account.updateProfile(
        email,
        displayName,
        emailVerifiedClaim != null ? emailVerifiedClaim : account.isEmailVerified());
    return userAccountRepository.save(account);
  }

  private String firstNonBlank(String first, String second) {
    if (first != null && !first.isBlank()) {
      return first;
    }
    return second;
  }
}
