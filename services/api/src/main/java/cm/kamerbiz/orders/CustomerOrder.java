package cm.kamerbiz.orders;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Entity
@Table(name = "customer_order")
public class CustomerOrder {
  @Id private UUID id;

  @Column(name = "business_id", nullable = false)
  private UUID businessId;

  @Column(nullable = false, unique = true, length = 32)
  private String reference;

  @Column(name = "tracking_token_hash", nullable = false, unique = true, length = 64)
  private String trackingTokenHash;

  @Column(name = "customer_name", nullable = false, length = 160)
  private String customerName;

  @Column(nullable = false, length = 32)
  private String phone;

  @Column(name = "fulfilment_method", nullable = false, length = 16)
  private String fulfilmentMethod;

  @Column(name = "delivery_address", length = 1000)
  private String deliveryAddress;

  @Column(name = "customer_note", length = 500)
  private String customerNote;

  @Column(name = "order_status", nullable = false, length = 24)
  private String orderStatus;

  @Column(name = "fulfilment_status", nullable = false, length = 24)
  private String fulfilmentStatus;

  @Column(name = "total_amount", nullable = false, precision = 18, scale = 2)
  private BigDecimal totalAmount;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected CustomerOrder() {}

  public CustomerOrder(
      UUID businessId,
      String reference,
      String trackingTokenHash,
      String name,
      String phone,
      String method,
      String address,
      String note,
      BigDecimal total) {
    this.id = UUID.randomUUID();
    this.businessId = businessId;
    this.reference = reference;
    this.trackingTokenHash = trackingTokenHash;
    this.customerName = name;
    this.phone = phone;
    this.fulfilmentMethod = method;
    this.deliveryAddress = address;
    this.customerNote = note;
    this.totalAmount = total;
    this.orderStatus = "NEW";
    this.fulfilmentStatus = "NOT_ARRANGED";
    this.createdAt = Instant.now();
    this.updatedAt = createdAt;
  }

  public void accept() {
    if (!"NEW".equals(orderStatus)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Only new orders can be accepted.");
    }
    this.orderStatus = "ACCEPTED";
    this.fulfilmentStatus = "PREPARING";
    this.updatedAt = Instant.now();
  }

  public void reject() {
    if (!"NEW".equals(orderStatus)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Only new orders can be rejected.");
    }
    this.orderStatus = "REJECTED";
    this.updatedAt = Instant.now();
  }

  public boolean canUpdateFulfilment(String status) {
    if (!"ACCEPTED".equals(orderStatus)) return false;
    return switch (fulfilmentStatus) {
      case "PREPARING" ->
          ("PICKUP".equals(fulfilmentMethod) && "READY_FOR_PICKUP".equals(status))
              || ("DELIVERY".equals(fulfilmentMethod) && "DISPATCHED".equals(status));
      case "READY_FOR_PICKUP" -> "PICKUP".equals(fulfilmentMethod) && "COMPLETED".equals(status);
      case "DISPATCHED" -> "DELIVERY".equals(fulfilmentMethod) && "DELIVERED".equals(status);
      default -> false;
    };
  }

  public void updateFulfilment(String status) {
    if (!canUpdateFulfilment(status)) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Invalid fulfilment status transition.");
    }
    this.fulfilmentStatus = status;
    if ("DELIVERED".equals(status) || "COMPLETED".equals(status)) {
      this.orderStatus = "COMPLETED";
    }
    this.updatedAt = Instant.now();
  }

  public UUID getId() {
    return id;
  }

  public UUID getBusinessId() {
    return businessId;
  }

  public String getReference() {
    return reference;
  }

  public String getTrackingTokenHash() {
    return trackingTokenHash;
  }

  public String getCustomerName() {
    return customerName;
  }

  public String getPhone() {
    return phone;
  }

  public String getFulfilmentMethod() {
    return fulfilmentMethod;
  }

  public String getDeliveryAddress() {
    return deliveryAddress;
  }

  public String getCustomerNote() {
    return customerNote;
  }

  public String getOrderStatus() {
    return orderStatus;
  }

  public String getFulfilmentStatus() {
    return fulfilmentStatus;
  }

  public BigDecimal getTotalAmount() {
    return totalAmount;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
