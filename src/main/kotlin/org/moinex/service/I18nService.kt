/*
 * Filename: I18nService.kt (original filename: I18nService.java)
 * Created on: December 29, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 03/08/2026
 */

package org.moinex.service

import org.springframework.stereotype.Service
import java.util.Locale
import java.util.MissingResourceException
import java.util.ResourceBundle
import java.util.prefs.Preferences

@Service
class I18nService {
    companion object {
        const val BUNDLE_BASE_NAME = "i18n.messages"
        const val PREF_KEY_LOCALE = "ui.locale"
        const val BRAZILIAN_PORTUGUESE_TAG = "pt-BR"
        val SUPPORTED_LOCALES = listOf(Locale.forLanguageTag(BRAZILIAN_PORTUGUESE_TAG), Locale.ENGLISH)
        val preferences: Preferences = Preferences.userRoot().node("org.moinex")
    }

    var locale: Locale = resolveInitialLocale()
        set(value) {
            field = value
            preferences.put(PREF_KEY_LOCALE, toPreferenceValue(value))
            bundleCache = null
        }

    private var bundleCache: ResourceBundle? = null

    fun getSupportedLocales(): List<Locale> = SUPPORTED_LOCALES

    fun getBundle(): ResourceBundle =
        bundleCache ?: try {
            ResourceBundle.getBundle(BUNDLE_BASE_NAME, locale).also { bundleCache = it }
        } catch (_: MissingResourceException) {
            ResourceBundle.getBundle(BUNDLE_BASE_NAME, Locale.ENGLISH).also { bundleCache = it }
        }

    fun tr(key: String?): String =
        key?.let {
            try {
                getBundle().getString(it)
            } catch (_: MissingResourceException) {
                it
            }
        } ?: ""

    private fun resolveInitialLocale(): Locale {
        val stored =
            preferences[PREF_KEY_LOCALE, null]
                ?.takeIf { it.isNotBlank() }
                ?.let { Locale.forLanguageTag(it.replace('_', '-')) }

        if (stored != null) return stored

        val system = Locale.getDefault()
        return system?.let { systemLocale ->
            SUPPORTED_LOCALES.firstOrNull { supported ->
                supported.language.equals(systemLocale.language, ignoreCase = true) &&
                    (
                        supported.country.isEmpty() ||
                            supported.country.equals(systemLocale.country, ignoreCase = true)
                    )
            }
        } ?: Locale.forLanguageTag(BRAZILIAN_PORTUGUESE_TAG)
    }

    private fun toPreferenceValue(locale: Locale): String =
        locale.language +
            locale.country
                .takeIf { it.isNotBlank() }
                ?.let { "_$it" }
                .orEmpty()
}
