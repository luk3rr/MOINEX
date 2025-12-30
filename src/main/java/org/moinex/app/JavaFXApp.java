/*
 * Filename: JavaFXApp.java
 * Created on: September 15, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.app;

import java.io.IOException;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.moinex.service.I18nService;
import org.moinex.util.APIUtils;
import org.moinex.util.Constants;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * JavaFX application entry point
 */
public class JavaFXApp extends Application {
    private ConfigurableApplicationContext springContext;

    @Override
    public void init() throws Exception {
        String[] args = getParameters().getRaw().toArray(new String[0]);
        springContext = SpringApp.start(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader splashLoader =
                new FXMLLoader(getClass().getResource(Constants.SPLASH_SCREEN_FXML));
        Parent splashRoot = splashLoader.load();
        Stage splashStage = new Stage();
        splashStage.initStyle(StageStyle.UNDECORATED);
        splashStage.setScene(new Scene(splashRoot));
        splashStage.show();

        Thread.ofVirtual()
                .start(
                        () -> {
                            try {
                                I18nService i18nService = springContext.getBean(I18nService.class);
                                FXMLLoader loader =
                                        new FXMLLoader(
                                                getClass().getResource(Constants.MAIN_FXML),
                                                i18nService.getBundle());
                                loader.setControllerFactory(springContext::getBean);
                                Parent mainRoot = loader.load();

                                Thread.sleep(1000);

                                Platform.runLater(
                                        () -> {
                                            primaryStage.setTitle(i18nService.tr("app.title"));
                                            primaryStage.setScene(new Scene(mainRoot));
                                            primaryStage.show();
                                            splashStage.close();
                                        });
                            } catch (InterruptedException | IOException e) {
                                Thread.currentThread().interrupt();
                                Platform.exit();
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
