/*
 * Filename: CalculatorService.java
 * Created on: January  4, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import javafx.scene.control.TextField;
import lombok.Getter;
import lombok.Setter;
import org.moinex.util.WindowUtils;
import org.springframework.stereotype.Service;

/**
 * Service class for the Calculator interface
 * <p>
 * Stores the result of the last calculation and allows it to be accessed by other
 * classes
 */
@Service
@Getter
@Setter
public class CalculatorService {
    private String result;

    public CalculatorService() {
        result = null;
    }

    public void updateComponentWithResult(TextField component) {
        if (result != null) {
            try {
                BigDecimal resultValue = new BigDecimal(result);

                if (resultValue.compareTo(BigDecimal.ZERO) < 0) {
                    WindowUtils.showInformationDialog(
                            "Invalid value", "The value must be positive");
                    return;
                }

                // Round the result to 2 decimal places
                result = resultValue.setScale(2, RoundingMode.HALF_UP).toString();

                component.setText(result);
            } catch (NumberFormatException e) {
                // Must be unreachable
                WindowUtils.showErrorDialog("Invalid value", "The value must be a number");
            }
        }
    }
}
