/*
 * Filename: AssetType.java
 * Created on: January  2, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.model.enums;

/**
 * ENUM that represents the type of asset for allocation purposes
 * Includes both ticker types and other investment types like bonds
 */
public enum AssetType {
    STOCK,
    FUND,
    CRYPTOCURRENCY,
    REIT,
    ETF,
    BOND
}
