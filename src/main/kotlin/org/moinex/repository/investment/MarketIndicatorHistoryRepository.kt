/*
 * Filename: MarketIndicatorHistoryRepository.kt (original filename: MarketIndicatorHistoryRepository.java)
 * Created on: February 20, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 07/03/2026
 */

package org.moinex.repository.investment

import org.moinex.model.enums.InterestIndex
import org.moinex.model.investment.MarketIndicatorHistory
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.util.Optional

@Repository
interface MarketIndicatorHistoryRepository : JpaRepository<MarketIndicatorHistory, Int> {
    @Query(
        "SELECT m " +
            "FROM MarketIndicatorHistory m " +
            "WHERE m.indicatorType = :indicatorType " +
            "AND m.referenceDate = :referenceDate",
    )
    fun findByIndicatorTypeAndReferenceDate(
        @Param("indicatorType") indicatorType: InterestIndex,
        @Param("referenceDate") referenceDate: LocalDate,
    ): Optional<MarketIndicatorHistory>

    @Query(
        "SELECT m " +
            "FROM MarketIndicatorHistory m " +
            "WHERE m.indicatorType = :indicatorType " +
            "AND m.referenceDate BETWEEN :startDate AND :endDate " +
            "ORDER BY m.referenceDate ASC",
    )
    fun findByIndicatorTypeAndReferenceDateBetween(
        @Param("indicatorType") indicatorType: InterestIndex,
        @Param("startDate") startDate: LocalDate,
        @Param("endDate") endDate: LocalDate,
    ): List<MarketIndicatorHistory>

    @Query(
        "SELECT CASE WHEN COUNT(m) > 0 THEN true ELSE false END " +
            "FROM MarketIndicatorHistory m " +
            "WHERE m.indicatorType = :indicatorType " +
            "AND m.referenceDate = :referenceDate",
    )
    fun existsByIndicatorTypeAndReferenceDate(
        @Param("indicatorType") indicatorType: InterestIndex,
        @Param("referenceDate") referenceDate: LocalDate,
    ): Boolean

    @Query(
        "SELECT m " +
            "FROM MarketIndicatorHistory m " +
            "WHERE m.indicatorType = :indicatorType " +
            "ORDER BY m.referenceDate DESC LIMIT 1",
    )
    fun findLatestByIndicatorType(
        @Param("indicatorType") indicatorType: InterestIndex,
    ): Optional<MarketIndicatorHistory>

    @Query(
        "SELECT m " +
            "FROM MarketIndicatorHistory m " +
            "WHERE m.indicatorType = :indicatorType " +
            "ORDER BY m.referenceDate ASC LIMIT 1",
    )
    fun findEarliestByIndicatorType(
        @Param("indicatorType") indicatorType: InterestIndex,
    ): Optional<MarketIndicatorHistory>
}
