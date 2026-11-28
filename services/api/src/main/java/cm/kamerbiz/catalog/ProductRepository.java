package cm.kamerbiz.catalog;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, UUID> {
  List<Product> findAllByBusinessIdAndArchivedOrderByUpdatedAtDesc(
      UUID businessId, boolean archived);

  List<Product> findAllByBusinessIdOrderByUpdatedAtDesc(UUID businessId);

  Optional<Product> findByIdAndBusinessId(UUID id, UUID businessId);

  boolean existsByBusinessIdAndProductCode(UUID businessId, String productCode);

  boolean existsByBusinessIdAndImageUrl(UUID businessId, String imageUrl);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select p from Product p where p.id = :id and p.businessId = :businessId")
  Optional<Product> findForUpdate(@Param("id") UUID id, @Param("businessId") UUID businessId);
}
