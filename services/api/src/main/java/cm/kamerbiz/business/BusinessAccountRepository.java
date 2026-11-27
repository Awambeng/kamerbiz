package cm.kamerbiz.business;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BusinessAccountRepository extends JpaRepository<BusinessAccount, UUID> {
  Optional<BusinessAccount> findBySlug(String slug);

  boolean existsBySlug(String slug);
}
