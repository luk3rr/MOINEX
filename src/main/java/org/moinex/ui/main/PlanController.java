package org.moinex.ui.main;

import com.jfoenix.controls.JFXButton;
import java.util.List;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import lombok.NoArgsConstructor;
import org.moinex.ui.dialog.financialplanning.AddPlanController;
import org.moinex.util.Constants;
import org.moinex.util.WindowUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Controller;

/**
 * Controller for the Financial Planning view
 */
@Controller
@NoArgsConstructor
public class PlanController {

    @FXML private ComboBox<String> periodComboBox;

    @FXML private Label baseMonthlyIncome;

    @FXML private AnchorPane pieChartAnchorPane;

    @FXML private VBox budgetGroupVBox;

    @FXML private JFXButton budgetGroupPrevButton;

    @FXML private JFXButton budgetGroupNextButton;

    @FXML private AnchorPane budgetGroupPane1;

    @FXML private AnchorPane budgetGroupPane2;

    @FXML private AnchorPane budgetGroupPane3;

    private ConfigurableApplicationContext springContext;

    @Autowired
    public PlanController(ConfigurableApplicationContext springContext) {
        this.springContext = springContext;
    }

    @FXML
    public void initialize() {}

    @FXML
    private void handleEditPlan() {}

    @FXML
    private void handleNewPlan() {
        WindowUtils.openModalWindow(
                Constants.ADD_PLAN_FXML,
                "Add New Financial Plan",
                springContext,
                (AddPlanController controller) -> {},
                List.of());
    }
}
