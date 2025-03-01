/*
 * Filename: MainController.java
 * Created on: September 19, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.main;

import java.io.IOException;
import java.util.List;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.moinex.ui.common.CalculatorController;
import org.moinex.ui.common.CalendarController;
import org.moinex.util.Constants;
import org.moinex.util.WindowUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Controller;

/**
 * Controller for the main view
 */
@Controller
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

    @Autowired
    private ConfigurableApplicationContext springContext;

    private boolean  isMenuExpanded = false;
    private Button[] sidebarButtons;

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
            getClass().getResource(Constants.MAIN_STYLE_SHEET).toExternalForm());

        footbarArea.getStylesheets().add(
            getClass().getResource(Constants.MAIN_STYLE_SHEET).toExternalForm());

        menuButton.setOnAction(event -> ToggleMenu());

        homeButton.setOnAction(event -> {
            LoadContent(Constants.HOME_FXML, Constants.HOME_STYLE_SHEET);
            UpdateSelectedButton(homeButton);
        });

        walletButton.setOnAction(event -> {
            LoadContent(Constants.WALLET_FXML, Constants.WALLET_STYLE_SHEET);
            UpdateSelectedButton(walletButton);
        });

        creditCardButton.setOnAction(event -> {
            LoadContent(Constants.CREDIT_CARD_FXML, Constants.CREDIT_CARD_STYLE_SHEET);
            UpdateSelectedButton(creditCardButton);
        });

        transactionButton.setOnAction(event -> {
            LoadContent(Constants.TRANSACTION_FXML, Constants.TRANSACTION_STYLE_SHEET);
            UpdateSelectedButton(transactionButton);
        });

        goalsButton.setOnAction(event -> {
            LoadContent(Constants.GOALS_FXML, Constants.GOALS_STYLE_SHEET);
            UpdateSelectedButton(goalsButton);
        });

        savingsButton.setOnAction(event -> {
            LoadContent(Constants.SAVINGS_FXML, Constants.SAVINGS_STYLE_SHEET);
            UpdateSelectedButton(savingsButton);
        });

        importButton.setOnAction(event -> {
            LoadContent(Constants.CSV_IMPORT_FXML, Constants.CSV_IMPORT_STYLE_SHEET);
            UpdateSelectedButton(importButton);
        });

        settingsButton.setOnAction(event -> {
            LoadContent(Constants.SETTINGS_FXML, Constants.SETTINGS_STYLE_SHEET);
            UpdateSelectedButton(settingsButton);
        });

        // Load start page
        LoadContent(Constants.HOME_FXML, Constants.HOME_STYLE_SHEET);
        UpdateSelectedButton(homeButton);
    }

    @FXML
    private void handleOpenCalculator()
    {
        WindowUtils.OpenPopupWindow(Constants.CALCULATOR_FXML,
                                    "Calculator",
                                    springContext,
                                    (CalculatorController controller)
                                        -> {},
                                    List.of(() -> {}));
    }

    @FXML
    private void handleOpenCalendar()
    {
        WindowUtils.OpenPopupWindow(Constants.CALENDAR_FXML,
                                    "Calendar",
                                    springContext,
                                    (CalendarController controller)
                                        -> {},
                                    List.of(() -> {}));
    }

    /**
     * Load the content of the main window with the content of the FXML file
     * passed as parameter
     * @param fxmlFile The path to the FXML file to be loaded
     */
    public void LoadContent(String fxmlFile, String styleSheet)
    {
        try
        {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFile));
            loader.setControllerFactory(springContext::getBean);
            Parent newContent = loader.load();

            newContent.getStylesheets().add(
                getClass().getResource(styleSheet).toExternalForm());

            newContent.getStylesheets().add(
                getClass().getResource(Constants.COMMON_STYLE_SHEET).toExternalForm());

            AnchorPane.setTopAnchor(newContent, 0.0);
            AnchorPane.setRightAnchor(newContent, 0.0);
            AnchorPane.setBottomAnchor(newContent, 0.0);
            AnchorPane.setLeftAnchor(newContent, 0.0);

            contentArea.getChildren().clear();
            contentArea.getChildren().add(newContent);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    private void UpdateSelectedButton(Button selectedButton)
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
    private void ToggleMenu()
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
        // before the animation starts. This encreses the visual quality
        // of the animation
        if (isMenuExpanded)
        {
            SetButtonTextVisibility(false);
        }

        timeline.setOnFinished(event -> {
            SetButtonTextVisibility(isMenuExpanded);
            SetButtonWidth(targetWidth);
        });

        timeline.play();

        isMenuExpanded = !isMenuExpanded;
    }

    /**
     * Set the visibility of the text of the sidebarButtons
     * @param visible True if the text should be visible, false otherwise
     */
    private void SetButtonTextVisibility(boolean visible)
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
    private void SetButtonWidth(double width)
    {
        for (Button button : sidebarButtons)
        {
            button.setPrefWidth(width);
        }
    }
}
