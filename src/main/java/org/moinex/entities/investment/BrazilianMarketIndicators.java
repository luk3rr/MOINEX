/*
 * Filename: BrazilianMarketIndicators.java
 * Created on: January 17, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.entities.investment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;

import org.moinex.util.Constants;

@Entity
@Table(name = "brazilian_market_indicators")
public class BrazilianMarketIndicators
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
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

    /**
     * Default constructor for JPA
     */
    public BrazilianMarketIndicators() { }

    /**
     * Get the SELIC target
     * @return The SELIC target
     */
    public BigDecimal GetSelicTarget()
    {
        return selicTarget;
    }

    /**
     * Get the IPCA of the last month
     * @return The IPCA of the last month
     */
    public BigDecimal GetIpcaLastMonth()
    {
        return ipcaLastMonth;
    }

    /**
     * Get the reference of the IPCA of the last month
     * @return The reference of the IPCA of the last month
     */
    public YearMonth GetIpcaLastMonthReference()
    {
        if (ipcaLastMonthReference == null)
        {
            return null;
        }

        return YearMonth.parse(ipcaLastMonthReference, Constants.DB_MONTH_YEAR_FORMATTER);
    }

    /**
     * Get the IPCA of the last 12 months
     * @return The IPCA of the last 12 months
     */
    public BigDecimal GetIpca12Months()
    {
        return ipca12Months;
    }

    /**
     * Get the last update of the Brazilian market indicators
     * @return The last update of the Brazilian market indicators
     */
    public LocalDateTime GetLastUpdate()
    {
        if (lastUpdate == null)
        {
            return null;
        }

        return LocalDateTime.parse(lastUpdate, Constants.DB_DATE_FORMATTER);
    }

    /**
     * Set the SELIC target
     * @param selicTarget The SELIC target
     */
    public void SetSelicTarget(BigDecimal selicTarget)
    {
        this.selicTarget = selicTarget;
    }

    /**
     * Set the IPCA of the last month
     * @param ipcaLastMonth The IPCA of the last month
     */
    public void SetIpcaLastMonth(BigDecimal ipcaLastMonth)
    {
        this.ipcaLastMonth = ipcaLastMonth;
    }

    /**
     * Set the reference of the IPCA of the last month
     * @param ipcaLastMonthReference The reference of the IPCA of the last month
     */
    public void SetIpcaLastMonthReference(YearMonth ipcaLastMonthReference)
    {
        if (ipcaLastMonthReference == null)
        {
            this.ipcaLastMonthReference = null;
            return;
        }

        this.ipcaLastMonthReference = ipcaLastMonthReference.format(Constants.DB_MONTH_YEAR_FORMATTER);
    }

    /**
     * Set the IPCA of the last 12 months
     * @param ipca12Months The IPCA of the last 12 months
     */
    public void SetIpca12Months(BigDecimal ipca12Months)
    {
        this.ipca12Months = ipca12Months;
    }

    /**
     * Set the last update of the Brazilian market indicators
     * @param lastUpdate The last update of the Brazilian market indicators
     */
    public void SetLastUpdate(LocalDateTime lastUpdate)
    {
        if (lastUpdate == null)
        {
            this.lastUpdate = null;
            return;
        }

        this.lastUpdate = lastUpdate.format(Constants.DB_DATE_FORMATTER);
    }
}
