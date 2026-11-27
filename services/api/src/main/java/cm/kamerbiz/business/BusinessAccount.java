package cm.kamerbiz.business;

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
@Table(name = "business_account", uniqueConstraints = @UniqueConstraint(columnNames = "slug"))
public class BusinessAccount {
  @Id private UUID id;

  @Column(nullable = false, length = 255)
  private String name;

  @Column(nullable = false, length = 100)
  private String slug;

  @Column(length = 2000)
  private String description;

  @Column(name = "logo_url", length = 1000)
  private String logoUrl;

  @Column(length = 32)
  private String phone;

  @Column(length = 32)
  private String whatsapp;

  @Column(length = 320)
  private String email;

  @Column(length = 500)
  private String address;

  @Column(length = 120)
  private String city;

  @Column(length = 120)
  private String region;

  @Column(nullable = false, length = 80)
  private String country = "Cameroon";

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected BusinessAccount() {}

  public BusinessAccount(String name, String slug) {
    this.id = UUID.randomUUID();
    this.name = name;
    this.slug = slug;
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

  public void updateProfile(
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
      String country) {
    if (name != null) {
      this.name = name;
    }
    if (slug != null) {
      this.slug = slug;
    }
    if (description != null) {
      this.description = description;
    }
    if (logoUrl != null) {
      this.logoUrl = logoUrl;
    }
    if (phone != null) {
      this.phone = phone;
    }
    if (whatsapp != null) {
      this.whatsapp = whatsapp;
    }
    if (email != null) {
      this.email = email;
    }
    if (address != null) {
      this.address = address;
    }
    if (city != null) {
      this.city = city;
    }
    if (region != null) {
      this.region = region;
    }
    if (country != null) {
      this.country = country;
    }
  }

  public UUID getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getSlug() {
    return slug;
  }

  public String getDescription() {
    return description;
  }

  public String getLogoUrl() {
    return logoUrl;
  }

  public String getPhone() {
    return phone;
  }

  public String getWhatsapp() {
    return whatsapp;
  }

  public String getEmail() {
    return email;
  }

  public String getAddress() {
    return address;
  }

  public String getCity() {
    return city;
  }

  public String getRegion() {
    return region;
  }

  public String getCountry() {
    return country;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
