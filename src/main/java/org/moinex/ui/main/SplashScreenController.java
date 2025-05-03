/*
 * Filename: SplashScreenController.java
 * Created on: November 25, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.main;

import javafx.fxml.FXML;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Controller;

/**
 * Controller for the splash screen
 */
@Controller
public class SplashScreenController {

    public SplashScreenController(ConfigurableApplicationContext springContext) {}

    @FXML
    public void initialize() {
        // Splash screen
    }
}
