package cm.kamerbiz.orders;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerOrderItemRepository extends JpaRepository<CustomerOrderItem, UUID> {
  List<CustomerOrderItem> findAllByOrderId(UUID orderId);

  List<CustomerOrderItem> findAllByOrderIdIn(Collection<UUID> orderIds);
}
