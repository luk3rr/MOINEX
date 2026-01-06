/*
 * Filename: InterestType.java
 * Created on: January 14, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.model.enums;

/**
 * ENUM that represents the type of interest of a bond
 */
public enum InterestType {
    FIXED, // Juros prefixados
    FLOATING, // Juros pós-fixados
    ZERO_COUPON // Sem pagamentos de juros recorrentes
}
