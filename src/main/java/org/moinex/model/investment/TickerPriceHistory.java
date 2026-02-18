/*
 * Filename: TickerPriceHistory.java
 * Created on: February 17, 2026
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
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.moinex.util.Constants;

/**
 * Entity that stores historical price data for tickers
 * Used to calculate capital appreciation/depreciation over time
 */
@Entity
@Table(name = "ticker_price_history")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class TickerPriceHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    @Setter(AccessLevel.NONE)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "ticker_id", nullable = false)
    private Ticker ticker;

    @Column(name = "price_date", nullable = false)
    private String priceDate;

    @Column(name = "closing_price", nullable = false)
    private BigDecimal closingPrice;

    @Column(name = "is_month_end", nullable = false)
    @Builder.Default
    private boolean isMonthEnd = false;

    public LocalDate getPriceDate() {
        return LocalDate.parse(priceDate, Constants.DATE_FORMATTER_NO_TIME);
    }

    public void setPriceDate(LocalDate priceDate) {
        this.priceDate = priceDate.format(Constants.DATE_FORMATTER_NO_TIME);
    }
}
