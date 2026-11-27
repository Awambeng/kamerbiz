package cm.kamerbiz.business;

import cm.kamerbiz.identity.UserAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(
    name = "business_membership",
    uniqueConstraints = @UniqueConstraint(columnNames = {"business_id", "user_id"}))
public class BusinessMembership {
  @Id private UUID id;

  @Column(name = "business_id", nullable = false)
  private UUID businessId;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private UserAccount user;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private BusinessRole role;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected BusinessMembership() {}

  public BusinessMembership(UUID businessId, UserAccount user, BusinessRole role) {
    this.id = UUID.randomUUID();
    this.businessId = Objects.requireNonNull(businessId);
    this.user = Objects.requireNonNull(user);
    this.role = Objects.requireNonNull(role);
    this.createdAt = Instant.now();
  }

  public UUID getId() {
    return id;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public UUID getBusinessId() {
    return businessId;
  }

  public UserAccount getUser() {
    return user;
  }

  public BusinessRole getRole() {
    return role;
  }
}
