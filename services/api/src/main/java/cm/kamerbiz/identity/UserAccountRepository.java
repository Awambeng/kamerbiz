package cm.kamerbiz.identity;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAccountRepository extends JpaRepository<UserAccount, UUID> {
  Optional<UserAccount> findByIdentityIssuerAndIdentitySubject(
      String identityIssuer, String identitySubject);
}
