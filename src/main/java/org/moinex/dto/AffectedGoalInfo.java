/*
 * Filename: AffectedGoalInfo.java
 * Created on: January  3, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.dto;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO com informações sobre goals afetadas por uma venda de ativo
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AffectedGoalInfo {
    private Integer goalId;
    private String goalName;
    private BigDecimal currentAllocation;
    private BigDecimal proportion;
    private BigDecimal suggestedReduction;
}
