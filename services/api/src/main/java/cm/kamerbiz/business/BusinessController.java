package cm.kamerbiz.business;

import cm.kamerbiz.business.BusinessService.BusinessResponse;
import cm.kamerbiz.business.BusinessService.CreateBusinessRequest;
import cm.kamerbiz.business.BusinessService.UpdateBusinessRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/businesses")
public class BusinessController {
  private final BusinessService businessService;

  public BusinessController(BusinessService businessService) {
    this.businessService = businessService;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public BusinessResponse create(
      @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreateBusinessRequest request) {
    return businessService.create(jwt, request);
  }

  @GetMapping
  public List<BusinessResponse> listMine(@AuthenticationPrincipal Jwt jwt) {
    return businessService.listMine(jwt);
  }

  @GetMapping("/{businessId}")
  public BusinessResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID businessId) {
    return businessService.get(jwt, businessId);
  }

  @PatchMapping("/{businessId}")
  public BusinessResponse update(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID businessId,
      @Valid @RequestBody UpdateBusinessRequest request) {
    return businessService.update(jwt, businessId, request);
  }
}
