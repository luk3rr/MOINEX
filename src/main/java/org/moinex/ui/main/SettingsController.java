package org.moinex.ui.main;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.ComboBox;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import lombok.NoArgsConstructor;
import org.moinex.service.PreferencesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Controller;

@Controller
@NoArgsConstructor
public class SettingsController {

    @FXML private ComboBox<Locale> languageComboBox;

    private PreferencesService preferencesService;

    private ConfigurableApplicationContext springContext;

    private final Map<Locale, String> localeLabels = new LinkedHashMap<>();

    @Autowired
    public SettingsController(
            PreferencesService preferencesService, ConfigurableApplicationContext springContext) {
        this.preferencesService = preferencesService;
        this.springContext = springContext;

        localeLabels.put(Locale.forLanguageTag("pt-BR"), "Português");
        localeLabels.put(Locale.ENGLISH, "English");
    }

    @FXML
    private void initialize() {
        languageComboBox.getItems().setAll(preferencesService.getSupportedLocales());

        languageComboBox.setConverter(
                new StringConverter<>() {
                    @Override
                    public String toString(Locale locale) {
                        if (locale == null) {
                            return "";
                        }
                        return localeLabels.getOrDefault(locale, locale.toLanguageTag());
                    }

                    @Override
                    public Locale fromString(String string) {
                        return null;
                    }
                });

        languageComboBox.getSelectionModel().select(preferencesService.getLocale());

        languageComboBox
                .valueProperty()
                .addListener(
                        (obs, oldVal, newVal) -> {
                            if (newVal == null || newVal.equals(oldVal)) {
                                return;
                            }

                            preferencesService.setLocale(newVal);

                            if (languageComboBox.getScene() == null
                                    || languageComboBox.getScene().getWindow() == null) {
                                return;
                            }

                            // Capture references before Platform.runLater
                            final javafx.scene.Scene scene = languageComboBox.getScene();
                            final Stage stage = (Stage) scene.getWindow();

                            Platform.runLater(
                                    () -> {
                                        try {
                                            FXMLLoader loader =
                                                    new FXMLLoader(
                                                            getClass()
                                                                    .getResource(
                                                                            org.moinex.util
                                                                                    .Constants
                                                                                    .MAIN_FXML),
                                                            preferencesService.getBundle());
                                            loader.setControllerFactory(springContext::getBean);

                                            Parent mainRoot = loader.load();

                                            scene.setRoot(mainRoot);
                                            stage.setTitle(
                                                    preferencesService.translate("app.title"));
                                        } catch (IOException e) {
                                            // If reload fails, keep current UI
                                        }
                                    });
                        });
    }
}
