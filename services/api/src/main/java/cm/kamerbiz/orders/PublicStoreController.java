package cm.kamerbiz.orders;

import cm.kamerbiz.orders.OrderService.CreateOrderRequest;
import cm.kamerbiz.orders.OrderService.OrderCreated;
import cm.kamerbiz.orders.OrderService.StoreResponse;
import cm.kamerbiz.orders.OrderService.TrackingResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public")
public class PublicStoreController {
  private final OrderService service;

  public PublicStoreController(OrderService service) {
    this.service = service;
  }

  @GetMapping("/stores/{slug}")
  public StoreResponse store(@PathVariable String slug) {
    return service.store(slug);
  }

  @PostMapping("/stores/{slug}/orders")
  @ResponseStatus(HttpStatus.CREATED)
  public OrderCreated order(
      @PathVariable String slug, @Valid @RequestBody CreateOrderRequest request) {
    return service.create(slug, request);
  }

  @GetMapping("/orders/track/{token}")
  public TrackingResponse track(@PathVariable String token) {
    return service.track(token);
  }
}
