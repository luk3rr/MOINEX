/*
 * Filename: GoalAssetAllocation.java
 * Created on: January  3, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.model.goal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.moinex.util.Constants;
import org.moinex.util.enums.AllocationType;
import org.moinex.util.enums.GoalAssetType;

/**
 * Representa a alocação de um ativo de investimento em uma goal
 */
@Entity
@Table(name = "goal_asset_allocation")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoalAssetAllocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    @Setter(AccessLevel.NONE)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "goal_id", nullable = false)
    private Goal goal;

    @Enumerated(EnumType.STRING)
    @Column(name = "asset_type", nullable = false, length = 20)
    private GoalAssetType assetType;

    @Column(name = "asset_id", nullable = false)
    private Integer assetId;

    @Enumerated(EnumType.STRING)
    @Column(name = "allocation_type", nullable = false, length = 20)
    private AllocationType allocationType;

    @Column(name = "allocation_value", nullable = false, precision = 20, scale = 8)
    private BigDecimal allocationValue;

    @Column(name = "created_at", nullable = false)
    private String createdAt;

    @Column(name = "updated_at", nullable = false)
    private String updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now.format(Constants.DB_DATE_FORMATTER);
        this.updatedAt = now.format(Constants.DB_DATE_FORMATTER);
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now().format(Constants.DB_DATE_FORMATTER);
    }

    public LocalDateTime getCreatedAtAsDateTime() {
        return LocalDateTime.parse(createdAt, Constants.DB_DATE_FORMATTER);
    }

    public LocalDateTime getUpdatedAtAsDateTime() {
        return LocalDateTime.parse(updatedAt, Constants.DB_DATE_FORMATTER);
    }
}
