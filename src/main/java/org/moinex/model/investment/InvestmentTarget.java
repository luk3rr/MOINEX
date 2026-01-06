/*
 * Filename: InvestmentTarget.java
 * Created on: January  2, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.model.investment;

import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.*;
import org.moinex.model.enums.AssetType;

@Entity
@Table(name = "investment_target")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvestmentTarget {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    @Setter(AccessLevel.NONE)
    private Integer id;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, unique = true)
    private AssetType assetType;

    @Column(name = "target_percentage", nullable = false)
    private BigDecimal targetPercentage;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;
}
