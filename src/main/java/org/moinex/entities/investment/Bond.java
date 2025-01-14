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

import org.moinex.util.BondType;
import org.moinex.util.InterestIndex;
import org.moinex.util.InterestType;

/**
 * Class that represents a bond
 */
@Entity
@Table(name = "bond")
public class Bond extends Asset
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private BondType type;

    @Column(name = "interest_index", nullable = false)
    private InterestIndex interestIndex;

    @Column(name = "interest_type", nullable = false)
    private InterestType interestType;

    @Column(name = "interest_rate", nullable = false)
    private BigDecimal interestRate;

    @Column(name = "maturity_date", nullable = true)
    private String maturityDate;

    @Column(name             = "archived",
            nullable         = false,
            columnDefinition = "boolean default false")
    private Boolean archived = false; // Default value is false

    /**
     * Default constructor for JPA
     */
    public Bond() { }

    /**
     * Constructor for Bond
     * @param name The name of the bond
     * @param symbol The symbol of the bond
     * @param currentQuantity The current quantity of the bond
     * @param currentUnitValue The current unit value of the bond
     * @param maturityDate The maturity date of the bond
     */
    public Bond(String     name,
                String     symbol,
                BigDecimal currentQuantity,
                BigDecimal currentUnitValue,
                BigDecimal averageUnitValue,
                String     maturityDate)
    {
        super(name, symbol, currentQuantity, currentUnitValue, averageUnitValue, BigDecimal.ONE);
        this.maturityDate = maturityDate;
    }

    public Long GetId()
    {
        return id;
    }

    public String GetMaturityDate()
    {
        return maturityDate;
    }

    public void SetMaturityDate(String maturityDate)
    {
        this.maturityDate = maturityDate;
    }
}
