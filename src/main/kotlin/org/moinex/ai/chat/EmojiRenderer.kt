/*
 * Filename: EmojiRenderer.kt
 * Created on: September 20, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ai.chat

import java.util.Base64

internal object EmojiRenderer {
    private const val ZERO_WIDTH_JOINER = 0x200D
    private const val VARIATION_SELECTOR_16 = 0xFE0F
    private const val KEYCAP_COMBINER = 0x20E3
    private val SKIN_TONE_MODIFIERS = 0x1F3FB..0x1F3FF
    private val REGIONAL_INDICATORS = 0x1F1E6..0x1F1FF
    private val KEYCAP_BASES: Set<Int> = ('0'.code..'9'.code).toSet() + '#'.code + '*'.code

    private val LETTERLIKE_SYMBOLS = 0x2100..0x214F
    private val ARROWS = 0x2190..0x21FF
    private val MISC_TECHNICAL_AND_DINGBATS = 0x2300..0x27BF
    private val MISC_SYMBOLS_AND_ARROWS = 0x2B00..0x2BFF
    private val MAIN_EMOJI_BLOCK = 0x1F000..0x1FAFF

    private val EMOJI_BASE_RANGES =
        listOf(
            LETTERLIKE_SYMBOLS,
            ARROWS,
            MISC_TECHNICAL_AND_DINGBATS,
            MISC_SYMBOLS_AND_ARROWS,
            MAIN_EMOJI_BLOCK,
        )

    private val dataUriCache = mutableMapOf<String, String?>()

    fun replaceEmojiWithImages(html: String): String {
        val codePoints = html.codePoints().toArray()
        val builder = StringBuilder()
        var i = 0

        while (i < codePoints.size) {
            val range = matchEmojiSequence(codePoints, i)
            if (range == null) {
                builder.appendCodePoint(codePoints[i])
                i++
                continue
            }

            val originalText = buildString { range.forEach { appendCodePoint(codePoints[it]) } }
            val dataUri = filenameCandidates(codePoints, range).firstNotNullOfOrNull { dataUriFor(it) }
            builder.append(if (dataUri != null) imgTag(dataUri, originalText) else originalText)
            i = range.last + 1
        }
        return builder.toString()
    }

    private fun matchEmojiSequence(
        codePoints: IntArray,
        start: Int,
    ): IntRange? {
        val first = codePoints[start]

        if (first in REGIONAL_INDICATORS &&
            start + 1 < codePoints.size &&
            codePoints[start + 1] in REGIONAL_INDICATORS
        ) {
            return start until (start + 2)
        }

        if (first in KEYCAP_BASES) {
            var end = start + 1
            if (end < codePoints.size && codePoints[end] == VARIATION_SELECTOR_16) end++
            return if (end < codePoints.size && codePoints[end] == KEYCAP_COMBINER) start until (end + 1) else null
        }

        if (!isEmojiBase(first)) return null

        var end = start + 1
        var extending = true
        while (extending && end < codePoints.size) {
            val next = codePoints[end]
            extending =
                when {
                    next == VARIATION_SELECTOR_16 || next in SKIN_TONE_MODIFIERS -> {
                        end++
                        true
                    }
                    next == ZERO_WIDTH_JOINER && end + 1 < codePoints.size && isEmojiBase(codePoints[end + 1]) -> {
                        end += 2
                        true
                    }
                    else -> false
                }
        }
        return start until end
    }

    private fun isEmojiBase(codePoint: Int): Boolean = EMOJI_BASE_RANGES.any { codePoint in it }

    private fun filenameCandidates(
        codePoints: IntArray,
        range: IntRange,
    ): List<String> {
        val full = range.joinToString("-") { Integer.toHexString(codePoints[it]) }
        val stripped =
            range
                .filter { codePoints[it] != VARIATION_SELECTOR_16 }
                .joinToString("-") { Integer.toHexString(codePoints[it]) }
        return if (full == stripped) listOf(full) else listOf(full, stripped)
    }

    private fun dataUriFor(codepointFilename: String): String? =
        dataUriCache.getOrPut(codepointFilename) {
            val bytes = javaClass.getResourceAsStream("/emoji/$codepointFilename.svg")?.use { it.readBytes() }
            bytes?.let { "data:image/svg+xml;base64," + Base64.getEncoder().encodeToString(it) }
        }

    private fun imgTag(
        dataUri: String,
        alt: String,
    ): String = """<img src="$dataUri" alt="$alt" style="height:1.2em;width:1.2em;vertical-align:-0.2em;">"""
}
