/*
 * Filename: MarketQuotesAndCommodities.java
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

@Entity
@Table(name = "market_quotes_and_commodities")
public class MarketQuotesAndCommodities
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "dollar")
    private BigDecimal dollar;

    @Column(name = "euro")
    private BigDecimal euro;

    @Column(name = "ibovespa")
    private BigDecimal ibovespa;

    @Column(name = "bitcoin")
    private BigDecimal bitcoin;

    @Column(name = "ethereum")
    private BigDecimal ethereum;

    @Column(name = "gold")
    private BigDecimal gold;

    @Column(name = "soybean")
    private BigDecimal soybean;

    @Column(name = "coffee")
    private BigDecimal coffee;

    @Column(name = "wheat")
    private BigDecimal wheat;

    @Column(name = "oil_brent")
    private BigDecimal oilBrent;

    @Column(name = "last_update")
    private LocalDateTime lastUpdate;

    /**
     * Default constructor for JPA
     */
    public MarketQuotesAndCommodities() { }

    /**
     * Get id
     * @return The id
     */
    public Long GetId()
    {
        return id;
    }

    /**
     * Get the dollar quote
     * @return The dollar quote
     */
    public BigDecimal GetDollar()
    {
        return dollar;
    }

    /**
     * Get the euro quote
     * @return The euro quote
     */
    public BigDecimal GetEuro()
    {
        return euro;
    }

    /**
     * Get the Ibovespa quote
     * @return The Ibovespa quote
     */
    public BigDecimal GetIbovespa()
    {
        return ibovespa;
    }

    /**
     * Get the Bitcoin quote
     * @return The Bitcoin quote
     */
    public BigDecimal GetBitcoin()
    {
        return bitcoin;
    }

    /**
     * Get the Ethereum quote
     * @return The Ethereum quote
     */
    public BigDecimal GetEthereum()
    {
        return ethereum;
    }

    /**
     * Get the gold quote
     * @return The gold quote
     */
    public BigDecimal GetGold()
    {
        return gold;
    }

    /**
     * Get the soybean quote
     * @return The soybean quote
     */
    public BigDecimal GetSoybean()
    {
        return soybean;
    }

    /**
     * Get the coffee quote
     * @return The coffee quote
     */
    public BigDecimal GetCoffee()
    {
        return coffee;
    }

    /**
     * Get the wheat quote
     * @return The wheat quote
     */
    public BigDecimal GetWheat()
    {
        return wheat;
    }

    /**
     * Get the oil Brent quote
     * @return The oil Brent quote
     */
    public BigDecimal GetOilBrent()
    {
        return oilBrent;
    }

    /**
     * Get the last update of the market quotes and commodities
     * @return The last update of the market quotes and commodities
     */
    public LocalDateTime GetLastUpdate()
    {
        return lastUpdate;
    }

    /**
     * Set the dollar quote
     * @param dollar The dollar quote
     */
    public void SetDollar(BigDecimal dollar)
    {
        this.dollar = dollar;
    }

    /**
     * Set the euro quote
     * @param euro The euro quote
     */
    public void SetEuro(BigDecimal euro)
    {
        this.euro = euro;
    }

    /**
     * Set the Ibovespa quote
     * @param ibovespa The Ibovespa quote
     */
    public void SetIbovespa(BigDecimal ibovespa)
    {
        this.ibovespa = ibovespa;
    }

    /**
     * Set the Bitcoin quote
     * @param bitcoin The Bitcoin quote
     */
    public void SetBitcoin(BigDecimal bitcoin)
    {
        this.bitcoin = bitcoin;
    }

    /**
     * Set the Ethereum quote
     * @param ethereum The Ethereum quote
     */
    public void SetEthereum(BigDecimal ethereum)
    {
        this.ethereum = ethereum;
    }

    /**
     * Set the gold quote
     * @param gold The gold quote
     */
    public void SetGold(BigDecimal gold)
    {
        this.gold = gold;
    }

    /**
     * Set the soybean quote
     * @param soybean The soybean quote
     */
    public void SetSoybean(BigDecimal soybean)
    {
        this.soybean = soybean;
    }

    /**
     * Set the coffee quote
     * @param coffee The coffee quote
     */
    public void SetCoffee(BigDecimal coffee)
    {
        this.coffee = coffee;
    }

    /**
     * Set the wheat quote
     * @param wheat The wheat quote
     */
    public void SetWheat(BigDecimal wheat)
    {
        this.wheat = wheat;
    }

    /**
     * Set the oil Brent quote
     * @param oilBrent The oil Brent quote
     */
    public void SetOilBrent(BigDecimal oilBrent)
    {
        this.oilBrent = oilBrent;
    }

    /**
     * Set the last update of the market quotes and commodities
     * @param lastUpdate The last update of the market quotes and commodities
     */
    public void SetLastUpdate(LocalDateTime lastUpdate)
    {
        this.lastUpdate = lastUpdate;
    }
}
