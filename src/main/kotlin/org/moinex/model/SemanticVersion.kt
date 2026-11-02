/*
 * Filename: SemanticVersion.kt
 * Created on: November 2, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.model

data class SemanticVersion(
    val major: Int,
    val minor: Int,
    val patch: Int,
) : Comparable<SemanticVersion> {
    override fun compareTo(other: SemanticVersion): Int =
        compareValuesBy(this, other, SemanticVersion::major, SemanticVersion::minor, SemanticVersion::patch)

    override fun toString(): String = "$major.$minor.$patch"

    companion object {
        private val PATTERN = Regex("""^v?(\d+)\.(\d+)\.(\d+)$""")

        fun parse(text: String?): SemanticVersion? {
            val match = text?.trim()?.let { PATTERN.matchEntire(it) } ?: return null
            val (major, minor, patch) = match.destructured
            return runCatching { SemanticVersion(major.toInt(), minor.toInt(), patch.toInt()) }.getOrNull()
        }
    }
}
