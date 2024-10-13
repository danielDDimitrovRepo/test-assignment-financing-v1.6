package lu.crx.financing.services;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lu.crx.financing.entities.Invoice;
import lu.crx.financing.entities.Purchaser;
import lu.crx.financing.entities.PurchaserFinancingSettings;
import lu.crx.financing.repository.InvoiceRepository;
import lu.crx.financing.repository.PurchaserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import static java.lang.Math.round;
import static lu.crx.financing.entities.constants.InvoiceStatus.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class FinancingService {

    private static final int BANKING_YEAR_IN_DAYS = 360;

    @Value("${financing.invoice.batch_size}")
    private int invoicesBatchSize;

    private final InvoiceRepository invoiceRepository;
    private final PurchaserRepository purchaserRepository;

    /**
     * This method reads non-financed invoice batches from the DB and processes them 1 by 1.
     * It also caches all Purchasers, which is needed for the invoice financing calculations.
     * <p>
     * NOTE: It assumes that it can cache all Purchasers in order to find corresponding Creditor settings.
     * <p>
     * Two other alternatives for not caching all Purchasers:
     * 1. Perform DB join for each invoice to find Purchasers that have Creditor config. Downside: Huge performance hit
     * 2. Search for Purchaser->Creditor configs for all invoices from current batch.
     * Downside: Complicated/Overengineering if not really needed
     */
    @Transactional
    public void finance() {
        log.info("Financing started");

        Slice<Invoice> invoiceSlice =
                invoiceRepository.findAllByStatus(NON_FINANCED, PageRequest.of(0, invoicesBatchSize));

        if (invoiceSlice.isEmpty()) {
            log.info("Cannot find any non-financed invoices");
            return;
        }

        List<Purchaser> purchasers = purchaserRepository.findAll();
        if (purchasers.isEmpty()) {
            log.info("No purchasers in DB yet");
            return;
        }

        financeInvoices(invoiceSlice.getContent(), purchasers);

        while (invoiceSlice.hasNext()) {
            invoiceSlice = invoiceRepository.findAllByStatus(NON_FINANCED, invoiceSlice.nextPageable());
            financeInvoices(invoiceSlice.getContent(), purchasers);
        }

        log.info("Financing completed");
    }

    /**
     * This method is responsible for the financing of all invoices that are passed to it.
     * It persists the following for each successful financing:
     * - early payment in cents
     * - purchaser
     * - financing rate in BPS
     * - financing term
     * - financing date
     * - status FINANCED
     * <p>
     * NOTE 1: It sets corresponding {@link lu.crx.financing.entities.constants.InvoiceStatus}
     * based on the status of the financing operation, either a success or failure and persists it in the DB
     * <p>
     * NOTE 2: when calculating the <STRONG>financing rate</STRONG> and Purchaser's <STRONG>interest in cents</STRONG>
     * (used for calculating the early payment value), it rounds both of those individually to Java's Math.round()
     * default behaviour: if result is between .1 to .4 - it rounds DOWN, if result is between .5 to .9 - it rounds UP
     * <p>
     * NOTE 3: Bellow method could've been implemented with fewer loops (streams), but it would've made the code
     * harder to read and more prone to bugs, especially if we want to have the benefit of custom statuses.
     * Therefore, it was a conscious decision to avoid looping less for the sake of little to no performance boost.
     */
    private void financeInvoices(List<Invoice> invoices, List<Purchaser> purchasers) {
        for (Invoice invoice : invoices) {
            log.debug("Processing non-financed invoice: {}", invoice.getId());

            LocalDate financingDate = LocalDate.now();
            long financingTermInDays = ChronoUnit.DAYS.between(financingDate, invoice.getMaturityDate());

            Map<Purchaser, PurchaserFinancingSettings> purchaserToSettings = new HashMap<>();

            for (Purchaser p : purchasers) {
                Optional<PurchaserFinancingSettings> settings =
                        p.getPurchaserFinancingSettings().stream()
                                .filter(config -> config.getCreditor().getId() == invoice.getCreditor().getId())
                                .findFirst();

                settings.ifPresent(purchaserFinancingSettings ->
                        purchaserToSettings.put(p, purchaserFinancingSettings));
            }

            if (purchaserToSettings.isEmpty()) {
                log.info("Financing failed. Invoice ID: {} - no Purchaser config for creditor: {}"
                        , invoice.getId(), invoice.getCreditor());
                invoice.setStatus(MISSING_PURCHASERS);
                invoiceRepository.save(invoice);
                continue;
            }

            Map<Purchaser, PurchaserFinancingSettings> purchaserToSettingsWithValidTerm =
                    purchaserToSettings.entrySet().stream()
                            .filter(e -> financingTermInDays >= e.getKey().getMinimumFinancingTermInDays())
                            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));


            if (purchaserToSettingsWithValidTerm.isEmpty()) {
                log.info("Financing failed. Invoice ID: {} - no Purchaser with equal or smaller financing term: {}"
                        , invoice.getId(), financingTermInDays);
                invoice.setStatus(SHORT_FINANCING_TERM);
                invoiceRepository.save(invoice);
                continue;
            }

            Pair<Purchaser, Integer> purchaserToBestFinancingRate =
                    purchaserToSettingsWithValidTerm.entrySet().stream()
                            .map(e -> Pair.of(e.getKey(), getFinancingRate(e, financingTermInDays)))
                            .min(Comparator.comparing(Pair::getSecond))
                            .orElseThrow();

            if (purchaserToBestFinancingRate.getSecond() > invoice.getCreditor().getMaxFinancingRateInBps()) {
                log.info("Financing failed. Invoice ID: {} - best Purchaser: {}, financing rate: {}, " +
                                "less than max for creditor's: {}",
                        invoice.getId(),
                        purchaserToBestFinancingRate.getFirst(),
                        purchaserToBestFinancingRate.getSecond(),
                        invoice.getCreditor().getMaxFinancingRateInBps());
                invoice.setStatus(FINANCING_RATE_LIMIT_EXCEEDED);
                invoiceRepository.save(invoice);
                continue;
            }

            float bpsAdjustment = purchaserToBestFinancingRate.getSecond() * 0.0001f;
            long purchaserInterestInCents = round(invoice.getValueInCents() * bpsAdjustment);

            invoice.setEarlyPaymentValueInCents(invoice.getValueInCents() - purchaserInterestInCents);
            invoice.setPurchaser(purchaserToBestFinancingRate.getFirst());
            invoice.setFinancingRateInBps(purchaserToBestFinancingRate.getSecond());
            invoice.setFinancingTermInDays(financingTermInDays);
            invoice.setFinancingDate(financingDate);
            invoice.setStatus(FINANCED);

            invoiceRepository.save(invoice);
            log.debug("Invoice successfully financed: {}", invoice);
        }
    }

    private int getFinancingRate(Map.Entry<Purchaser, PurchaserFinancingSettings> e, long financingTermInDays) {
        return round((float) (e.getValue().getAnnualRateInBps() * financingTermInDays) /
                BANKING_YEAR_IN_DAYS);
    }

}
