package cm.kamerbiz.identity;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserPlatformRoleRepository extends JpaRepository<UserPlatformRole, UUID> {}
