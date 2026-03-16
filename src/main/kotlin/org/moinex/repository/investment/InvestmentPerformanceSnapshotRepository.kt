package org.moinex.repository.investment

import org.moinex.model.investment.InvestmentPerformanceSnapshot
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.YearMonth

@Repository
interface InvestmentPerformanceSnapshotRepository : JpaRepository<InvestmentPerformanceSnapshot, Int> {
    fun findByReferenceMonth(referenceMonth: YearMonth): InvestmentPerformanceSnapshot?

    @Query(
        "SELECT ips " +
            "FROM InvestmentPerformanceSnapshot ips " +
            "ORDER BY ips.referenceMonth ASC",
    )
    fun findAllOrderedByDate(): List<InvestmentPerformanceSnapshot>

    @Modifying
    @Transactional
    @Query("DELETE FROM InvestmentPerformanceSnapshot")
    override fun deleteAll()

    fun existsByReferenceMonth(referenceMonth: YearMonth): Boolean
}
