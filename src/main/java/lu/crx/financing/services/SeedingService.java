package lu.crx.financing.services;

import java.time.LocalDate;
import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import lu.crx.financing.entities.Creditor;
import lu.crx.financing.entities.Debtor;
import lu.crx.financing.entities.Invoice;
import lu.crx.financing.entities.Purchaser;
import lu.crx.financing.entities.PurchaserFinancingSettings;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SeedingService {

    private EntityManager entityManager;

    private Creditor creditor1;
    private Creditor creditor2;
    private Creditor creditor3;

    private Debtor debtor1;
    private Debtor debtor2;
    private Debtor debtor3;

    private Purchaser purchaser1;
    private Purchaser purchaser2;
    private Purchaser purchaser3;

    public SeedingService(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Transactional
    public void seedMasterData() {
        log.info("Seeding master data");

        /**
         * Since creditors, purchasers and debtors are unique in the DB - we're checking if they already exist
         * in case this is a subsequent application run.
         *
         * In order to check if a result exists, we need to call getResultList, rather than getSingleResult,
         * which throws an exception if not found
         */

        // creditors
        List<Creditor> coffeeBeansLlc = entityManager.createQuery("SELECT c FROM Creditor c WHERE c.name = ?1", Creditor.class)
                .setParameter(1, "Coffee Beans LLC")
                .getResultList();

        if (coffeeBeansLlc.isEmpty()) {
            creditor1 = Creditor.builder()
                    .name("Coffee Beans LLC")
                    .maxFinancingRateInBps(5)
                    .build();
            entityManager.persist(creditor1);
        } else {
            creditor1 = coffeeBeansLlc.getFirst();
        }

        List<Creditor> homeBrew = entityManager.createQuery("SELECT c FROM Creditor c WHERE c.name = ?1", Creditor.class)
                .setParameter(1, "Home Brew")
                .getResultList();

        if (homeBrew.isEmpty()) {
            creditor2 = Creditor.builder()
                    .name("Home Brew")
                    .maxFinancingRateInBps(3)
                    .build();
            entityManager.persist(creditor2);
        } else {
            creditor2 = homeBrew
                    .getFirst();
        }

        List<Creditor> beanstalk = entityManager.createQuery("SELECT c FROM Creditor c WHERE c.name = ?1", Creditor.class)
                .setParameter(1, "Beanstalk")
                .getResultList();

        if (beanstalk.isEmpty()) {
            creditor3 = Creditor.builder()
                    .name("Beanstalk")
                    .maxFinancingRateInBps(2)
                    .build();
            entityManager.persist(creditor3);
        } else {
            creditor3 = beanstalk
                    .getFirst();
        }

        // debtors
        List<Debtor> chocolateFactory = entityManager.createQuery("SELECT d FROM Debtor d WHERE d.name = ?1", Debtor.class)
                .setParameter(1, "Chocolate Factory")
                .getResultList();

        if (chocolateFactory.isEmpty()) {
            debtor1 = Debtor.builder()
                    .name("Chocolate Factory")
                    .build();
            entityManager.persist(debtor1);
        } else {
            debtor1 = chocolateFactory
                    .getFirst();
        }

        List<Debtor> sweetsInc = entityManager.createQuery("SELECT d FROM Debtor d WHERE d.name = ?1", Debtor.class)
                .setParameter(1, "Sweets Inc")
                .getResultList();

        if (sweetsInc.isEmpty()) {
            debtor2 = Debtor.builder()
                    .name("Sweets Inc")
                    .build();
            entityManager.persist(debtor2);
        } else {
            debtor2 = sweetsInc
                    .getFirst();
        }

        List<Debtor> chocoLoco = entityManager.createQuery("SELECT d FROM Debtor d WHERE d.name = ?1", Debtor.class)
                .setParameter(1, "ChocoLoco")
                .getResultList();

        if (chocoLoco.isEmpty()) {
            debtor3 = Debtor.builder()
                    .name("ChocoLoco")
                    .build();
            entityManager.persist(debtor3);
        } else {
            debtor3 = chocoLoco
                    .getFirst();
        }

        // purchasers
        List<Purchaser> richBank = entityManager.createQuery("SELECT p FROM Purchaser p WHERE p.name = ?1", Purchaser.class)
                .setParameter(1, "RichBank")
                .getResultList();

        if (richBank.isEmpty()) {
            entityManager.persist(Purchaser.builder()
                    .name("RichBank")
                    .minimumFinancingTermInDays(10)
                    .purchaserFinancingSetting(PurchaserFinancingSettings.builder()
                            .creditor(creditor1)
                            .annualRateInBps(50)
                            .build())
                    .purchaserFinancingSetting(PurchaserFinancingSettings.builder()
                            .creditor(creditor2)
                            .annualRateInBps(60)
                            .build())
                    .purchaserFinancingSetting(PurchaserFinancingSettings.builder()
                            .creditor(creditor3)
                            .annualRateInBps(30)
                            .build())
                    .build());
        }

        List<Purchaser> fatBank = entityManager.createQuery("SELECT p FROM Purchaser p WHERE p.name = ?1", Purchaser.class)
                .setParameter(1, "FatBank")
                .getResultList();

        if (fatBank.isEmpty()) {
            entityManager.persist(Purchaser.builder()
                    .name("FatBank")
                    .minimumFinancingTermInDays(12)
                    .purchaserFinancingSetting(PurchaserFinancingSettings.builder()
                            .creditor(creditor1)
                            .annualRateInBps(40)
                            .build())
                    .purchaserFinancingSetting(PurchaserFinancingSettings.builder()
                            .creditor(creditor2)
                            .annualRateInBps(80)
                            .build())
                    .purchaserFinancingSetting(PurchaserFinancingSettings.builder()
                            .creditor(creditor3)
                            .annualRateInBps(25)
                            .build())
                    .build());
        }

        List<Purchaser> megaBank = entityManager.createQuery("SELECT p FROM Purchaser p WHERE p.name = ?1", Purchaser.class)
                .setParameter(1, "MegaBank")
                .getResultList();

        if (megaBank.isEmpty()) {
            entityManager.persist(Purchaser.builder()
                    .name("MegaBank")
                    .minimumFinancingTermInDays(8)
                    .purchaserFinancingSetting(PurchaserFinancingSettings.builder()
                            .creditor(creditor1)
                            .annualRateInBps(30)
                            .build())
                    .purchaserFinancingSetting(PurchaserFinancingSettings.builder()
                            .creditor(creditor2)
                            .annualRateInBps(50)
                            .build())
                    .purchaserFinancingSetting(PurchaserFinancingSettings.builder()
                            .creditor(creditor3)
                            .annualRateInBps(45)
                            .build())
                    .build());
        }

    }

    @Transactional
    public void seedInvoices() {
        log.info("Seeding the invoices");

        entityManager.persist(Invoice.builder()
                .creditor(creditor1)
                .debtor(debtor1)
                .valueInCents(200000)
                .maturityDate(LocalDate.now().plusDays(52))
                .build());

        entityManager.persist(Invoice.builder()
                .creditor(creditor1)
                .debtor(debtor2)
                .valueInCents(800000)
                .maturityDate(LocalDate.now().plusDays(33))
                .build());

        entityManager.persist(Invoice.builder()
                .creditor(creditor1)
                .debtor(debtor3)
                .valueInCents(600000)
                .maturityDate(LocalDate.now().plusDays(43))
                .build());

        entityManager.persist(Invoice.builder()
                .creditor(creditor1)
                .debtor(debtor1)
                .valueInCents(500000)
                .maturityDate(LocalDate.now().plusDays(80))
                .build());

        entityManager.persist(Invoice.builder()
                .creditor(creditor1)
                .debtor(debtor2)
                .valueInCents(6000000)
                .maturityDate(LocalDate.now().plusDays(5))
                .build());

        entityManager.persist(Invoice.builder()
                .creditor(creditor2)
                .debtor(debtor3)
                .valueInCents(500000)
                .maturityDate(LocalDate.now().plusDays(10))
                .build());

        entityManager.persist(Invoice.builder()
                .creditor(creditor2)
                .debtor(debtor1)
                .valueInCents(800000)
                .maturityDate(LocalDate.now().plusDays(15))
                .build());

        entityManager.persist(Invoice.builder()
                .creditor(creditor2)
                .debtor(debtor2)
                .valueInCents(9000000)
                .maturityDate(LocalDate.now().plusDays(30))
                .build());

        entityManager.persist(Invoice.builder()
                .creditor(creditor2)
                .debtor(debtor3)
                .valueInCents(450000)
                .maturityDate(LocalDate.now().plusDays(32))
                .build());

        entityManager.persist(Invoice.builder()
                .creditor(creditor2)
                .debtor(debtor1)
                .valueInCents(800000)
                .maturityDate(LocalDate.now().plusDays(11))
                .build());

        entityManager.persist(Invoice.builder()
                .creditor(creditor3)
                .debtor(debtor2)
                .valueInCents(3000000)
                .maturityDate(LocalDate.now().plusDays(10))
                .build());

        entityManager.persist(Invoice.builder()
                .creditor(creditor3)
                .debtor(debtor3)
                .valueInCents(5000000)
                .maturityDate(LocalDate.now().plusDays(14))
                .build());

        entityManager.persist(Invoice.builder()
                .creditor(creditor3)
                .debtor(debtor1)
                .valueInCents(9000000)
                .maturityDate(LocalDate.now().plusDays(23))
                .build());

        entityManager.persist(Invoice.builder()
                .creditor(creditor3)
                .debtor(debtor2)
                .valueInCents(800000)
                .maturityDate(LocalDate.now().plusDays(18))
                .build());

        entityManager.persist(Invoice.builder()
                .creditor(creditor3)
                .debtor(debtor3)
                .valueInCents(9000000)
                .maturityDate(LocalDate.now().plusDays(50))
                .build());
    }

}
