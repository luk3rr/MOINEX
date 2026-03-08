/*
 * Filename: AssetType.kt (original filename: AssetType.java)
 * Created on: January  2, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 28/02/2026
 */

package org.moinex.model.enums

enum class AssetType(
    val category: AssetCategory,
) {
    STOCK(AssetCategory.TICKER),
    FUND(AssetCategory.TICKER),
    CRYPTOCURRENCY(AssetCategory.TICKER),
    REIT(AssetCategory.TICKER),
    ETF(AssetCategory.TICKER),
    BOND(AssetCategory.FIXED_INCOME),
}

enum class AssetCategory {
    TICKER,
    FIXED_INCOME,
}
