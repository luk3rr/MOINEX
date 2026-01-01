package org.moinex.service;

import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;
import org.springframework.stereotype.Service;

@Service
public class I18nService {
    private static final String BUNDLE_BASE_NAME = "i18n.messages";
    private static final String PREF_KEY_LOCALE = "ui.locale";

    private final Preferences preferences = Preferences.userRoot().node("org.moinex");

    private Locale locale;

    public I18nService() {
        this.locale = resolveInitialLocale();
    }

    public Locale getLocale() {
        return locale;
    }

    public void setLocale(Locale locale) {
        if (locale == null) {
            return;
        }

        this.locale = locale;
        preferences.put(PREF_KEY_LOCALE, toPreferenceValue(locale));
    }

    public List<Locale> getSupportedLocales() {
        return List.of(Locale.forLanguageTag("pt-BR"), Locale.ENGLISH);
    }

    public ResourceBundle getBundle() {
        try {
            return ResourceBundle.getBundle(BUNDLE_BASE_NAME, locale);
        } catch (MissingResourceException ex) {
            return ResourceBundle.getBundle(BUNDLE_BASE_NAME, Locale.ENGLISH);
        }
    }

    public String tr(String key) {
        if (key == null) {
            return "";
        }

        try {
            return getBundle().getString(key);
        } catch (MissingResourceException ex) {
            return key;
        }
    }

    private Locale resolveInitialLocale() {
        String stored = preferences.get(PREF_KEY_LOCALE, null);
        if (stored != null && !stored.isBlank()) {
            return Locale.forLanguageTag(stored.replace('_', '-'));
        }

        Locale system = Locale.getDefault();
        if (system != null) {
            for (Locale supported : getSupportedLocales()) {
                if (supported.getLanguage().equalsIgnoreCase(system.getLanguage())) {
                    if (supported.getCountry().isEmpty()
                            || supported.getCountry().equalsIgnoreCase(system.getCountry())) {
                        return supported;
                    }
                }
            }
        }

        return Locale.forLanguageTag("pt-BR");
    }

    private String toPreferenceValue(Locale locale) {
        if (locale.getCountry().isBlank()) {
            return locale.getLanguage();
        }
        return locale.getLanguage() + "_" + locale.getCountry();
    }
}
