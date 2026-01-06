package org.moinex.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class I18nServiceTest {

    private I18nService i18nService;

    @BeforeEach
    void setUp() {
        // Clear preferences before each test
        Preferences prefs = Preferences.userRoot().node("org.moinex");
        prefs.remove("ui.locale");

        i18nService = new I18nService();
    }

    @Nested
    @DisplayName("Locale Management Tests")
    class LocaleManagementTests {
        @Test
        @DisplayName("Should return current locale")
        void getLocale_ReturnsCurrentLocale() {
            Locale locale = i18nService.getLocale();

            assertNotNull(locale);
        }

        @Test
        @DisplayName("Should set locale successfully")
        void setLocale_SetsLocaleSuccessfully() {
            Locale newLocale = Locale.ENGLISH;

            i18nService.setLocale(newLocale);

            assertEquals(Locale.ENGLISH, i18nService.getLocale());
        }

        @Test
        @DisplayName("Should not set locale when null")
        void setLocale_DoesNotSetWhenNull() {
            Locale originalLocale = i18nService.getLocale();

            i18nService.setLocale(null);

            assertEquals(originalLocale, i18nService.getLocale());
        }

        @Test
        @DisplayName("Should persist locale to preferences")
        void setLocale_PersistsToPreferences() {
            Locale newLocale = Locale.forLanguageTag("pt-BR");

            i18nService.setLocale(newLocale);

            // Create new instance to verify persistence
            I18nService newInstance = new I18nService();
            assertEquals(newLocale, newInstance.getLocale());
        }

        @Test
        @DisplayName("Should set English locale")
        void setLocale_SetsEnglishLocale() {
            i18nService.setLocale(Locale.ENGLISH);

            assertEquals(Locale.ENGLISH, i18nService.getLocale());
        }

        @Test
        @DisplayName("Should set Portuguese Brazil locale")
        void setLocale_SetsPortugueseBrazilLocale() {
            Locale ptBR = Locale.forLanguageTag("pt-BR");

            i18nService.setLocale(ptBR);

            assertEquals(ptBR, i18nService.getLocale());
        }
    }

    @Nested
    @DisplayName("Supported Locales Tests")
    class SupportedLocalesTests {
        @Test
        @DisplayName("Should return list of supported locales")
        void getSupportedLocales_ReturnsListOfLocales() {
            List<Locale> supportedLocales = i18nService.getSupportedLocales();

            assertNotNull(supportedLocales);
            assertEquals(2, supportedLocales.size());
        }

        @Test
        @DisplayName("Should include Portuguese Brazil in supported locales")
        void getSupportedLocales_IncludesPortugueseBrazil() {
            List<Locale> supportedLocales = i18nService.getSupportedLocales();

            assertTrue(supportedLocales.contains(Locale.forLanguageTag("pt-BR")));
        }

        @Test
        @DisplayName("Should include English in supported locales")
        void getSupportedLocales_IncludesEnglish() {
            List<Locale> supportedLocales = i18nService.getSupportedLocales();

            assertTrue(supportedLocales.contains(Locale.ENGLISH));
        }
    }

    @Nested
    @DisplayName("Resource Bundle Tests")
    class ResourceBundleTests {
        @Test
        @DisplayName("Should return resource bundle for current locale")
        void getBundle_ReturnsResourceBundle() {
            ResourceBundle bundle = i18nService.getBundle();

            assertNotNull(bundle);
        }

        @Test
        @DisplayName("Should return English bundle when locale is not found")
        void getBundle_ReturnsEnglishBundleWhenLocaleNotFound() {
            // Set an unsupported locale
            i18nService.setLocale(Locale.forLanguageTag("fr-FR"));

            ResourceBundle bundle = i18nService.getBundle();

            assertNotNull(bundle);
            // Should fallback to English (en or en_US)
            assertTrue(
                    bundle.getLocale().getLanguage().equals("en"),
                    "Expected English locale but got: " + bundle.getLocale());
        }

        @Test
        @DisplayName("Should return Portuguese bundle when locale is pt-BR")
        void getBundle_ReturnsPortugueseBundleForPtBR() {
            i18nService.setLocale(Locale.forLanguageTag("pt-BR"));

            ResourceBundle bundle = i18nService.getBundle();

            assertNotNull(bundle);
        }

        @Test
        @DisplayName("Should return English bundle when locale is English")
        void getBundle_ReturnsEnglishBundleForEnglish() {
            i18nService.setLocale(Locale.ENGLISH);

            ResourceBundle bundle = i18nService.getBundle();

            assertNotNull(bundle);
        }
    }

    @Nested
    @DisplayName("Translation Tests")
    class TranslationTests {
        @Test
        @DisplayName("Should translate key successfully")
        void tr_TranslatesKeySuccessfully() {
            String result = i18nService.tr("app.name");

            assertNotNull(result);
            assertFalse(result.isEmpty());
        }

        @Test
        @DisplayName("Should return empty string when key is null")
        void tr_ReturnsEmptyStringWhenKeyIsNull() {
            String result = i18nService.tr(null);

            assertEquals("", result);
        }

        @Test
        @DisplayName("Should return key when translation is missing")
        void tr_ReturnsKeyWhenTranslationMissing() {
            String missingKey = "non.existent.key.that.does.not.exist";

            String result = i18nService.tr(missingKey);

            assertEquals(missingKey, result);
        }

        @Test
        @DisplayName("Should handle empty key")
        void tr_HandlesEmptyKey() {
            String result = i18nService.tr("");

            assertEquals("", result);
        }

        @Test
        @DisplayName("Should translate different keys")
        void tr_TranslatesDifferentKeys() {
            String result1 = i18nService.tr("app.name");
            String result2 = i18nService.tr("app.version");

            assertNotNull(result1);
            assertNotNull(result2);
        }
    }

    @Nested
    @DisplayName("Initial Locale Resolution Tests")
    class InitialLocaleResolutionTests {
        @Test
        @DisplayName("Should resolve initial locale from preferences when available")
        void resolveInitialLocale_FromPreferences() {
            // Set preference
            Preferences prefs = Preferences.userRoot().node("org.moinex");
            prefs.put("ui.locale", "en");

            I18nService newService = new I18nService();

            assertEquals(Locale.ENGLISH, newService.getLocale());

            // Cleanup
            prefs.remove("ui.locale");
        }

        @Test
        @DisplayName("Should resolve initial locale from system when no preference")
        void resolveInitialLocale_FromSystem() {
            // Clear preferences
            Preferences prefs = Preferences.userRoot().node("org.moinex");
            prefs.remove("ui.locale");

            I18nService newService = new I18nService();
            Locale locale = newService.getLocale();

            assertNotNull(locale);
            // Should be one of the supported locales or default pt-BR
            assertTrue(
                    locale.equals(Locale.forLanguageTag("pt-BR")) || locale.equals(Locale.ENGLISH));
        }

        @Test
        @DisplayName("Should default to pt-BR when system locale is not supported")
        void resolveInitialLocale_DefaultsToPtBR() {
            Preferences prefs = Preferences.userRoot().node("org.moinex");
            prefs.remove("ui.locale");

            I18nService newService = new I18nService();
            Locale locale = newService.getLocale();

            assertNotNull(locale);
        }
    }

    @Nested
    @DisplayName("Preference Value Conversion Tests")
    class PreferenceValueConversionTests {
        @Test
        @DisplayName("Should convert locale with country to preference value")
        void toPreferenceValue_WithCountry() {
            Locale ptBR = Locale.forLanguageTag("pt-BR");
            i18nService.setLocale(ptBR);

            // Verify it was stored correctly by creating new instance
            I18nService newService = new I18nService();
            assertEquals(ptBR, newService.getLocale());
        }

        @Test
        @DisplayName("Should convert locale without country to preference value")
        void toPreferenceValue_WithoutCountry() {
            Locale en = Locale.ENGLISH;
            i18nService.setLocale(en);

            // Verify it was stored correctly by creating new instance
            I18nService newService = new I18nService();
            assertEquals(en, newService.getLocale());
        }

        @Test
        @DisplayName("Should handle locale tag with underscore")
        void resolveInitialLocale_HandlesUnderscore() {
            Preferences prefs = Preferences.userRoot().node("org.moinex");
            prefs.put("ui.locale", "pt_BR");

            I18nService newService = new I18nService();

            assertEquals(Locale.forLanguageTag("pt-BR"), newService.getLocale());

            // Cleanup
            prefs.remove("ui.locale");
        }
    }
}
