package org.moinex.repository.financialplanning;

import org.moinex.model.financialplanning.BudgetGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BudgetGroupRepository extends JpaRepository<BudgetGroup, Integer> {
}
