package org.moinex.repository.financialplanning

import org.moinex.model.financialplanning.FIRECalculatorSettings
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface FIRECalculatorSettingsRepository : JpaRepository<FIRECalculatorSettings, Int>
