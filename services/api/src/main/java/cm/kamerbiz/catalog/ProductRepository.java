package cm.kamerbiz.catalog;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, UUID> {
  List<Product> findAllByBusinessIdAndArchivedOrderByUpdatedAtDesc(
      UUID businessId, boolean archived);

  List<Product> findAllByBusinessIdOrderByUpdatedAtDesc(UUID businessId);

  Optional<Product> findByIdAndBusinessId(UUID id, UUID businessId);

  boolean existsByBusinessIdAndProductCode(UUID businessId, String productCode);

  boolean existsByBusinessIdAndImageUrl(UUID businessId, String imageUrl);
}
