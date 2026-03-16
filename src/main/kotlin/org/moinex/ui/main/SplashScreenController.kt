/*
 * Filename: SplashScreenController.kt (original filename: SplashScreenController.java)
 * Created on: November 25, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 16/03/2026
 */

package org.moinex.ui.main

import javafx.fxml.FXML
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.stereotype.Controller

@Controller
class SplashScreenController(
    springContext: ConfigurableApplicationContext,
) {
    @FXML
    fun initialize() {
        // Splash screen
    }
}
