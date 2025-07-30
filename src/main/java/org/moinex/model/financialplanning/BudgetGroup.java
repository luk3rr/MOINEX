package org.moinex.model.financialplanning;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;
import lombok.*;
import org.moinex.model.Category;

@Entity
@Table(name = "budget_group")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BudgetGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(name = "target_percentage", nullable = false)
    private BigDecimal targetPercentage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private FinancialPlan plan;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "budget_group_categories",
            joinColumns = @JoinColumn(name = "budget_group_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id"))
    @Builder.Default
    private Set<Category> categories = new HashSet<>();
}
