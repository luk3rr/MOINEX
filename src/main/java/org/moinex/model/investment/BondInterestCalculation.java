/*
 * Filename: BondInterestCalculation.java
 * Created on: February 20, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.model.investment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.moinex.util.Constants;

@Entity
@Table(name = "bond_interest_calculation")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class BondInterestCalculation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    @Setter(AccessLevel.NONE)
    private Integer id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "bond_id", referencedColumnName = "id", nullable = false)
    private Bond bond;

    @Column(name = "calculation_date", nullable = false)
    private String calculationDate;

    @Column(name = "start_date", nullable = false)
    private String startDate;

    @Column(name = "end_date", nullable = false)
    private String endDate;

    @Column(name = "quantity", nullable = false)
    private BigDecimal quantity;

    @Column(name = "invested_amount", nullable = false)
    private BigDecimal investedAmount;

    @Column(name = "accumulated_interest", nullable = false)
    private BigDecimal accumulatedInterest;

    @Column(name = "final_value", nullable = false)
    private BigDecimal finalValue;

    @Column(name = "calculation_method")
    private String calculationMethod;

    @Column(name = "created_at", nullable = false)
    private String createdAt;

    public abstract static class BondInterestCalculationBuilder<
            C extends BondInterestCalculation, B extends BondInterestCalculationBuilder<C, B>> {
        public B calculationDate(LocalDate calculationDate) {
            this.calculationDate = calculationDate.format(Constants.DB_DATE_FORMATTER);
            return self();
        }

        public B startDate(LocalDate startDate) {
            this.startDate = startDate.format(Constants.DB_DATE_FORMATTER);
            return self();
        }

        public B endDate(LocalDate endDate) {
            this.endDate = endDate.format(Constants.DB_DATE_FORMATTER);
            return self();
        }

        public B createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt.format(Constants.DB_DATE_FORMATTER);
            return self();
        }
    }

    public LocalDate getCalculationDate() {
        if (calculationDate == null) {
            return null;
        }
        return LocalDate.parse(calculationDate, Constants.DB_DATE_FORMATTER);
    }

    public void setCalculationDate(LocalDate date) {
        if (date == null) {
            this.calculationDate = null;
            return;
        }
        this.calculationDate = date.format(Constants.DB_DATE_FORMATTER);
    }

    public LocalDate getStartDate() {
        if (startDate == null) {
            return null;
        }
        return LocalDate.parse(startDate, Constants.DB_DATE_FORMATTER);
    }

    public void setStartDate(LocalDate date) {
        if (date == null) {
            this.startDate = null;
            return;
        }
        this.startDate = date.format(Constants.DB_DATE_FORMATTER);
    }

    public LocalDate getEndDate() {
        if (endDate == null) {
            return null;
        }
        return LocalDate.parse(endDate, Constants.DB_DATE_FORMATTER);
    }

    public void setEndDate(LocalDate date) {
        if (date == null) {
            this.endDate = null;
            return;
        }
        this.endDate = date.format(Constants.DB_DATE_FORMATTER);
    }

    public LocalDateTime getCreatedAt() {
        if (createdAt == null) {
            return null;
        }
        return LocalDateTime.parse(createdAt, Constants.DB_DATE_FORMATTER);
    }

    public void setCreatedAt(LocalDateTime dateTime) {
        if (dateTime == null) {
            this.createdAt = null;
            return;
        }
        this.createdAt = dateTime.format(Constants.DB_DATE_FORMATTER);
    }
}
