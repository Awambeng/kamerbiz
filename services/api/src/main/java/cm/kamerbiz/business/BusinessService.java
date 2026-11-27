package cm.kamerbiz.business;

import cm.kamerbiz.identity.IdentityService;
import cm.kamerbiz.identity.UserAccount;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class BusinessService {
  private static final Set<BusinessRole> PROFILE_EDITORS =
      Set.of(BusinessRole.OWNER, BusinessRole.MANAGER);

  private final IdentityService identityService;
  private final BusinessAccountRepository businessRepository;
  private final BusinessMembershipRepository membershipRepository;
  private final BusinessMembershipAuthorizationService authorizationService;

  public BusinessService(
      IdentityService identityService,
      BusinessAccountRepository businessRepository,
      BusinessMembershipRepository membershipRepository,
      BusinessMembershipAuthorizationService authorizationService) {
    this.identityService = identityService;
    this.businessRepository = businessRepository;
    this.membershipRepository = membershipRepository;
    this.authorizationService = authorizationService;
  }

  @Transactional
  public BusinessResponse create(Jwt jwt, CreateBusinessRequest request) {
    UserAccount user = identityService.synchronize(jwt);
    String name = request.name().trim();
    String slug = uniqueSlug(name);
    BusinessAccount business = businessRepository.save(new BusinessAccount(name, slug));
    membershipRepository.save(new BusinessMembership(business.getId(), user, BusinessRole.OWNER));
    return BusinessResponse.from(business, BusinessRole.OWNER);
  }

  @Transactional(readOnly = true)
  public List<BusinessResponse> listMine(Jwt jwt) {
    return membershipRepository
        .findAllByUser_IdentityIssuerAndUser_IdentitySubject(
            jwt.getIssuer().toString(), jwt.getSubject())
        .stream()
        .flatMap(membership ->
            businessRepository.findById(membership.getBusinessId()).stream()
                .map(business -> BusinessResponse.from(business, membership.getRole())))
        .toList();
  }

  @Transactional(readOnly = true)
  public BusinessResponse get(Jwt jwt, UUID businessId) {
    BusinessMembership membership =
        authorizationService.requireMembership(jwt, businessId, Set.of(BusinessRole.values()));
    return BusinessResponse.from(findBusiness(businessId), membership.getRole());
  }

  @Transactional
  public BusinessResponse update(Jwt jwt, UUID businessId, UpdateBusinessRequest request) {
    BusinessMembership membership =
        authorizationService.requireMembership(jwt, businessId, PROFILE_EDITORS);
    BusinessAccount business = findBusiness(businessId);
    String slug = request.slug() == null ? null : availableSlug(request.slug(), businessId);
    business.updateProfile(
        request.name(),
        slug,
        request.description(),
        request.logoUrl(),
        request.phone(),
        request.whatsapp(),
        request.email(),
        request.address(),
        request.city(),
        request.region(),
        request.country());
    return BusinessResponse.from(businessRepository.save(business), membership.getRole());
  }

  private BusinessAccount findBusiness(UUID id) {
    return businessRepository
        .findById(id)
        .orElseThrow(() -> new AccessDeniedException("Business access denied."));
  }

  private String uniqueSlug(String name) {
    String base = toSlug(name);
    String candidate = base;
    int suffix = 2;
    while (businessRepository.existsBySlug(candidate)) {
      String ending = "-" + suffix++;
      candidate = base.substring(0, Math.min(base.length(), 100 - ending.length())) + ending;
    }
    return candidate;
  }

  private String availableSlug(String value, UUID businessId) {
    String slug = toSlug(value);
    var existing = businessRepository.findBySlug(slug);
    if (existing.isPresent() && !existing.get().getId().equals(businessId)) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "This store address is already in use.");
    }
    return slug;
  }

  private String toSlug(String value) {
    String normalized =
        Normalizer.normalize(value, Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "")
            .toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9]+", "-")
            .replaceAll("(^-|-$)", "");
    if (normalized.isBlank()) {
      return "business";
    }
    return normalized.substring(0, Math.min(normalized.length(), 100)).replaceAll("-+$", "");
  }

  public record CreateBusinessRequest(@NotBlank @Size(max = 255) String name) {}

  public record UpdateBusinessRequest(
      @Pattern(regexp = ".*\\\\S.*") @Size(max = 255) String name,
      @Size(max = 100) String slug,
      @Size(max = 2000) String description,
      @Size(max = 1000) String logoUrl,
      @Size(max = 32) String phone,
      @Size(max = 32) String whatsapp,
      @Email @Size(max = 320) String email,
      @Size(max = 500) String address,
      @Size(max = 120) String city,
      @Size(max = 120) String region,
      @Size(max = 80) String country) {}

  public record BusinessResponse(
      UUID id,
      String name,
      String slug,
      String description,
      String logoUrl,
      String phone,
      String whatsapp,
      String email,
      String address,
      String city,
      String region,
      String country,
      BusinessRole role) {
    static BusinessResponse from(BusinessAccount business, BusinessRole role) {
      return new BusinessResponse(
          business.getId(),
          business.getName(),
          business.getSlug(),
          business.getDescription(),
          business.getLogoUrl(),
          business.getPhone(),
          business.getWhatsapp(),
          business.getEmail(),
          business.getAddress(),
          business.getCity(),
          business.getRegion(),
          business.getCountry(),
          role);
    }
  }
}
