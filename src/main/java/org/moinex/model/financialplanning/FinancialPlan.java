package org.moinex.model.financialplanning;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.*;

@Entity
@Table(name = "financial_plan")
@Builder
@Getter
@Setter
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

    @OneToMany(
            mappedBy = "plan",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.EAGER)
    @Builder.Default
    private List<BudgetGroup> budgetGroups = new ArrayList<>();
}
