/*
 * Filename: SaleDistributionStrategy.java
 * Created on: January  3, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.util.enums;

/**
 * Estratégia de distribuição de venda entre goals
 */
public enum SaleDistributionStrategy {
    /**
     * Distribuir proporcionalmente entre todas as goals
     */
    PROPORTIONAL,
    
    /**
     * Afetar apenas uma goal específica
     */
    SINGLE_GOAL,
    
    /**
     * Distribuição manual definida pelo usuário
     */
    MANUAL,
    
    /**
     * Não afetar alocações das goals
     */
    KEEP_ALLOCATIONS
}
