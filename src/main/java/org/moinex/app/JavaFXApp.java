/*
 * Filename: JavaFXApp.java
 * Created on: September 15, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.app;

import java.io.IOException;

import org.moinex.util.APIUtils;
import org.moinex.util.Constants;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * JavaFX application entry point
 */
public class JavaFXApp extends Application {
    private ConfigurableApplicationContext springContext;

    @Override
    public void init() throws Exception {
        String[] args = getParameters().getRaw().toArray(new String[0]);

        springContext =
                new SpringApplicationBuilder().sources(MainApplication.class).run(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader splashLoader =
                new FXMLLoader(getClass().getResource(Constants.SPLASH_SCREEN_FXML));
        Parent splashRoot = splashLoader.load();
        Stage splashStage = new Stage();
        splashStage.initStyle(StageStyle.UNDECORATED); // Remove window border
        splashStage.setScene(new Scene(splashRoot));
        splashStage.show();
        Thread.ofVirtual().start(() -> {
            try {
                springContext =
                        new SpringApplicationBuilder()
                                .sources(MainApplication.class)
                                .run(getParameters().getRaw().toArray(new String[0]));

                FXMLLoader loader =
                        new FXMLLoader(getClass().getResource(Constants.MAIN_FXML));
                loader.setControllerFactory(springContext::getBean);
                Parent mainRoot = loader.load();

                // wait 1 second before showing the main window
                Thread.sleep(1000);

                javafx.application.Platform.runLater(() -> {
                    primaryStage.setTitle(Constants.APP_NAME);
                    primaryStage.setScene(new Scene(mainRoot));
                    primaryStage.show();
                    splashStage.close();
                });
            } catch (InterruptedException | IOException e) {
                Thread.currentThread().interrupt();
                javafx.application.Platform.exit();
            }
        });
    }

    @Override
    public void stop() throws Exception {
        APIUtils.shutdownExecutor();

        springContext.close();
        super.stop();
        Platform.exit();
    }
}
