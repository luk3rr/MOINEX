/*
 * Filename: GoalTrackingMode.java
 * Created on: January  3, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.util.enums;

/**
 * Modo de rastreamento de uma goal
 */
public enum GoalTrackingMode {
    /**
     * Goal vinculada a uma carteira mestra (comportamento atual)
     */
    WALLET,
    
    /**
     * Goal vinculada a ativos de investimento (bonds, tickers)
     */
    ASSET_ALLOCATION
}
