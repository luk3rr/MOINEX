package org.moinex.model.financialplanning;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "financial_plan")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinancialPlan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(name = "base_income", nullable = false)
    private BigDecimal baseIncome;
}
