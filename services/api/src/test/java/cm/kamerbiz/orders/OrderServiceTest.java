package cm.kamerbiz.orders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import cm.kamerbiz.business.BusinessAccount;
import cm.kamerbiz.business.BusinessAccountRepository;
import cm.kamerbiz.business.BusinessMembership;
import cm.kamerbiz.business.BusinessMembershipAuthorizationService;
import cm.kamerbiz.catalog.Product;
import cm.kamerbiz.catalog.ProductRepository;
import cm.kamerbiz.identity.UserAccount;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ResponseStatusException;

class OrderServiceTest {
  private BusinessAccountRepository businesses;
  private ProductRepository products;
  private CustomerOrderRepository orders;
  private CustomerOrderItemRepository items;
  private OrderStatusEventRepository events;
  private BusinessMembershipAuthorizationService authorization;
  private OrderService service;
  private UUID businessId;
  private final UUID productId = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    businesses = mock(BusinessAccountRepository.class);
    products = mock(ProductRepository.class);
    orders = mock(CustomerOrderRepository.class);
    items = mock(CustomerOrderItemRepository.class);
    events = mock(OrderStatusEventRepository.class);
    authorization = mock(BusinessMembershipAuthorizationService.class);
    service = new OrderService(businesses, products, orders, items, events, authorization);
    BusinessAccount business = new BusinessAccount("Grace Fashion", "grace-fashion");
    businessId = business.getId();
    when(businesses.findBySlug("grace-fashion")).thenReturn(Optional.of(business));
    UserAccount actor = mock(UserAccount.class);
    when(actor.getId()).thenReturn(UUID.randomUUID());
    BusinessMembership membership = mock(BusinessMembership.class);
    when(membership.getUser()).thenReturn(actor);
    when(authorization.requireMembership(any(Jwt.class), eq(businessId), anySet()))
        .thenReturn(membership);
  }

  @Test
  void calculatesOrderTotalFromServerProductPricesAndReturnsPrivateTrackingLink() {
    Product product = product("Blue Jeans", new BigDecimal("12000.00"), 5);
    when(products.findByIdAndBusinessId(productId, businessId)).thenReturn(Optional.of(product));
    when(orders.save(any(CustomerOrder.class))).thenAnswer(call -> call.getArgument(0));

    var result =
        service.create(
            "grace-fashion",
            new OrderService.CreateOrderRequest(
                "Customer",
                "677000000",
                "PICKUP",
                null,
                null,
                List.of(new OrderService.OrderLine(productId, 2))));

    assertThat(result.reference()).startsWith("KB-");
    assertThat(result.trackingUrl()).startsWith("/track/");
    assertThat(result.trackingUrl()).hasSize("/track/".length() + 43);
    verify(orders)
        .save(argThat(order -> order.getTotalAmount().compareTo(new BigDecimal("24000.00")) == 0));
    verify(items).save(any(CustomerOrderItem.class));
    verify(events).save(argThat(event -> event != null));
    verify(orders).save(argThat(order -> order.getTrackingTokenHash().length() == 64));
  }

  @Test
  void rejectsOrdersForInsufficientStock() {
    when(products.findByIdAndBusinessId(productId, businessId))
        .thenReturn(Optional.of(product("Blue Jeans", new BigDecimal("12000"), 1)));

    assertThatThrownBy(() ->
            service.create(
                "grace-fashion",
                new OrderService.CreateOrderRequest(
                    "Customer",
                    "677000000",
                    "PICKUP",
                    null,
                    null,
                    List.of(new OrderService.OrderLine(productId, 2)))))
        .isInstanceOf(ResponseStatusException.class)
        .extracting("statusCode")
        .isEqualTo(HttpStatus.CONFLICT);
    verify(orders, never()).save(any());
  }

  @Test
  void decrementsStockOnlyWhenVendorAcceptsOrder() {
    Product product = product("Blue Jeans", new BigDecimal("12000"), 5);
    UUID orderId = UUID.randomUUID();
    CustomerOrder order =
        new CustomerOrder(
            businessId,
            "KB-12345678",
            "private-token",
            "Customer",
            "677000000",
            "PICKUP",
            null,
            null,
            new BigDecimal("24000"));
    when(orders.findForUpdate(orderId, businessId)).thenReturn(Optional.of(order));
    when(items.findAllByOrderId(order.getId()))
        .thenReturn(
            List.of(
                new CustomerOrderItem(
                    order.getId(), productId, "Blue Jeans", new BigDecimal("12000"), 2)));
    when(products.findForUpdate(productId, businessId)).thenReturn(Optional.of(product));

    service.decide(jwt(), businessId, orderId, "ACCEPT");

    assertThat(product.getStockQuantity()).isEqualTo(3);
    assertThat(order.getOrderStatus()).isEqualTo("ACCEPTED");
    verify(events).save(any(OrderStatusEvent.class));
  }

  @Test
  void doesNotAcceptOrderWhenStockWasSoldBeforeReview() {
    UUID orderId = UUID.randomUUID();
    CustomerOrder order =
        new CustomerOrder(
            businessId,
            "KB-12345678",
            "private-token",
            "Customer",
            "677000000",
            "PICKUP",
            null,
            null,
            new BigDecimal("24000"));
    when(orders.findForUpdate(orderId, businessId)).thenReturn(Optional.of(order));
    when(items.findAllByOrderId(order.getId()))
        .thenReturn(
            List.of(
                new CustomerOrderItem(
                    order.getId(), productId, "Blue Jeans", new BigDecimal("12000"), 2)));
    when(products.findForUpdate(productId, businessId))
        .thenReturn(Optional.of(product("Blue Jeans", new BigDecimal("12000"), 1)));

    assertThatThrownBy(() -> service.decide(jwt(), businessId, orderId, "ACCEPT"))
        .isInstanceOf(ResponseStatusException.class)
        .extracting("statusCode")
        .isEqualTo(HttpStatus.CONFLICT);
    assertThat(order.getOrderStatus()).isEqualTo("NEW");
  }

  @Test
  void supportsMaximumCartValueAcrossAllAllowedLinesAndQuantities() {
    BigDecimal maximumPrice = new BigDecimal("999999999999.99");
    List<OrderService.OrderLine> lines = new java.util.ArrayList<>();
    for (int index = 0; index < 50; index++) {
      Product product = product("Premium Item " + index, maximumPrice, 100);
      when(products.findByIdAndBusinessId(product.getId(), businessId))
          .thenReturn(Optional.of(product));
      lines.add(new OrderService.OrderLine(product.getId(), 100));
    }
    when(orders.save(any(CustomerOrder.class))).thenAnswer(call -> call.getArgument(0));

    service.create(
        "grace-fashion",
        new OrderService.CreateOrderRequest("Customer", "677000000", "PICKUP", null, null, lines));

    verify(orders)
        .save(
            argThat(order ->
                order.getTotalAmount().compareTo(new BigDecimal("4999999999999950.00")) == 0));
  }

  @Test
  void rejectsInvalidFulfilmentStatusTransitions() {
    CustomerOrder order =
        new CustomerOrder(
            businessId,
            "KB-12345678",
            "hashed-token",
            "Customer",
            "677000000",
            "DELIVERY",
            "Address",
            null,
            new BigDecimal("100"));
    order.accept();

    assertThat(order.canUpdateFulfilment("DELIVERED")).isFalse();
    assertThat(order.canUpdateFulfilment("READY_FOR_PICKUP")).isFalse();
    assertThat(order.canUpdateFulfilment("DISPATCHED")).isTrue();
    order.updateFulfilment("DISPATCHED");
    assertThat(order.canUpdateFulfilment("PREPARING")).isFalse();
    assertThat(order.canUpdateFulfilment("DELIVERED")).isTrue();
  }

  private Product product(String name, BigDecimal price, int stock) {
    return new Product(businessId, "KB-PRODUCT", name, null, "Fashion", price, stock, null);
  }

  private Jwt jwt() {
    Instant now = Instant.now();
    return Jwt.withTokenValue("test-token")
        .header("alg", "none")
        .issuer("http://localhost:8081/realms/kamerbiz")
        .subject("vendor")
        .issuedAt(now)
        .expiresAt(now.plusSeconds(300))
        .build();
  }
}
