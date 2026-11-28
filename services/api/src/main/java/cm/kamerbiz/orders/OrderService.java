package cm.kamerbiz.orders;

import cm.kamerbiz.business.BusinessAccount;
import cm.kamerbiz.business.BusinessAccountRepository;
import cm.kamerbiz.business.BusinessMembershipAuthorizationService;
import cm.kamerbiz.business.BusinessRole;
import cm.kamerbiz.catalog.Product;
import cm.kamerbiz.catalog.ProductRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class OrderService {
  private static final Set<BusinessRole> VIEWERS = Set.of(BusinessRole.values());
  private static final Set<BusinessRole> EDITORS =
      Set.of(BusinessRole.OWNER, BusinessRole.MANAGER, BusinessRole.STAFF);
  private static final SecureRandom RANDOM = new SecureRandom();

  private final BusinessAccountRepository businesses;
  private final ProductRepository products;
  private final CustomerOrderRepository orders;
  private final CustomerOrderItemRepository items;
  private final OrderStatusEventRepository events;
  private final BusinessMembershipAuthorizationService authorization;

  public OrderService(
      BusinessAccountRepository businesses,
      ProductRepository products,
      CustomerOrderRepository orders,
      CustomerOrderItemRepository items,
      OrderStatusEventRepository events,
      BusinessMembershipAuthorizationService authorization) {
    this.businesses = businesses;
    this.products = products;
    this.orders = orders;
    this.items = items;
    this.events = events;
    this.authorization = authorization;
  }

  @Transactional(readOnly = true)
  public StoreResponse store(String slug) {
    BusinessAccount business =
        businesses
            .findBySlug(slug)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    List<StoreProduct> visible =
        products
            .findAllByBusinessIdAndArchivedOrderByUpdatedAtDesc(business.getId(), false)
            .stream()
            .map(product ->
                new StoreProduct(
                    product.getId(),
                    product.getName(),
                    product.getDescription(),
                    product.getCategory(),
                    product.getPrice(),
                    product.getStockQuantity(),
                    product.getImageUrl()))
            .toList();
    return new StoreResponse(
        business.getName(),
        business.getSlug(),
        business.getDescription(),
        business.getLogoUrl(),
        business.getPhone(),
        business.getWhatsapp(),
        business.getCity(),
        business.getRegion(),
        visible);
  }

  @Transactional
  public OrderCreated create(String slug, @Valid CreateOrderRequest request) {
    BusinessAccount business =
        businesses
            .findBySlug(slug)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    if (request.items() == null || request.items().isEmpty() || request.items().size() > 50) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose at least one product.");
    }
    if (!Set.of("DELIVERY", "PICKUP").contains(request.fulfilmentMethod())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
    }
    if ("DELIVERY".equals(request.fulfilmentMethod())
        && (request.deliveryAddress() == null || request.deliveryAddress().isBlank())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Delivery address is required.");
    }

    Set<UUID> ids = new HashSet<>();
    BigDecimal total = BigDecimal.ZERO;
    List<Product> selected = new ArrayList<>();
    for (OrderLine line : request.items()) {
      if (line.quantity() < 1 || line.quantity() > 100 || !ids.add(line.productId())) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid order items.");
      }
      Product product =
          products
              .findByIdAndBusinessId(line.productId(), business.getId())
              .orElseThrow(() ->
                  new ResponseStatusException(HttpStatus.BAD_REQUEST, "A product is unavailable."));
      if (product.isArchived() || product.getStockQuantity() < line.quantity()) {
        throw new ResponseStatusException(
            HttpStatus.CONFLICT, "A product no longer has enough stock.");
      }
      total = total.add(product.getPrice().multiply(BigDecimal.valueOf(line.quantity())));
      selected.add(product);
    }

    String trackingToken = token();
    String trackingTokenHash = hashToken(trackingToken);
    String reference =
        "KB-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase(Locale.ROOT);
    CustomerOrder order =
        orders.save(
            new CustomerOrder(
                business.getId(),
                reference,
                trackingTokenHash,
                request.customerName().trim(),
                request.phone().trim(),
                request.fulfilmentMethod(),
                clean(request.deliveryAddress()),
                clean(request.note()),
                total));
    events.save(new OrderStatusEvent(order.getId(), business.getId(), "ORDER", null, "NEW", null));
    for (int index = 0; index < selected.size(); index++) {
      Product product = selected.get(index);
      items.save(
          new CustomerOrderItem(
              order.getId(),
              product.getId(),
              product.getName(),
              product.getPrice(),
              request.items().get(index).quantity()));
    }
    return new OrderCreated(reference, "/track/" + trackingToken);
  }

  @Transactional(readOnly = true)
  public TrackingResponse track(String token) {
    CustomerOrder order =
        orders
            .findByTrackingTokenHash(hashToken(token))
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    return new TrackingResponse(
        order.getReference(), order.getOrderStatus(), order.getFulfilmentStatus());
  }

  @Transactional(readOnly = true)
  public List<VendorOrderResponse> list(Jwt jwt, UUID businessId) {
    authorization.requireMembership(jwt, businessId, VIEWERS);
    List<CustomerOrder> businessOrders = orders.findAllByBusinessIdOrderByCreatedAtDesc(businessId);
    if (businessOrders.isEmpty()) {
      return List.of();
    }
    Map<UUID, List<CustomerOrderItem>> itemsByOrder =
        items
            .findAllByOrderIdIn(businessOrders.stream().map(CustomerOrder::getId).toList())
            .stream()
            .collect(Collectors.groupingBy(CustomerOrderItem::getOrderId));
    return businessOrders.stream()
        .map(order -> vendorResponse(order, itemsByOrder.getOrDefault(order.getId(), List.of())))
        .toList();
  }

  @Transactional
  public VendorOrderResponse decide(Jwt jwt, UUID businessId, UUID orderId, String decision) {
    var actor = authorization.requireMembership(jwt, businessId, EDITORS);
    CustomerOrder order =
        orders
            .findForUpdate(orderId, businessId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    if (!"NEW".equals(order.getOrderStatus())) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "This order has already been reviewed.");
    }
    String previousStatus = order.getOrderStatus();
    if ("ACCEPT".equals(decision)) {
      reserveStock(order, businessId);
      order.accept();
    } else if ("REJECT".equals(decision)) {
      order.reject();
    } else {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
    }
    events.save(
        new OrderStatusEvent(
            order.getId(),
            businessId,
            "ORDER",
            previousStatus,
            order.getOrderStatus(),
            actor.getUser().getId()));
    return vendorResponse(order);
  }

  @Transactional
  public VendorOrderResponse fulfil(Jwt jwt, UUID businessId, UUID orderId, String status) {
    var actor = authorization.requireMembership(jwt, businessId, EDITORS);
    CustomerOrder order =
        orders
            .findForUpdate(orderId, businessId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    String previousStatus = order.getFulfilmentStatus();
    if (!order.canUpdateFulfilment(status)) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Invalid fulfilment status transition.");
    }
    order.updateFulfilment(status);
    events.save(
        new OrderStatusEvent(
            order.getId(),
            businessId,
            "FULFILMENT",
            previousStatus,
            order.getFulfilmentStatus(),
            actor.getUser().getId()));
    return vendorResponse(order);
  }

  private void reserveStock(CustomerOrder order, UUID businessId) {
    for (CustomerOrderItem item :
        items.findAllByOrderId(order.getId()).stream()
            .sorted((left, right) -> left.getProductId().compareTo(right.getProductId()))
            .toList()) {
      Product product =
          products
              .findForUpdate(item.getProductId(), businessId)
              .orElseThrow(() ->
                  new ResponseStatusException(
                      HttpStatus.CONFLICT, "A product is no longer available."));
      if (product.isArchived() || product.getStockQuantity() < item.getQuantity()) {
        throw new ResponseStatusException(
            HttpStatus.CONFLICT, "There is not enough stock to accept this order.");
      }
      product.decreaseStock(item.getQuantity());
    }
  }

  private VendorOrderResponse vendorResponse(CustomerOrder order) {
    return vendorResponse(order, items.findAllByOrderId(order.getId()));
  }

  private VendorOrderResponse vendorResponse(
      CustomerOrder order, List<CustomerOrderItem> sourceItems) {
    List<OrderItemResponse> orderItems =
        sourceItems.stream()
            .map(item ->
                new OrderItemResponse(
                    item.getProductName(),
                    item.getUnitPrice(),
                    item.getQuantity(),
                    item.getLineTotal()))
            .toList();
    return new VendorOrderResponse(
        order.getId(),
        order.getReference(),
        order.getCustomerName(),
        order.getPhone(),
        order.getFulfilmentMethod(),
        order.getDeliveryAddress(),
        order.getCustomerNote(),
        order.getOrderStatus(),
        order.getFulfilmentStatus(),
        order.getTotalAmount(),
        order.getCreatedAt(),
        orderItems);
  }

  private String hashToken(String token) {
    try {
      byte[] digest =
          MessageDigest.getInstance("SHA-256")
              .digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8));
      return java.util.HexFormat.of().formatHex(digest);
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 is unavailable.", exception);
    }
  }

  private String token() {
    byte[] bytes = new byte[32];
    RANDOM.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  private String clean(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }

  public record OrderLine(@NotNull UUID productId, @Min(1) @Max(100) int quantity) {}

  public record CreateOrderRequest(
      @NotBlank @Size(max = 160) String customerName,
      @NotBlank @Size(max = 32) String phone,
      @NotBlank String fulfilmentMethod,
      @Size(max = 1000) String deliveryAddress,
      @Size(max = 500) String note,
      @NotEmpty @Size(max = 50) List<@Valid OrderLine> items) {}

  public record StoreProduct(
      UUID id,
      String name,
      String description,
      String category,
      BigDecimal price,
      int stockQuantity,
      String imageUrl) {}

  public record StoreResponse(
      String name,
      String slug,
      String description,
      String logoUrl,
      String phone,
      String whatsapp,
      String city,
      String region,
      List<StoreProduct> products) {}

  public record OrderCreated(String reference, String trackingUrl) {}

  public record TrackingResponse(String reference, String status, String fulfilmentStatus) {}

  public record OrderItemResponse(
      String name, BigDecimal unitPrice, int quantity, BigDecimal total) {}

  public record VendorOrderResponse(
      UUID id,
      String reference,
      String customerName,
      String phone,
      String fulfilmentMethod,
      String deliveryAddress,
      String note,
      String status,
      String fulfilmentStatus,
      BigDecimal total,
      Instant createdAt,
      List<OrderItemResponse> items) {}
}
