package org.moinex.ui.main;

import java.io.IOException;
import javafx.fxml.FXML;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import lombok.NoArgsConstructor;
import org.moinex.service.I18nService;
import org.moinex.util.Constants;
import org.moinex.util.UIUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Controller;

@Controller
@NoArgsConstructor
public class GoalsPlansController {

    private static final Logger logger = LoggerFactory.getLogger(GoalsPlansController.class);

    @FXML private TabPane tabPane;
    @FXML private Tab goalsTab;
    @FXML private Tab plansTab;

    private ConfigurableApplicationContext springContext;
    private I18nService i18nService;

    @Autowired
    public GoalsPlansController(
            ConfigurableApplicationContext springContext, I18nService i18nService) {
        this.springContext = springContext;
        this.i18nService = i18nService;
    }

    @FXML
    public void initialize() {
        try {
            UIUtils.loadContentIntoTab(
                    goalsTab,
                    Constants.GOALS_FXML,
                    Constants.GOALS_STYLE_SHEET,
                    springContext,
                    getClass(),
                    i18nService.getBundle());
            UIUtils.loadContentIntoTab(
                    plansTab,
                    Constants.PLANS_FXML,
                    Constants.PLANS_STYLE_SHEET,
                    springContext,
                    getClass(),
                    i18nService.getBundle());
        } catch (IOException e) {
            logger.error("Error loading content: '{}'", e.getMessage());
        }
    }
}
