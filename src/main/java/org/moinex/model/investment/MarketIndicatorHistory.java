/*
 * Filename: MarketIndicatorHistory.java
 * Created on: February 20, 2026
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.moinex.model.enums.InterestIndex;
import org.moinex.util.Constants;

@Entity
@Table(name = "market_indicator_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class MarketIndicatorHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    @Setter(AccessLevel.NONE)
    private Integer id;

    @Enumerated(EnumType.STRING)
    @Column(name = "indicator_type", nullable = false)
    private InterestIndex indicatorType;

    @Column(name = "reference_date", nullable = false)
    private String referenceDate;

    @Column(name = "rate_value", nullable = false)
    private BigDecimal rateValue;

    @Column(name = "created_at", nullable = false)
    private String createdAt;

    public abstract static class MarketIndicatorHistoryBuilder<
            C extends MarketIndicatorHistory, B extends MarketIndicatorHistoryBuilder<C, B>> {
        public B referenceDate(LocalDate referenceDate) {
            this.referenceDate = referenceDate.format(Constants.DB_DATE_FORMATTER);
            return self();
        }

        public B createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt.format(Constants.DB_DATE_FORMATTER);
            return self();
        }
    }

    public LocalDate getReferenceDate() {
        if (referenceDate == null) {
            return null;
        }
        return LocalDate.parse(referenceDate, Constants.DB_DATE_FORMATTER);
    }

    public void setReferenceDate(LocalDate date) {
        if (date == null) {
            this.referenceDate = null;
            return;
        }
        this.referenceDate = date.format(Constants.DB_DATE_FORMATTER);
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
