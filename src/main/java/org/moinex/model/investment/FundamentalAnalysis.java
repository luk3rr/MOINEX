/*
 * Filename: FundamentalAnalysis.java
 * Created on: January  9, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.model.investment;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.moinex.model.enums.PeriodType;
import org.moinex.util.Constants;

/**
 * Entity that stores fundamental analysis data for a ticker
 * This data is fetched from the Python script and cached in the database
 * Linked to Ticker entity - analysis is only available for active (non-archived) tickers
 */
@Entity
@Table(name = "fundamental_analysis")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class FundamentalAnalysis {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    @Setter(AccessLevel.NONE)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticker_id", nullable = false, unique = true)
    private Ticker ticker;

    @Column(name = "company_name")
    private String companyName;

    @Column(name = "sector")
    private String sector;

    @Column(name = "industry")
    private String industry;

    @Column(name = "currency", nullable = false)
    private String currency;

    @Column(name = "period_type", nullable = false)
    private PeriodType periodType;

    @Column(name = "data_json", columnDefinition = "TEXT", nullable = false)
    private String dataJson; // Full JSON data from Python script

    @Column(name = "last_update", nullable = false)
    private String lastUpdate;

    @Column(name = "created_at", nullable = false)
    private String createdAt;

    public LocalDateTime getLastUpdate() {
        return LocalDateTime.parse(lastUpdate, Constants.DB_DATE_FORMATTER);
    }

    public void setLastUpdate(LocalDateTime lastUpdate) {
        this.lastUpdate = lastUpdate.format(Constants.DB_DATE_FORMATTER);
    }

    public LocalDateTime getCreatedAt() {
        return LocalDateTime.parse(createdAt, Constants.DB_DATE_FORMATTER);
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt.format(Constants.DB_DATE_FORMATTER);
    }
}
