package cm.kamerbiz.identity;

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
import java.util.UUID;

@Entity
@Table(
    name = "user_platform_role",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "role"}))
public class UserPlatformRole {
  @Id private UUID id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private UserAccount user;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private PlatformRole role;

  protected UserPlatformRole() {}

  public UserPlatformRole(UserAccount user, PlatformRole role) {
    this.id = UUID.randomUUID();
    this.user = user;
    this.role = role;
  }

  public UUID getId() {
    return id;
  }

  public UserAccount getUser() {
    return user;
  }

  public PlatformRole getRole() {
    return role;
  }
}
