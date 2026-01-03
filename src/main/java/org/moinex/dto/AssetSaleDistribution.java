/*
 * Filename: AssetSaleDistribution.java
 * Created on: January  3, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.dto;

import java.math.BigDecimal;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.moinex.util.enums.SaleDistributionStrategy;

/**
 * DTO para capturar a estratégia de distribuição de venda entre goals
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetSaleDistribution {
    private SaleDistributionStrategy strategy;
    private Integer singleGoalId;
    private Map<Integer, BigDecimal> manualDistribution;
}
