package lu.crx.financing.services;

import lu.crx.financing.entities.*;
import lu.crx.financing.repository.InvoiceRepository;
import lu.crx.financing.repository.PurchaserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.SliceImpl;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static lu.crx.financing.entities.constants.InvoiceStatus.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FinancingServiceTest {

    private final LocalDate now = LocalDate.now();

    @Mock
    private PurchaserRepository purchaserRepository;
    @Mock
    private InvoiceRepository invoiceRepository;

    @InjectMocks
    private FinancingService financingService;

    @Captor
    private ArgumentCaptor<Invoice> invoiceCaptor;

    private Debtor debtor;
    private Invoice invoice;
    private Creditor creditor;
    private Purchaser purchaser1;
    private Purchaser purchaser2;

    @BeforeEach
    void setUp() {
        setUpValidData();
        ReflectionTestUtils.setField(financingService, "invoicesBatchSize", 10);
    }

    /**
     * This test class could benefit from more test cases with more variation in data, but in order to fit into
     * the time constraint on which I've commited to, I've implemented only the readme scenario (with negatives) here
     * while validating most of the SeedingService examples manually. Hopefully it's enough to get an idea of the
     * testing approach.
     */

    @Test
    void givenReadmeExampleScenario_whenFinance_thenExpectInvoiceWithPurchaser2() {
        financingService.finance();

        verify(invoiceRepository).save(invoiceCaptor.capture());
        Invoice actual = invoiceCaptor.getValue();
        assertThat(actual).usingRecursiveComparison().isEqualTo(
                Invoice.builder()
                        .creditor(creditor)
                        .debtor(debtor)
                        .purchaser(purchaser2)
                        .financingDate(now)
                        .maturityDate(now.plusDays(30))
                        .financingTermInDays(30L)
                        .financingRateInBps(3)
                        .valueInCents(1000000)
                        .earlyPaymentValueInCents(999700L)
                        .status(FINANCED)
                        .build());
    }

    @Test
    void givenValidInvoiceAndNoCreditorConfig_whenFinance_thenExpectMissingPurchasersStatus() {
        purchaser1.setPurchaserFinancingSettings(Set.of());
        purchaser2.setPurchaserFinancingSettings(Set.of(PurchaserFinancingSettings.builder()
                .creditor(Creditor.builder().id(10).name("Unrelated").maxFinancingRateInBps(3).build())
                .annualRateInBps(50)
                .build()));

        financingService.finance();

        verify(invoiceRepository).save(invoiceCaptor.capture());
        Invoice actual = invoiceCaptor.getValue();
        assertThat(actual).usingRecursiveComparison().isEqualTo(
                Invoice.builder()
                        .creditor(creditor)
                        .debtor(debtor)
                        .valueInCents(1000000)
                        .maturityDate(now.plusDays(30))
                        .status(MISSING_PURCHASERS)
                        .build());
    }

    @Test
    void givenValidInvoiceAndPurchaserFinTermExceedsTheMin_whenFinance_thenExpectShortFinancingTermStatus() {
        purchaser1.setMinimumFinancingTermInDays(1000);
        purchaser2.setMinimumFinancingTermInDays(1000);

        financingService.finance();

        verify(invoiceRepository).save(invoiceCaptor.capture());
        Invoice actual = invoiceCaptor.getValue();
        assertThat(actual).usingRecursiveComparison().isEqualTo(
                Invoice.builder()
                        .creditor(creditor)
                        .debtor(debtor)
                        .valueInCents(1000000)
                        .maturityDate(now.plusDays(30))
                        .status(SHORT_FINANCING_TERM)
                        .build());
    }

    @Test
    void givenValidInvoiceAndPurchaserFinRateExceededsMaxRate_whenFinance_thenExpectRateExceededStatus() {
        creditor.setMaxFinancingRateInBps(1);

        financingService.finance();

        verify(invoiceRepository).save(invoiceCaptor.capture());
        Invoice actual = invoiceCaptor.getValue();
        assertThat(actual).usingRecursiveComparison().isEqualTo(
                Invoice.builder()
                        .creditor(creditor)
                        .debtor(debtor)
                        .valueInCents(1000000)
                        .maturityDate(now.plusDays(30))
                        .status(FINANCING_RATE_LIMIT_EXCEEDED)
                        .build());
    }

    private void setUpValidData() {
        debtor = Debtor.builder().name("Joe").build();
        creditor = Creditor.builder().name("Toyota").maxFinancingRateInBps(3).build();

        invoice = Invoice.builder()
                .creditor(creditor)
                .debtor(debtor)
                .valueInCents(1000000)
                .maturityDate(now.plusDays(30))
                .status(NON_FINANCED)
                .build();

        purchaser1 = Purchaser.builder()
                .name("RichBank")
                .minimumFinancingTermInDays(10)
                .purchaserFinancingSetting(PurchaserFinancingSettings.builder()
                        .creditor(creditor)
                        .annualRateInBps(50)
                        .build())
                .build();

        purchaser2 = Purchaser.builder()
                .name("FatBank")
                .minimumFinancingTermInDays(12)
                .purchaserFinancingSetting(PurchaserFinancingSettings.builder()
                        .creditor(creditor)
                        .annualRateInBps(40)
                        .build())
                .build();

        when(invoiceRepository.findAllByStatus(eq(NON_FINANCED), eq(PageRequest.of(0, 10))))
                .thenReturn(new SliceImpl<>(List.of(invoice), mock(PageRequest.class), false));

        when(purchaserRepository.findAll())
                .thenReturn(List.of(purchaser1, purchaser2));
    }

}