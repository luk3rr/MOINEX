/*
 * Filename: Bond.java
 * Created on: January  5, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.entities.investment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
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
@SuperBuilder
public class Bond extends Asset
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    @Setter(AccessLevel.NONE)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private BondType type;

    // TODO: Fix nullable. It should be false when the investment feature is implemented
    @Column(name = "interest_index" )
    private InterestIndex interestIndex;

    @Column(name = "interest_type" )
    private InterestType interestType;

    @Column(name = "interest_rate" )
    private BigDecimal interestRate;

    @Column(name = "maturity_date" )
    private String maturityDate;

    @Builder.Default
    @Column(name             = "archived",
            nullable         = false,
            columnDefinition = "boolean default false")
    private boolean archived = false; // Default value is false
}
