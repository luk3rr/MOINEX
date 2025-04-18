/*
 * Filename: BrazilianMarketIndicators.java
 * Created on: January 17, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.model.investment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.moinex.util.Constants;

@Entity
@Table(name = "brazilian_market_indicators")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class BrazilianMarketIndicators
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    @Setter(AccessLevel.NONE)
    private Long id;

    @Column(name = "selic_target")
    private BigDecimal selicTarget;

    @Column(name = "ipca_last_month_rate")
    private BigDecimal ipcaLastMonth;

    @Column(name = "ipca_last_month_reference")
    private String ipcaLastMonthReference;

    @Column(name = "ipca_12_months")
    private BigDecimal ipca12Months;

    @Column(name = "last_update")
    private String lastUpdate;

    public abstract static class BrazilianMarketIndicatorsBuilder<
        C extends   BrazilianMarketIndicators, B
            extends BrazilianMarketIndicatorsBuilder<C, B>>
    {
        public B ipcaLastMonthReference(YearMonth ipcaLastMonthReference)
        {
            this.ipcaLastMonthReference =
                ipcaLastMonthReference.format(Constants.DB_MONTH_YEAR_FORMATTER);
            return self();
        }

        public B lastUpdate(LocalDateTime lastUpdate)
        {
            this.lastUpdate = lastUpdate.format(Constants.DB_DATE_FORMATTER);
            return self();
        }
    }

    /**
     * Get the reference of the IPCA of the last month
     * @return The reference of the IPCA of the last month
     */
    public YearMonth getIpcaLastMonthReference()
    {
        if (ipcaLastMonthReference == null)
        {
            return null;
        }

        return YearMonth.parse(ipcaLastMonthReference,
                               Constants.DB_MONTH_YEAR_FORMATTER);
    }

    /**
     * Get the last update of the Brazilian market indicators
     * @return The last update of the Brazilian market indicators
     */
    public LocalDateTime getLastUpdate()
    {
        if (lastUpdate == null)
        {
            return null;
        }

        return LocalDateTime.parse(lastUpdate, Constants.DB_DATE_FORMATTER);
    }

    /**
     * Set the reference of the IPCA of the last month
     * @param ipcaLastMonthReference The reference of the IPCA of the last month
     */
    public void setIpcaLastMonthReference(YearMonth ipcaLastMonthReference)
    {
        if (ipcaLastMonthReference == null)
        {
            this.ipcaLastMonthReference = null;
            return;
        }

        this.ipcaLastMonthReference =
            ipcaLastMonthReference.format(Constants.DB_MONTH_YEAR_FORMATTER);
    }

    /**
     * Set the last update of the Brazilian market indicators
     * @param lastUpdate The last update of the Brazilian market indicators
     */
    public void setLastUpdate(LocalDateTime lastUpdate)
    {
        if (lastUpdate == null)
        {
            this.lastUpdate = null;
            return;
        }

        this.lastUpdate = lastUpdate.format(Constants.DB_DATE_FORMATTER);
    }
}
