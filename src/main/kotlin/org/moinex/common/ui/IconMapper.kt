/*
 * Filename: IconMapper.kt
 * Created on: October 29, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.common.ui

/**
 * Maps the icon file names stored in the database to Ikonli literals.
 */
object IconMapper {
    private val walletTypeLiterals: Map<String, String> =
        mapOf(
            "broker.png" to Icons.WALLET_TYPE_BROKER,
            "checking.png" to Icons.WALLET_TYPE_CHECKING,
            "cryptocurrency.png" to Icons.WALLET_TYPE_CRYPTOCURRENCY,
            "food_voucher.png" to Icons.WALLET_TYPE_FOOD_VOUCHER,
            "goal.png" to Icons.WALLET_TYPE_GOAL,
            "meal_voucher.png" to Icons.WALLET_TYPE_MEAL_VOUCHER,
            "others.png" to Icons.WALLET_TYPE_OTHERS,
            "savings.png" to Icons.WALLET_TYPE_SAVINGS,
            "wallet.png" to Icons.WALLET_TYPE_WALLET,
        )

    /**
     * Literal for the icon file name of a wallet type (for example `checking.png`); unknown names get the default icon.
     */
    fun walletTypeLiteral(fileName: String?): String = walletTypeLiterals[fileName] ?: Icons.DEFAULT

    fun isKnownWalletTypeIcon(fileName: String?): Boolean = fileName != null && fileName in walletTypeLiterals
}
