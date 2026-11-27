package cm.kamerbiz.identity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "user_account",
    uniqueConstraints = @UniqueConstraint(columnNames = {"identity_issuer", "identity_subject"}))
public class UserAccount {
  @Id private UUID id;

  @Column(name = "identity_issuer", nullable = false, length = 512)
  private String identityIssuer;

  @Column(name = "identity_subject", nullable = false, length = 255)
  private String identitySubject;

  @Column(length = 320)
  private String email;

  @Column(name = "display_name", length = 255)
  private String displayName;

  @Column(name = "email_verified", nullable = false)
  private boolean emailVerified;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected UserAccount() {}

  public UserAccount(String identityIssuer, String identitySubject) {
    this.id = UUID.randomUUID();
    this.identityIssuer = identityIssuer;
    this.identitySubject = identitySubject;
  }

  @PrePersist
  void onCreate() {
    Instant now = Instant.now();
    createdAt = now;
    updatedAt = now;
  }

  @PreUpdate
  void onUpdate() {
    updatedAt = Instant.now();
  }

  public void updateProfile(String email, String displayName, boolean emailVerified) {
    this.email = email;
    this.displayName = displayName;
    this.emailVerified = emailVerified;
  }

  public UUID getId() {
    return id;
  }

  public String getIdentityIssuer() {
    return identityIssuer;
  }

  public String getIdentitySubject() {
    return identitySubject;
  }

  public String getEmail() {
    return email;
  }

  public String getDisplayName() {
    return displayName;
  }

  public boolean isEmailVerified() {
    return emailVerified;
  }
}
