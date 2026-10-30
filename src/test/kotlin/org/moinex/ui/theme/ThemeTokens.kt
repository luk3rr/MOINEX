package org.moinex.ui.theme

import kotlin.math.pow

/**
 * Reads the color tokens of the real theme stylesheets so tests can check them without JavaFX.
 */
object ThemeTokens {
    private val tokenLine = Regex("""^\s*(theme-[a-z0-9-]+)\s*:\s*([^;]+);""", RegexOption.MULTILINE)

    fun load(theme: String): Map<String, String> {
        val css =
            ThemeTokens::class.java
                .getResource("/css/theme/tokens.$theme.css")!!
                .readText()
        return tokenLine
            .findAll(css)
            .associate { it.groupValues[1] to it.groupValues[2].trim() }
    }

    fun color(
        tokens: Map<String, String>,
        name: String,
    ): Int {
        var value = tokens[name] ?: error("token $name is not defined")
        var hops = 0
        while (value.startsWith("theme-")) {
            value = tokens[value] ?: error("token $name points to the undefined token $value")
            check(++hops < MAX_ALIAS_HOPS) { "token $name has an alias loop" }
        }
        return parse(value)
    }

    fun contrast(
        foreground: Int,
        background: Int,
    ): Double {
        val a = luminance(foreground)
        val b = luminance(background)
        return (maxOf(a, b) + 0.05) / (minOf(a, b) + 0.05)
    }

    private fun parse(value: String): Int {
        if (value.equals("white", ignoreCase = true)) return 0xFFFFFF
        val hex = value.removePrefix("#")
        val full = if (hex.length == 3) hex.map { "$it$it" }.joinToString("") else hex
        require(full.length == 6) { "unsupported color value '$value'" }
        return full.toInt(HEX_RADIX)
    }

    private fun luminance(rgb: Int): Double {
        fun channel(shift: Int): Double {
            val c = ((rgb shr shift) and 0xFF) / MAX_CHANNEL
            return if (c <= LINEAR_THRESHOLD) c / LINEAR_DIVISOR else ((c + GAMMA_OFFSET) / GAMMA_DIVISOR).pow(GAMMA)
        }
        return RED_WEIGHT * channel(RED_SHIFT) + GREEN_WEIGHT * channel(GREEN_SHIFT) + BLUE_WEIGHT * channel(0)
    }

    private const val HEX_RADIX = 16
    private const val MAX_ALIAS_HOPS = 8
    private const val MAX_CHANNEL = 255.0
    private const val LINEAR_THRESHOLD = 0.03928
    private const val LINEAR_DIVISOR = 12.92
    private const val GAMMA_OFFSET = 0.055
    private const val GAMMA_DIVISOR = 1.055
    private const val GAMMA = 2.4
    private const val RED_WEIGHT = 0.2126
    private const val GREEN_WEIGHT = 0.7152
    private const val BLUE_WEIGHT = 0.0722
    private const val RED_SHIFT = 16
    private const val GREEN_SHIFT = 8
}
