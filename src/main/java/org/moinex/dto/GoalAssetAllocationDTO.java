/*
 * Filename: GoalAssetAllocationDTO.java
 * Created on: January  3, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.dto;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.moinex.util.enums.AllocationType;
import org.moinex.util.enums.GoalAssetType;

/**
 * DTO para criação/edição de alocação de ativo em goal
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoalAssetAllocationDTO {
    private Integer id;
    private Integer goalId;
    private GoalAssetType assetType;
    private Integer assetId;
    private String assetName;
    private AllocationType allocationType;
    private BigDecimal allocationValue;
    private BigDecimal currentAssetValue;
}
