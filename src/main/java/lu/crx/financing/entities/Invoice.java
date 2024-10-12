package lu.crx.financing.entities;

import jakarta.persistence.*;
import lombok.*;
import lu.crx.financing.entities.constants.InvoiceStatus;

import java.io.Serializable;
import java.time.LocalDate;

import static lu.crx.financing.entities.constants.InvoiceStatus.NON_FINANCED;

/**
 * An invoice issued by the {@link Creditor} to the {@link Debtor} for shipped goods.
 */
@Entity
@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(indexes = @Index(name = "is_index", columnList = "status"))
public class Invoice implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    /**
     * Creditor is the entity that issued the invoice.
     */
    @ManyToOne(optional = false)
    private Creditor creditor;

    /**
     * Debtor is the entity obliged to pay according to the invoice.
     */
    @ManyToOne
    private Debtor debtor;

    /**
     * Purchaser is the entity financing the invoice.
     */
    @ManyToOne
    private Purchaser purchaser;

    /**
     * Financing date, on which the Purchaser pays the Creditor
     */
    private LocalDate financingDate;

    /**
     * Maturity date is the date on which the {@link #debtor} is to pay for the invoice.
     * In case the invoice was financed, the money will be paid in full on this date to the purchaser of the invoice.
     */
    @Basic(optional = false)
    private LocalDate maturityDate;

    /**
     * Number of days between the financing date (Purchaser -> Creditor) and
     * the invoice's maturity date (Debtor -> Purchaser).
     */
    @Column(name = "financing_term_days")
    private Long financingTermInDays;

    /**
     * Financing rate for the Invoice, proportional to its financing term.
     */
    @Column(name = "financing_rate_bps")
    private Integer financingRateInBps;

    /**
     * The amount to be paid for the shipment by the Debtor.
     */
    @Column(name = "value_cents", nullable = false)
    private long valueInCents;

    /**
     * The amount to be paid by the Purchaser to the Creditor. This value should be less than the amount to be paid
     * by the Debtor and the difference represents the interest for the Purchaser.
     */
    @Column(name = "early_value_cents")
    private Long earlyPaymentValueInCents;

    /**
     * Invoice status based on the state of the financing
     */
    @Basic(optional = false)
    @Enumerated(EnumType.STRING)
    private InvoiceStatus status;

    @PrePersist
    public void prePersist() {
        if (status == null) {
            status = NON_FINANCED;
        }
    }

}
