/*
 * Filename: MarketQuotesAndCommodities.java
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
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "market_quotes_and_commodities")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketQuotesAndCommodities
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    @Setter(AccessLevel.NONE)
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
}
