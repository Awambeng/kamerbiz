package cm.kamerbiz.orders;

import cm.kamerbiz.orders.OrderService.VendorOrderResponse;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/businesses/{businessId}/orders")
public class VendorOrderController {
  private final OrderService service;

  public VendorOrderController(OrderService service) {
    this.service = service;
  }

  @GetMapping
  public List<VendorOrderResponse> list(
      @AuthenticationPrincipal Jwt jwt, @PathVariable UUID businessId) {
    return service.list(jwt, businessId);
  }

  @PostMapping("/{orderId}/decision")
  public VendorOrderResponse decide(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID businessId,
      @PathVariable UUID orderId,
      @RequestParam String action) {
    return service.decide(jwt, businessId, orderId, action.toUpperCase());
  }

  @PatchMapping("/{orderId}/fulfilment")
  public VendorOrderResponse fulfil(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID businessId,
      @PathVariable UUID orderId,
      @RequestParam String status) {
    return service.fulfil(jwt, businessId, orderId, status.toUpperCase());
  }
}
