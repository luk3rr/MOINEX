/*
 * Filename: PreferencesService.kt (original filename: I18nService.java)
 * Created on: December 29, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 03/08/2026
 * Unified with UserPreferencesService on 03/09/2026
 */

package org.moinex.service

import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.util.Locale
import java.util.MissingResourceException
import java.util.ResourceBundle
import java.util.prefs.Preferences

@Service
class PreferencesService {
    companion object {
        const val BUNDLE_BASE_NAME = "i18n.messages"
        const val PREF_KEY_LOCALE = "ui.locale"
        const val PREF_KEY_HIDE_MONETARY_VALUES = "ui.hideMonetaryValues"
        const val PREF_KEY_THEME = "ui.theme"
        const val PREF_KEY_DB_DIR = "db.directory"
        const val PREF_KEY_SAVINGS_RATE_TARGET = "insights.savingsRateTarget"
        const val PREF_KEY_AI_CHAT_SIDEBAR_COLLAPSED = "ai.chat.sidebarCollapsed"
        const val PREF_KEY_AI_HISTORY_MAX_CHATS = "ai.history.maxChats"
        const val PREF_KEY_AI_HISTORY_MAX_DAYS = "ai.history.maxDays"
        const val PREF_KEY_UPDATE_CHECK_ENABLED = "updates.checkEnabled"
        const val PREF_KEY_LAST_NOTIFIED_VERSION = "updates.lastNotifiedVersion"
        const val DEFAULT_SAVINGS_RATE_TARGET = 20.0
        const val THEME_LIGHT = "light"
        const val THEME_DARK = "dark"
        const val BRAZILIAN_PORTUGUESE_TAG = "pt-BR"
        val SUPPORTED_LOCALES: Map<Locale, String> =
            linkedMapOf(
                Locale.forLanguageTag(BRAZILIAN_PORTUGUESE_TAG) to "Português",
                Locale.ENGLISH to "English",
            )
        val preferences: Preferences = Preferences.userRoot().node("org.moinex")

        /**
         * What a limit field means: blank is no limit, a whole number above zero is that limit, and anything else
         * leaves the [current] limit as it was.
         */
        fun interpretLimit(
            text: String?,
            current: Int?,
        ): Int? {
            val clean = text?.trim().orEmpty()
            return if (clean.isEmpty()) null else clean.toIntOrNull()?.takeIf { it > 0 } ?: current
        }
    }

    var locale: Locale = resolveInitialLocale()
        set(value) {
            field = value
            preferences.put(PREF_KEY_LOCALE, toPreferenceValue(value))
            bundle = resolveBundle()
        }

    var hideMonetaryValues: Boolean = resolveInitialHideMonetaryValues()
        set(value) {
            field = value
            preferences.putBoolean(PREF_KEY_HIDE_MONETARY_VALUES, value)
        }

    var theme: String = preferences[PREF_KEY_THEME, THEME_LIGHT]
        set(value) {
            field = value
            preferences.put(PREF_KEY_THEME, value)
        }

    var aiChatSidebarCollapsed: Boolean
        get() = preferences.getBoolean(PREF_KEY_AI_CHAT_SIDEBAR_COLLAPSED, false)
        set(value) = preferences.putBoolean(PREF_KEY_AI_CHAT_SIDEBAR_COLLAPSED, value)

    /** How many stored assistant conversations to keep; null keeps all of them. */
    var aiHistoryMaxChats: Int?
        get() = readLimit(PREF_KEY_AI_HISTORY_MAX_CHATS)
        set(value) = writeLimit(PREF_KEY_AI_HISTORY_MAX_CHATS, value)

    /** How many days a stored assistant conversation is kept after its last activity; null keeps them forever. */
    var aiHistoryMaxDays: Int?
        get() = readLimit(PREF_KEY_AI_HISTORY_MAX_DAYS)
        set(value) = writeLimit(PREF_KEY_AI_HISTORY_MAX_DAYS, value)

    var dbDirectory: String
        get() = preferences[PREF_KEY_DB_DIR, "${System.getProperty("user.home")}/.moinex/data"]
        set(value) = preferences.put(PREF_KEY_DB_DIR, value)

    var savingsRateTarget: BigDecimal
        get() = BigDecimal.valueOf(preferences.getDouble(PREF_KEY_SAVINGS_RATE_TARGET, DEFAULT_SAVINGS_RATE_TARGET))
        set(value) = preferences.putDouble(PREF_KEY_SAVINGS_RATE_TARGET, value.toDouble())

    fun isDarkMode(): Boolean = theme == THEME_DARK

    fun toggleTheme() {
        theme = if (isDarkMode()) THEME_LIGHT else THEME_DARK
    }

    var bundle: ResourceBundle = resolveBundle()

    fun getSupportedLocales(): List<Locale> = SUPPORTED_LOCALES.keys.toList()

    fun getSupportedLocalesWithLabels(): Map<Locale, String> = SUPPORTED_LOCALES

    fun translate(key: String?): String =
        key?.let {
            try {
                bundle.getString(it)
            } catch (_: MissingResourceException) {
                it
            }
        } ?: ""

    var updateCheckEnabled: Boolean
        get() = preferences.getBoolean(PREF_KEY_UPDATE_CHECK_ENABLED, true)
        set(value) = preferences.putBoolean(PREF_KEY_UPDATE_CHECK_ENABLED, value)

    var lastNotifiedVersion: String?
        get() = preferences[PREF_KEY_LAST_NOTIFIED_VERSION, null]
        set(value) {
            if (value == null) {
                preferences.remove(PREF_KEY_LAST_NOTIFIED_VERSION)
            } else {
                preferences.put(PREF_KEY_LAST_NOTIFIED_VERSION, value)
            }
        }

    fun showMonetaryValues(): Boolean = !hideMonetaryValues

    fun toggleHideMonetaryValues() {
        hideMonetaryValues = !hideMonetaryValues
    }

    private fun readLimit(key: String): Int? = preferences.getInt(key, 0).takeIf { it > 0 }

    private fun writeLimit(
        key: String,
        value: Int?,
    ) {
        if (value != null && value > 0) preferences.putInt(key, value) else preferences.remove(key)
    }

    private fun resolveInitialLocale(): Locale {
        val stored =
            preferences[PREF_KEY_LOCALE, null]
                ?.takeIf { it.isNotBlank() }
                ?.let { Locale.forLanguageTag(it.replace('_', '-')) }

        if (stored != null) return stored

        val system = Locale.getDefault()
        return system?.let { systemLocale ->
            SUPPORTED_LOCALES.keys.firstOrNull { supported ->
                supported.language.equals(systemLocale.language, ignoreCase = true) &&
                    (
                        supported.country.isEmpty() ||
                            supported.country.equals(systemLocale.country, ignoreCase = true)
                    )
            }
        } ?: Locale.forLanguageTag(BRAZILIAN_PORTUGUESE_TAG)
    }

    private fun resolveInitialHideMonetaryValues(): Boolean =
        preferences.getBoolean(PREF_KEY_HIDE_MONETARY_VALUES, false)

    private fun resolveBundle(): ResourceBundle =
        runCatching {
            ResourceBundle.getBundle(BUNDLE_BASE_NAME, locale)
        }.getOrElse {
            ResourceBundle.getBundle(BUNDLE_BASE_NAME, Locale.ENGLISH)
        }

    private fun toPreferenceValue(locale: Locale): String =
        locale.language +
            locale.country
                .takeIf { it.isNotBlank() }
                ?.let { "_$it" }
                .orEmpty()
}
