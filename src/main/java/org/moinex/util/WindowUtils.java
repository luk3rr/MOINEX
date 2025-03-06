/*
 * Filename: WindowUtils.java
 * Created on: October 12, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.util;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Logger;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.springframework.context.ApplicationContext;

/**
 * Utility class for managing window-related operations
 */
public final class WindowUtils
{
    private static final Logger logger = LoggerConfig.getLogger();

    /**
     * Sets the attributes of an alert dialog
     * @param alert The alert dialog
     * @param title The title of the dialog
     * @param header The header of the dialog
     * @param message The message to be displayed
     */
    private static void
    setAlertAttributes(Alert alert, String title, String header, String message)
    {
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(message);
    }

    /**
     * Shows a confirmation dialog with Yes/No options
     * @param title The title of the dialog
     * @param header The header of the dialog
     * @param message The message to be displayed
     * @return True if the user clicked Yes, false otherwise
     */
    public static boolean
    showConfirmationDialog(String title, String header, String message)
    {
        Alert alert = new Alert(AlertType.CONFIRMATION);

        // Set the confirmation button
        alert.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO);

        setAlertAttributes(alert, title, header, message);

        ButtonType result = alert.showAndWait().orElse(ButtonType.NO);
        return result == ButtonType.YES;
    }

    /**
     * Shows an information dialog with an OK button
     * @param title The title of the dialog
     * @param header The header of the dialog
     * @param message The message to be displayed
     */
    public static void
    showInformationDialog(String title, String header, String message)
    {
        Alert alert = new Alert(AlertType.INFORMATION);
        setAlertAttributes(alert, title, header, message);
        alert.showAndWait();
    }

    /**
     * Shows an error dialog with an OK button
     * @param title The title of the dialog
     * @param header The header of the dialog
     * @param message The message to be displayed
     */
    public static void showErrorDialog(String title, String header, String message)
    {
        Alert alert = new Alert(AlertType.ERROR);
        setAlertAttributes(alert, title, header, message);
        alert.showAndWait();
    }

    /**
     * Shows a success dialog with an OK button
     * @param title The title of the dialog
     * @param header The header of the dialog
     * @param message The message to be displayed
     */
    public static void showSuccessDialog(String title, String header, String message)
    {
        Alert alert = new Alert(AlertType.INFORMATION);

        // Set the success icon
        alert.setGraphic(new ImageView(new Image(
            WindowUtils.class.getResource(Constants.SUCCESS_ICON).toString())));

        setAlertAttributes(alert, title, header, message);
        alert.showAndWait();
    }

    /**
     * Centers the window on the screen
     * @param stage The stage to be centered
     */
    public static void centerWindowOnScreen(Stage stage)
    {
        stage.centerOnScreen();
    }

    /**
     * Opens a modal window (blocks interaction with the main window)
     * @param fxmlFileName The path to the FXML file to be loaded
     * @param title The title of the window
     * @param springContext The Spring application context
     * @param controllerSetup A consumer to setup the controller
     */
    public static <T> void openModalWindow(String             fxmlFileName,
                                           String             title,
                                           ApplicationContext springContext,
                                           Consumer<T>        controllerSetup)
    {
        openModalWindow(fxmlFileName, title, springContext, controllerSetup, List.of());
    }

    /**
     * Opens a modal window (blocks interaction with the main window)
     * @param fxmlFileName The path to the FXML file to be loaded
     * @param title The title of the window
     * @param springContext The Spring application context
     * @param controllerSetup A consumer to setup the controller
     * @param onHiddenActions A list of actions to be executed when the window is hidden
     */
    public static <T> void openModalWindow(String             fxmlFileName,
                                           String             title,
                                           ApplicationContext springContext,
                                           Consumer<T>        controllerSetup,
                                           List<Runnable>     onHiddenActions)
    {
        try
        {
            FXMLLoader loader =
                new FXMLLoader(WindowUtils.class.getResource(fxmlFileName));
            loader.setControllerFactory(springContext::getBean);
            Parent root = loader.load();

            Stage modalStage = new Stage();
            modalStage.initModality(Modality.APPLICATION_MODAL); // Makes it modal
            Scene scene = new Scene(root);
            scene.getStylesheets().add(
                WindowUtils.class.getResource(Constants.COMMON_STYLE_SHEET)
                    .toExternalForm());

            T controller = loader.getController();
            controllerSetup.accept(controller);

            modalStage.setTitle(title);
            modalStage.setScene(scene);

            // Execute the actions when the window is hidden
            modalStage.setOnHidden(e -> { onHiddenActions.forEach(Runnable::run); });

            modalStage.showAndWait();
        }
        catch (IOException e)
        {
            logger.severe("Error loading FXML file: " + fxmlFileName);
        }
    }

    /**
     * Opens a popup window (does not block interaction with the main window)
     * @param fxmlFileName The FXML file to load
     * @param title The title of the window
     * @param springContext The Spring application context
     * @param controllerSetup A consumer to setup the controller
     */
    public static <T> void openPopupWindow(String             fxmlFileName,
                                           String             title,
                                           ApplicationContext springContext,
                                           Consumer<T>        controllerSetup)
    {
        openPopupWindow(fxmlFileName, title, springContext, controllerSetup, List.of());
    }

    /**
     * Opens a popup window (does not block interaction with the main window)
     * @param fxmlFileName The FXML file to load
     * @param title The title of the window
     * @param springContext The Spring application context
     * @param controllerSetup A consumer to setup the controller
     * @param onHiddenActions A list of actions to be executed when the window is hidden
     */
    public static <T> void openPopupWindow(String             fxmlFileName,
                                           String             title,
                                           ApplicationContext springContext,
                                           Consumer<T>        controllerSetup,
                                           List<Runnable>     onHiddenActions)
    {
        try
        {
            FXMLLoader loader =
                new FXMLLoader(WindowUtils.class.getResource(fxmlFileName));
            loader.setControllerFactory(springContext::getBean);
            Parent root = loader.load();

            Stage popupStage = new Stage();
            Scene scene      = new Scene(root);
            scene.getStylesheets().add(
                WindowUtils.class.getResource(Constants.COMMON_STYLE_SHEET)
                    .toExternalForm());

            T controller = loader.getController();
            controllerSetup.accept(controller);

            popupStage.setTitle(title);
            popupStage.setScene(scene);

            // Execute the actions when the window is hidden
            popupStage.setOnHidden(e -> { onHiddenActions.forEach(Runnable::run); });

            popupStage.showAndWait();
        }
        catch (IOException e)
        {
            logger.severe("Error loading FXML file: " + fxmlFileName);
        }
    }
}
