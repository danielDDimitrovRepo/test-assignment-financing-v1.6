package lu.crx.financing.entities;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * Purchaser is an entity (usually a bank) that wants to purchase the invoices. I.e. it issues a loan
 * to the creditor for the term and the value of the invoice, according to the rate set up by this purchaser.
 */
@Entity
@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = {"name"})
public class Purchaser implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    @Column(nullable = false, unique = true)
    private String name;

    /**
     * The minimum financing term (duration between the financing date and the maturity date of the invoice).
     */
    @Basic(optional = false)
    private int minimumFinancingTermInDays;

    /**
     * The per-creditor settings for financing.
     */
    @Singular
    @OneToMany(cascade = CascadeType.PERSIST)
    private Set<PurchaserFinancingSettings> purchaserFinancingSettings = new HashSet<>();

}
