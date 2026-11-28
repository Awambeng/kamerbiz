package cm.kamerbiz.orders;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderStatusEventRepository extends JpaRepository<OrderStatusEvent, UUID> {
  List<OrderStatusEvent> findAllByOrderIdOrderByCreatedAtAsc(UUID orderId);
}
