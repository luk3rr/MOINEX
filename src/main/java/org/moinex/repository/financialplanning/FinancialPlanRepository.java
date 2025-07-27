package org.moinex.repository.financialplanning;

import org.moinex.model.financialplanning.FinancialPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FinancialPlanRepository extends JpaRepository<FinancialPlan, Integer> {
    boolean existsByName(String name);
}
