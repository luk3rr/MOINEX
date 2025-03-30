/*
 * Filename: MainController.java
 * Created on: September 19, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.main;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import javafx.util.Pair;
import lombok.NoArgsConstructor;
import org.moinex.services.UserPreferencesService;
import org.moinex.ui.common.CalculatorController;
import org.moinex.ui.common.CalendarController;
import org.moinex.util.Constants;
import org.moinex.util.WindowUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Controller;

/**
 * Controller for the main view
 */
@Controller
@NoArgsConstructor
public class MainController
{
    @FXML
    private VBox sidebar;

    @FXML
    private AnchorPane rootPane;

    @FXML
    private HBox footbarArea;

    @FXML
    private Button menuButton;

    @FXML
    private Button homeButton;

    @FXML
    private Button walletButton;

    @FXML
    private Button creditCardButton;

    @FXML
    private Button transactionButton;

    @FXML
    private Button goalsButton;

    @FXML
    private Button savingsButton;

    @FXML
    private Button importButton;

    @FXML
    private Button settingsButton;

    @FXML
    private AnchorPane contentArea;

    @FXML
    private ImageView toggleMonetaryValuesIcon;

    private ConfigurableApplicationContext springContext;

    private UserPreferencesService userPreferencesService;

    private Pair<String, String> currentContent;

    private boolean  isMenuExpanded = false;
    private Button[] sidebarButtons;

    private static final Logger logger = LoggerFactory.getLogger(MainController.class);

    @Autowired
    public MainController(ConfigurableApplicationContext springContext, UserPreferencesService userPreferencesService) {
        this.springContext = springContext;
        this.userPreferencesService = userPreferencesService;
    }

    @FXML
    public void initialize()
    {
        // Create an array with all the buttons in the sidebar
        // This is used to update the style of the selected button
        // when the user clicks on it or when the sidebar is toggled
        sidebarButtons =
            new Button[] { menuButton,       homeButton,        walletButton,
                           creditCardButton, transactionButton, goalsButton,
                           savingsButton,    importButton,      settingsButton };

        rootPane.getStylesheets().add(
            Objects.requireNonNull(getClass().getResource(Constants.MAIN_STYLE_SHEET)).toExternalForm());

        footbarArea.getStylesheets().add(
            Objects.requireNonNull(getClass().getResource(Constants.MAIN_STYLE_SHEET)).toExternalForm());

        menuButton.setOnAction(event -> toggleMenu());

        homeButton.setOnAction(event -> {
            loadContent(Constants.HOME_FXML, Constants.HOME_STYLE_SHEET);
            updateSelectedButton(homeButton);
        });

        walletButton.setOnAction(event -> {
            loadContent(Constants.WALLET_FXML, Constants.WALLET_STYLE_SHEET);
            updateSelectedButton(walletButton);
        });

        creditCardButton.setOnAction(event -> {
            loadContent(Constants.CREDIT_CARD_FXML, Constants.CREDIT_CARD_STYLE_SHEET);
            updateSelectedButton(creditCardButton);
        });

        transactionButton.setOnAction(event -> {
            loadContent(Constants.TRANSACTION_FXML, Constants.TRANSACTION_STYLE_SHEET);
            updateSelectedButton(transactionButton);
        });

        goalsButton.setOnAction(event -> {
            loadContent(Constants.GOALS_FXML, Constants.GOALS_STYLE_SHEET);
            updateSelectedButton(goalsButton);
        });

        savingsButton.setOnAction(event -> {
            loadContent(Constants.SAVINGS_FXML, Constants.SAVINGS_STYLE_SHEET);
            updateSelectedButton(savingsButton);
        });

        importButton.setOnAction(event -> {
            loadContent(Constants.CSV_IMPORT_FXML, Constants.CSV_IMPORT_STYLE_SHEET);
            updateSelectedButton(importButton);
        });

        settingsButton.setOnAction(event -> {
            loadContent(Constants.SETTINGS_FXML, Constants.SETTINGS_STYLE_SHEET);
            updateSelectedButton(settingsButton);
        });

        // Load start page
        loadContent(Constants.HOME_FXML, Constants.HOME_STYLE_SHEET);
        updateSelectedButton(homeButton);
    }

    @FXML
    private void handleOpenCalculator()
    {
        WindowUtils.openPopupWindow(Constants.CALCULATOR_FXML,
                                    "Calculator",
                                    springContext,
                                    (CalculatorController controller)
                                        -> {},
                                    List.of(() -> {}));
    }

    @FXML
    private void handleOpenCalendar()
    {
        WindowUtils.openPopupWindow(Constants.CALENDAR_FXML,
                                    "Calendar",
                                    springContext,
                                    (CalendarController controller)
                                        -> {},
                                    List.of(() -> {}));
    }

    @FXML
    private void handleToggleMonetaryValues()
    {
        userPreferencesService.toggleHideMonetaryValues();

        toggleMonetaryValuesIcon.setImage(new Image(
            userPreferencesService.showMonetaryValues() ? Constants.SHOW_ICON
                                                        : Constants.HIDE_ICON));

        // Reload all the interface
        loadContent(currentContent.getKey(), currentContent.getValue());
    }

    /**
     * Load the content of the main window with the content of the FXML file
     * passed as parameter
     * @param fxmlFile The path to the FXML file to be loaded
     */
    public void loadContent(String fxmlFile, String styleSheet)
    {
        try
        {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFile));
            loader.setControllerFactory(springContext::getBean);
            Parent newContent = loader.load();

            newContent.getStylesheets().add(
                Objects.requireNonNull(getClass().getResource(styleSheet)).toExternalForm());

            newContent.getStylesheets().add(
                Objects.requireNonNull(getClass().getResource(Constants.COMMON_STYLE_SHEET)).toExternalForm());

            AnchorPane.setTopAnchor(newContent, 0.0);
            AnchorPane.setRightAnchor(newContent, 0.0);
            AnchorPane.setBottomAnchor(newContent, 0.0);
            AnchorPane.setLeftAnchor(newContent, 0.0);

            contentArea.getChildren().clear();
            contentArea.getChildren().add(newContent);

            currentContent = new Pair<>(fxmlFile, styleSheet);
        }
        catch (IOException e)
        {
            logger.error("Error loading content: '{}'", e.getMessage());
        }
    }

    private void updateSelectedButton(Button selectedButton)
    {
        for (Button button : sidebarButtons)
        {
            button.getStyleClass().remove(Constants.SIDEBAR_SELECTED_BUTTON_STYLE);
        }

        selectedButton.getStyleClass().add(Constants.SIDEBAR_SELECTED_BUTTON_STYLE);
    }

    /**
     * Toggle the visibility of the sidebar menu
     */
    private void toggleMenu()
    {
        double targetWidth = isMenuExpanded ? Constants.MENU_COLLAPSED_WIDTH
                                            : Constants.MENU_EXPANDED_WIDTH;

        Timeline timeline = new Timeline();

        KeyValue keyValueSidebarWidth =
            new KeyValue(sidebar.prefWidthProperty(), targetWidth);

        KeyFrame keyFrame =
            new KeyFrame(Duration.millis(Constants.MENU_ANIMATION_DURATION),
                         keyValueSidebarWidth);

        timeline.getKeyFrames().add(keyFrame);

        // If the menu is being expanded, hide the text of the buttons
        // before the animation starts.
        // This increases the visual quality
        // of the animation
        if (isMenuExpanded)
        {
            setButtonTextVisibility(false);
        }

        timeline.setOnFinished(event -> {
            setButtonTextVisibility(isMenuExpanded);
            setButtonWidth(targetWidth);
        });

        timeline.play();

        isMenuExpanded = !isMenuExpanded;
    }

    /**
     * Set the visibility of the text of the sidebarButtons
     * @param visible True if the text should be visible, false otherwise
     */
    private void setButtonTextVisibility(boolean visible)
    {
        for (Button button : sidebarButtons)
        {
            button.setContentDisplay(visible ? ContentDisplay.LEFT
                                             : ContentDisplay.GRAPHIC_ONLY);
        }
    }

    /**
     * Set the width of the sidebarButtons
     * @param width The width to be set
     */
    private void setButtonWidth(double width)
    {
        for (Button button : sidebarButtons)
        {
            button.setPrefWidth(width);
        }
    }
}
