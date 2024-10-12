package lu.crx.financing.services;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.*;
import lu.crx.financing.entities.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static lu.crx.financing.entities.constants.InvoiceStatus.FINANCED;
import static lu.crx.financing.entities.constants.InvoiceStatus.NON_FINANCED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
public class FinancingServicePerformanceTest {

    private static final LocalDate NOW = LocalDate.now();

    @Autowired
    private EntityManagerFactory entityManagerFactory;
    private EntityManager entityManager;
    private Query count;

    @Autowired
    private FinancingService financingService;

    private final List<Creditor> creditors = new ArrayList<>();
    private final List<Purchaser> purchasers = new ArrayList<>();
    private final Debtor debtor = Debtor.builder().name("BadAtFinancing").build();

    /**
     * Using @PostConstruct instead of @BeforeEach in order to exclude DB setup from the test execution time report
     */
    @PostConstruct
    void setUp() {
        entityManager = entityManagerFactory.createEntityManager();
        entityManager.setFlushMode(FlushModeType.COMMIT);

        EntityTransaction tx = entityManager.getTransaction();

        tx.begin();
        seedMasterData();
        seedInvoices();
        tx.commit();

        count = entityManager.createQuery("SELECT COUNT(*) FROM Invoice i WHERE i.status = ?1");
        long nonFinancedCount = (long) count.setParameter(1, NON_FINANCED).getSingleResult();
        long financedCount = (long) count.setParameter(1, FINANCED).getSingleResult();

        if (nonFinancedCount != 10000L || financedCount != 1000000) {
            fail(String.format("Expected DB state with 10k non-financed and 1mil financed, instead got %d and %d",
                    nonFinancedCount, financedCount));
        }
    }

    @Test
    void given1milFinanced10kNonFinanced100PurchasersAndCreditors_whenFinance_finance10kNonFinancedInUnder30Sec() {

        long start = System.currentTimeMillis();

        financingService.finance();

        long end = System.currentTimeMillis();

        assertThat(end - start).isLessThan(30000);

        count = entityManager.createQuery("SELECT COUNT(*) FROM Invoice i WHERE i.status = ?1");
        long nonFinancedCount = (long) count.setParameter(1, NON_FINANCED).getSingleResult();
        long financedCount = (long) count.setParameter(1, FINANCED).getSingleResult();

        assertThat(nonFinancedCount).isZero();
        assertThat(financedCount).isEqualTo(1010000);
    }

    private void seedMasterData() {
        entityManager.persist(debtor);

        IntStream.rangeClosed(0, 99).forEach(i -> {

            Creditor creditor = Creditor.builder()
                    .name("Creditor_" + i)
                    .maxFinancingRateInBps(3)
                    .build();
            creditors.add(creditor);
            entityManager.persist(creditor);

            Purchaser purchaser = Purchaser.builder()
                    .name("Purchaser_" + i)
                    .minimumFinancingTermInDays(10)
                    .purchaserFinancingSetting(PurchaserFinancingSettings.builder()
                            .creditor(creditor)
                            .annualRateInBps(40)
                            .build())
                    .build();
            purchasers.add(purchaser);
            entityManager.persist(purchaser);

        });

    }

    private void seedInvoices() {

        IntStream.rangeClosed(0, 9999).forEach(i -> {

            entityManager.persist(Invoice.builder()
                    .creditor(creditors.get(i % creditors.size()))
                    .debtor(debtor)
                    .valueInCents(1000000)
                    .maturityDate(NOW.plusDays(10))
                    .build());

            IntStream.rangeClosed(0, 99).forEach(x -> entityManager.persist(
                    Invoice.builder()
                            .creditor(creditors.get(x))
                            .debtor(debtor)
                            .valueInCents(1000000)
                            .maturityDate(NOW.plusDays(10))
                            .status(FINANCED)
                            .build()));
        });

    }

}
