/*
 * Filename: CalculatorController.java
 * Created on: January  4, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ui.common;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import lombok.NoArgsConstructor;
import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;
import org.moinex.service.CalculatorService;
import org.moinex.util.WindowUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

/**
 * Controller class for the Calculator application
 */
@Controller
@NoArgsConstructor
public class CalculatorController
{
    @FXML
    private TextField mainDisplay;

    @FXML
    private TextField expressionDisplay;

    private CalculatorService calculatorService;

    private StringBuilder expression = new StringBuilder();

    /**
     * Constructor for CalculatorController
     * @param calculatorService The service for the calculator
     */
    @Autowired
    public CalculatorController(CalculatorService calculatorService)
    {
        this.calculatorService = calculatorService;
    }

    @FXML
    private void initialize()
    {
        // Clear the result when the calculator is opened
        calculatorService.setResult(null);
        expression.setLength(0);
        expressionDisplay.setText("");
        mainDisplay.setText("0");
    }

    @FXML
    private void addAction(ActionEvent ignoredEvent)
    {
        addOperator("+");
    }

    @FXML
    private void minusAction(ActionEvent ignoredEvent)
    {
        addOperator("-");
    }

    @FXML
    private void divideAction(ActionEvent ignoredEvent)
    {
        addOperator("/");
    }

    @FXML
    private void multiplicationAction(ActionEvent ignoredEvent)
    {
        addOperator("*");
    }

    @FXML
    private void calculate(ActionEvent ignoredEvent)
    {
        if (expression.isEmpty())
        {
            return;
        }

        try
        {
            Double result = evaluateExpression(expression.toString());

            expressionDisplay.setText(
                expression.toString().replaceAll("([+\\-*/])", " $1 ") + " = " +
                result);

            mainDisplay.setText(String.valueOf(result));

            expression = new StringBuilder(
                String.valueOf(result)); // Save the result for further calculations

            calculatorService.setResult(result.toString());
        }
        catch (IllegalArgumentException e)
        {
            expressionDisplay.setText("Error");
            mainDisplay.setText("");
            expression.setLength(0); // Clear the expression

            calculatorService.setResult(null);

            WindowUtils.showErrorDialog("Error evaluating expression", e.getMessage());
        }
    }

    @FXML
    private void clearTextField(ActionEvent ignoredEvent)
    {
        expression.setLength(0);
        expressionDisplay.setText("");
        mainDisplay.setText("");
    }

    @FXML
    private void finish(ActionEvent event)
    {
        calculate(event);

        Stage stage = (Stage)mainDisplay.getScene().getWindow();
        stage.close();
    }

    @FXML
    private void buttonDotClicked(ActionEvent ignoredEvent)
    {
        if (!expression.toString().endsWith(".") && !currentToken().contains("."))
        {
            if (expression.isEmpty() ||
                isOperator(expression.charAt(expression.length() - 1)))
            {
                expression.append("0.");
            }
            else
            {
                expression.append(".");
            }

            updateExpressionDisplay();
        }
    }

    @FXML
    private void button0Clicked(ActionEvent ignoredEvent)
    {
        addNumber("0");
    }

    @FXML
    private void button1Clicked(ActionEvent ignoredEvent)
    {
        addNumber("1");
    }

    @FXML
    private void button2Clicked(ActionEvent ignoredEvent)
    {
        addNumber("2");
    }

    @FXML
    private void button3Clicked(ActionEvent ignoredEvent)
    {
        addNumber("3");
    }

    @FXML
    private void button4Clicked(ActionEvent ignoredEvent)
    {
        addNumber("4");
    }

    @FXML
    private void button5Clicked(ActionEvent ignoredEvent)
    {
        addNumber("5");
    }

    @FXML
    private void button6Clicked(ActionEvent ignoredEvent)
    {
        addNumber("6");
    }

    @FXML
    private void button7Clicked(ActionEvent ignoredEvent)
    {
        addNumber("7");
    }

    @FXML
    private void button8Clicked(ActionEvent ignoredEvent)
    {
        addNumber("8");
    }

    @FXML
    private void button9Clicked(ActionEvent ignoredEvent)
    {
        addNumber("9");
    }

    public void updateExpressionDisplay()
    {
        // Show expression with spaces between operators
        expressionDisplay.setText(
            expression.toString().replaceAll("([+\\-*/])", " $1 "));
    }

    public void updateMainDisplay()
    {
        // Get the last number in the expression or a result
        mainDisplay.setText(currentToken());
    }

    public void addNumber(String number)
    {
        expression.append(number);
        updateExpressionDisplay();
        updateMainDisplay();
    }

    public void addOperator(String operator)
    {
        if (!expression.isEmpty() &&
            !isOperator(expression.charAt(expression.length() - 1)))
        {
            expression.append(operator);
            updateExpressionDisplay();
        }
    }

    private boolean isOperator(char c)
    {
        return c == '+' || c == '-' || c == '*' || c == '/';
    }

    private String currentToken()
    {
        // Find the last operator in the expression and get the token after it
        int lastOperatorIndex =
            Math.max(expression.lastIndexOf("+"),
                     Math.max(expression.lastIndexOf("-"),
                              Math.max(expression.lastIndexOf("*"),
                                       expression.lastIndexOf("/"))));

        String token = expression.substring(lastOperatorIndex + 1);

        // If the token starts with "-" and is the first character of the expression or
        // after an operator, it is a negative number
        if (token.startsWith("-") && lastOperatorIndex == -1)
        {
            return token;
        }

        // If the token starts with "-" and the previous operator was also a subtraction
        // operator, treat it as a negative number
        if (token.startsWith("-") && expression.charAt(lastOperatorIndex) != '-' &&
            lastOperatorIndex != -1)
        {
            return token;
        }

        return !token.isEmpty() ? token : "";
    }

    private Double evaluateExpression(String expression)
    {
        Expression exp    = new ExpressionBuilder(expression).build();
        Double     result = exp.evaluate();

        // If the result is a number, return it. Otherwise, return 0
        return result instanceof Number number ? result.doubleValue() : 0.0;
    }
}
