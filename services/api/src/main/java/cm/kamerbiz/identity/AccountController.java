package cm.kamerbiz.identity;

import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/account")
public class AccountController {
  private final IdentityService identityService;

  public AccountController(IdentityService identityService) {
    this.identityService = identityService;
  }

  @GetMapping
  public AccountResponse currentAccount(@AuthenticationPrincipal Jwt jwt) {
    UserAccount account = identityService.synchronize(jwt);
    return new AccountResponse(
        account.getId(), account.getEmail(), account.getDisplayName(), account.isEmailVerified());
  }

  public record AccountResponse(UUID id, String email, String displayName, boolean emailVerified) {}
}
