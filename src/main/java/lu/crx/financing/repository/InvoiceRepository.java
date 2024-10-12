package lu.crx.financing.repository;

import lu.crx.financing.entities.Invoice;
import lu.crx.financing.entities.constants.InvoiceStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InvoiceRepository extends PagingAndSortingRepository<Invoice, Long>, CrudRepository<Invoice, Long> {

    Slice<Invoice> findAllByStatus(InvoiceStatus status, Pageable pageable);

}
