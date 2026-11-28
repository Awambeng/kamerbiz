package cm.kamerbiz.catalog;

import cm.kamerbiz.catalog.ProductService.ImportPreviewResponse;
import cm.kamerbiz.catalog.ProductService.ImportResponse;
import cm.kamerbiz.catalog.ProductService.ProductRequest;
import cm.kamerbiz.catalog.ProductService.ProductResponse;
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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/businesses/{businessId}/products")
public class ProductController {
  private final ProductService service;

  public ProductController(ProductService service) {
    this.service = service;
  }

  @GetMapping
  public List<ProductResponse> list(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID businessId,
      @RequestParam(defaultValue = "false") boolean includeArchived) {
    return service.list(jwt, businessId, includeArchived);
  }

  @PostMapping(value = "/import/preview", consumes = "text/csv")
  public ImportPreviewResponse previewImport(
      @AuthenticationPrincipal Jwt jwt, @PathVariable UUID businessId, @RequestBody String csv) {
    return service.previewCsv(jwt, businessId, csv);
  }

  @PostMapping(value = "/import", consumes = "text/csv")
  public ImportResponse importCsv(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID businessId,
      @RequestHeader("X-Import-Key") String batchKey,
      @RequestBody String csv) {
    return service.importCsv(jwt, businessId, batchKey, csv);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ProductResponse create(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID businessId,
      @Valid @RequestBody ProductRequest request) {
    return service.create(jwt, businessId, request);
  }

  @PatchMapping("/{productId}")
  public ProductResponse update(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID businessId,
      @PathVariable UUID productId,
      @Valid @RequestBody ProductRequest request) {
    return service.update(jwt, businessId, productId, request);
  }

  @PostMapping("/{productId}/archive")
  public ProductResponse archive(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID businessId,
      @PathVariable UUID productId) {
    return service.setArchived(jwt, businessId, productId, true);
  }

  @PostMapping("/{productId}/restore")
  public ProductResponse restore(
      @AuthenticationPrincipal Jwt jwt,
      @PathVariable UUID businessId,
      @PathVariable UUID productId) {
    return service.setArchived(jwt, businessId, productId, false);
  }
}
