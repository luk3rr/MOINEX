/*
 * Filename: AllocationType.java
 * Created on: January  3, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.util.enums;

/**
 * Tipo de alocação de ativo em uma goal
 */
public enum AllocationType {
    /**
     * Alocação por percentual (ex: 50% do bond)
     */
    PERCENTAGE,
    
    /**
     * Alocação por quantidade (ex: 30 ações)
     */
    QUANTITY,
    
    /**
     * Alocação por valor fixo (ex: R$ 10.000)
     */
    VALUE
}
