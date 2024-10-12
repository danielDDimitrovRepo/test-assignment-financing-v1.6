package lu.crx.financing.entities;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;

/**
 * A debtor is an entity that purchased some goods from the {@link Creditor}, received an {@link Invoice}
 * and is obliged to pay for the invoice at the specified date called maturity date
 * (see {@link Invoice#getMaturityDate()}).
 */
@Entity
@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Debtor implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    @Column(nullable = false, unique = true)
    private String name;

}
