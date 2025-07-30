package org.moinex.ui.dialog.financialplanning;

import java.util.*;
import javafx.fxml.FXML;
import lombok.NoArgsConstructor;
import org.moinex.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

/**
 * Controller for the Add Budget Group dialog
 */
@Controller
@NoArgsConstructor
public class AddBudgetGroupController extends BaseBudgetGroupController {

    @Autowired
    public AddBudgetGroupController(CategoryService categoryService) {
        super(categoryService);
    }

    @FXML
    public void initialize() {
        super.initialize();
    }
}
