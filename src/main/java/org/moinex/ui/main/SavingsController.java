/*
 * Filename: SavingsController.java
 * Created on: January  5, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.main;

import java.io.IOException;
import javafx.fxml.FXML;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import lombok.NoArgsConstructor;
import org.moinex.service.PreferencesService;
import org.moinex.util.Constants;
import org.moinex.util.UIUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Controller;

/** Controller class for the Savings view - Container for tabs */
@Controller
@NoArgsConstructor
public class SavingsController {
    private static final Logger logger = LoggerFactory.getLogger(SavingsController.class);

    @FXML private TabPane tabPane;
    @FXML private Tab overviewTab;
    @FXML private Tab stocksFundsTab;
    @FXML private Tab bondsTab;

    private ConfigurableApplicationContext springContext;
    private PreferencesService preferencesService;

    @Autowired
    public SavingsController(
            ConfigurableApplicationContext springContext, PreferencesService preferencesService) {
        this.springContext = springContext;
        this.preferencesService = preferencesService;
    }

    @FXML
    public void initialize() {
        try {
            UIUtils.loadContentIntoTab(
                    overviewTab,
                    Constants.SAVINGS_OVERVIEW_FXML,
                    Constants.SAVINGS_OVERVIEW_STYLE_SHEET,
                    springContext,
                    getClass(),
                    preferencesService.getBundle());
            UIUtils.loadContentIntoTab(
                    stocksFundsTab,
                    Constants.SAVINGS_STOCKS_FUNDS_FXML,
                    Constants.SAVINGS_STOCKS_FUNDS_STYLE_SHEET,
                    springContext,
                    getClass(),
                    preferencesService.getBundle());
            UIUtils.loadContentIntoTab(
                    bondsTab,
                    Constants.SAVINGS_BONDS_FXML,
                    Constants.SAVINGS_BONDS_STYLE_SHEET,
                    springContext,
                    getClass(),
                    preferencesService.getBundle());
        } catch (IOException e) {
            logger.error("Error loading content: '{}'", e.getMessage());
        }
    }
}
