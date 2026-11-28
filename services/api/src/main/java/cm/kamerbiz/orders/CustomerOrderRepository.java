package cm.kamerbiz.orders;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, UUID> {
  Optional<CustomerOrder> findByTrackingTokenHash(String trackingTokenHash);

  Optional<CustomerOrder> findByIdAndBusinessId(UUID id, UUID businessId);

  List<CustomerOrder> findAllByBusinessIdOrderByCreatedAtDesc(UUID businessId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select o from CustomerOrder o where o.id = :id and o.businessId = :businessId")
  Optional<CustomerOrder> findForUpdate(@Param("id") UUID id, @Param("businessId") UUID businessId);
}
