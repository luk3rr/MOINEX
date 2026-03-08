/*
 * Filename: MarketQuotesAndCommodities.kt (original filename: MarketQuotesAndCommodities.java)
 * Created on: January 17, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 07/03/2026
 */

package org.moinex.model.investment

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.moinex.common.extension.toRounded
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(name = "market_quotes_and_commodities")
class MarketQuotesAndCommodities(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Int? = null,
    @Column(name = "dollar")
    var dollar: BigDecimal? = null,
    @Column(name = "euro")
    var euro: BigDecimal? = null,
    @Column(name = "ibovespa")
    var ibovespa: BigDecimal? = null,
    @Column(name = "bitcoin")
    var bitcoin: BigDecimal? = null,
    @Column(name = "ethereum")
    var ethereum: BigDecimal? = null,
    @Column(name = "gold")
    var gold: BigDecimal? = null,
    @Column(name = "soybean")
    var soybean: BigDecimal? = null,
    @Column(name = "coffee")
    var coffee: BigDecimal? = null,
    @Column(name = "wheat")
    var wheat: BigDecimal? = null,
    @Column(name = "oil_brent")
    var oilBrent: BigDecimal? = null,
    @Column(name = "last_update")
    var lastUpdate: LocalDateTime? = null,
) {
    init {
        dollar = dollar?.toRounded()
        euro = euro?.toRounded()
        ibovespa = ibovespa?.toRounded()
        bitcoin = bitcoin?.toRounded()
        ethereum = ethereum?.toRounded()
        gold = gold?.toRounded()
        soybean = soybean?.toRounded()
        coffee = coffee?.toRounded()
        wheat = wheat?.toRounded()
        oilBrent = oilBrent?.toRounded()
    }

    override fun toString(): String = "MarketQuotesAndCommodities [id=$id, lastUpdate=$lastUpdate]"
}
