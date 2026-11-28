package cm.kamerbiz.orders;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "order_status_event")
public class OrderStatusEvent {
  @Id private UUID id;

  @Column(name = "order_id", nullable = false)
  private UUID orderId;

  @Column(name = "business_id", nullable = false)
  private UUID businessId;

  @Column(name = "event_type", nullable = false, length = 24)
  private String eventType;

  @Column(name = "old_status", length = 24)
  private String oldStatus;

  @Column(name = "new_status", nullable = false, length = 24)
  private String newStatus;

  @Column(name = "actor_user_id")
  private UUID actorUserId;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected OrderStatusEvent() {}

  public OrderStatusEvent(
      UUID orderId,
      UUID businessId,
      String eventType,
      String oldStatus,
      String newStatus,
      UUID actorUserId) {
    this.id = UUID.randomUUID();
    this.orderId = orderId;
    this.businessId = businessId;
    this.eventType = eventType;
    this.oldStatus = oldStatus;
    this.newStatus = newStatus;
    this.actorUserId = actorUserId;
    this.createdAt = Instant.now();
  }
}
