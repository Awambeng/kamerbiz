package cm.kamerbiz.orders;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "customer_order_item")
public class CustomerOrderItem {
  @Id private UUID id;

  @Column(name = "order_id", nullable = false)
  private UUID orderId;

  @Column(name = "product_id", nullable = false)
  private UUID productId;

  @Column(name = "product_name", nullable = false, length = 255)
  private String productName;

  @Column(name = "unit_price", nullable = false, precision = 18, scale = 2)
  private BigDecimal unitPrice;

  @Column(nullable = false)
  private int quantity;

  @Column(name = "line_total", nullable = false, precision = 18, scale = 2)
  private BigDecimal lineTotal;

  protected CustomerOrderItem() {}

  public CustomerOrderItem(
      UUID orderId, UUID productId, String name, BigDecimal price, int quantity) {
    this.id = UUID.randomUUID();
    this.orderId = orderId;
    this.productId = productId;
    this.productName = name;
    this.unitPrice = price;
    this.quantity = quantity;
    this.lineTotal = price.multiply(BigDecimal.valueOf(quantity));
  }

  public UUID getOrderId() {
    return orderId;
  }

  public UUID getProductId() {
    return productId;
  }

  public int getQuantity() {
    return quantity;
  }

  public String getProductName() {
    return productName;
  }

  public BigDecimal getUnitPrice() {
    return unitPrice;
  }

  public BigDecimal getLineTotal() {
    return lineTotal;
  }
}
