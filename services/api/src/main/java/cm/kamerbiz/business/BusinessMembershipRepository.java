package cm.kamerbiz.business;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BusinessMembershipRepository extends JpaRepository<BusinessMembership, UUID> {
  Optional<BusinessMembership> findByBusinessIdAndUserId(UUID businessId, UUID userId);
}
