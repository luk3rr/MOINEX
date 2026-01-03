/*
 * Filename: Bond.java
 * Created on: January  5, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.model.investment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.moinex.util.Constants;
import org.moinex.util.enums.BondType;
import org.moinex.util.enums.InterestIndex;
import org.moinex.util.enums.InterestType;

/**
 * Class that represents a bond
 */
@Entity
@Table(name = "bond")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Bond {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    @Setter(AccessLevel.NONE)
    private Integer id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "symbol")
    private String symbol;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private BondType type;

    @Column(name = "issuer")
    private String issuer;

    @Column(name = "maturity_date")
    private String maturityDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "interest_type")
    private InterestType interestType;

    @Enumerated(EnumType.STRING)
    @Column(name = "interest_index")
    private InterestIndex interestIndex;

    @Column(name = "interest_rate")
    private BigDecimal interestRate;

    @Builder.Default
    @Column(name = "archived", nullable = false)
    private boolean archived = false;

    public abstract static class BondBuilder<C extends Bond, B extends BondBuilder<C, B>> {
        public B maturityDate(LocalDateTime maturityDate) {
            this.maturityDate = maturityDate.format(Constants.DB_DATE_FORMATTER);
            return self();
        }
    }

    public LocalDateTime getMaturityDate() {
        return LocalDateTime.parse(maturityDate, Constants.DB_DATE_FORMATTER);
    }

    public void setMaturityDate(LocalDateTime date) {
        this.maturityDate = date.format(Constants.DB_DATE_FORMATTER);
    }
}
