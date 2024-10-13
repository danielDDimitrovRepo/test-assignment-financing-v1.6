package lu.crx.financing.repository;

import lu.crx.financing.entities.Purchaser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PurchaserRepository extends JpaRepository<Purchaser, Long> {

}
