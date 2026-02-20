/*
 * Filename: MarketIndicatorHistoryRepository.java
 * Created on: February 20, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.repository.investment;

import java.util.List;
import java.util.Optional;
import org.moinex.model.enums.InterestIndex;
import org.moinex.model.investment.MarketIndicatorHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface MarketIndicatorHistoryRepository
        extends JpaRepository<MarketIndicatorHistory, Integer> {

    @Query(
            "SELECT m FROM MarketIndicatorHistory m WHERE m.indicatorType = :indicatorType"
                    + " AND m.referenceDate = :referenceDate")
    Optional<MarketIndicatorHistory> findByIndicatorTypeAndReferenceDate(
            @Param("indicatorType") InterestIndex indicatorType,
            @Param("referenceDate") String referenceDate);

    @Query(
            "SELECT m FROM MarketIndicatorHistory m WHERE m.indicatorType = :indicatorType AND"
                + " m.referenceDate BETWEEN :startDate AND :endDate ORDER BY m.referenceDate ASC")
    List<MarketIndicatorHistory> findByIndicatorTypeAndReferenceDateBetween(
            @Param("indicatorType") InterestIndex indicatorType,
            @Param("startDate") String startDate,
            @Param("endDate") String endDate);

    @Query(
            "SELECT CASE WHEN COUNT(m) > 0 THEN true ELSE false END FROM MarketIndicatorHistory m"
                + " WHERE m.indicatorType = :indicatorType AND m.referenceDate = :referenceDate")
    boolean existsByIndicatorTypeAndReferenceDate(
            @Param("indicatorType") InterestIndex indicatorType,
            @Param("referenceDate") String referenceDate);

    @Query(
            "SELECT m FROM MarketIndicatorHistory m WHERE m.indicatorType = :indicatorType"
                    + " ORDER BY m.referenceDate DESC LIMIT 1")
    Optional<MarketIndicatorHistory> findLatestByIndicatorType(
            @Param("indicatorType") InterestIndex indicatorType);

    @Query(
            "SELECT m FROM MarketIndicatorHistory m WHERE m.indicatorType = :indicatorType"
                    + " ORDER BY m.referenceDate ASC LIMIT 1")
    Optional<MarketIndicatorHistory> findEarliestByIndicatorType(
            @Param("indicatorType") InterestIndex indicatorType);
}
