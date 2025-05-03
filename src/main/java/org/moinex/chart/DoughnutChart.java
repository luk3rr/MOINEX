/*
 * Filename: DoughnutChart.java
 * Created on: October 7, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.chart;

import javafx.animation.ScaleTransition;
import javafx.collections.ObservableList;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;
import org.moinex.util.UIUtils;

public class DoughnutChart extends PieChart {
    private final Circle innerCircle;
    private final Label centerLabel;
    private final StackPane stackPane;
    private final double seriesTotal;

    public DoughnutChart(ObservableList<Data> pieData) {
        super(pieData);

        innerCircle = new Circle();
        centerLabel = new Label();
        stackPane = new StackPane();

        // Set some default styles for the inner circle and label
        innerCircle.setFill(Color.WHITE);
        innerCircle.setStroke(Color.WHITE);

        // Define negative values as zero
        pieData.forEach(
                data -> {
                    if (data.getPieValue() < 0) {
                        data.setPieValue(0);
                    }
                });

        // Calculate the total value of the chart
        seriesTotal = pieData.stream().mapToDouble(PieChart.Data::getPieValue).sum();

        // Add event handlers for each section of the chart
        for (PieChart.Data data : getData()) {
            Node node = data.getNode();

            double percentage = (data.getPieValue() / seriesTotal) * 100;
            Tooltip tooltip = new Tooltip(UIUtils.formatPercentage(percentage));

            // Set the tooltip behavior
            tooltip.showDelayProperty().setValue(Duration.ZERO);
            tooltip.hideDelayProperty().setValue(Duration.ZERO);
            tooltip.setStyle("-fx-font-size: 16px;");

            Tooltip.install(node, tooltip);

            // Add a mouse hover animation (scaling effect)
            node.setOnMouseEntered(
                    event -> {
                        ScaleTransition st = new ScaleTransition(Duration.millis(100), node);
                        st.setToX(1.1);
                        st.setToY(1.1);
                        st.play();
                    });

            // Reset the animation when mouse exits
            node.setOnMouseExited(
                    event -> {
                        ScaleTransition st = new ScaleTransition(Duration.millis(100), node);
                        st.setToX(1.0);
                        st.setToY(1.0);
                        st.play();
                    });
        }
    }

    @Override
    protected void layoutChartChildren(
            double top, double left, double contentWidth, double contentHeight) {
        super.layoutChartChildren(top, left, contentWidth, contentHeight);

        addInnerCircleIfNotPresent();
        updateInnerCircleLayout();

        setCenterLabelTextStyle(
                UIUtils.formatCurrency(seriesTotal), "-fx-font-size: 16px; -fx-font-weight: bold;");
    }

    private void addInnerCircleIfNotPresent() {
        if (!getData().isEmpty()) {
            Node pie = getData().get(0).getNode();
            if (pie.getParent() instanceof Pane parent) {
                if (!parent.getChildren().contains(innerCircle)) {
                    parent.getChildren().add(innerCircle);
                }

                if (!parent.getChildren().contains(stackPane)) {
                    parent.getChildren().add(stackPane);
                }
            }
        }

        if (!stackPane.getChildren().contains(centerLabel)) {
            stackPane.getChildren().add(centerLabel);
        }
    }

    private void updateInnerCircleLayout() {
        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double maxX = Double.MIN_VALUE;
        double maxY = Double.MIN_VALUE;

        for (PieChart.Data data : getData()) {
            Node node = data.getNode();

            Bounds bounds = node.getBoundsInParent();

            if (bounds.getMinX() < minX) {
                minX = bounds.getMinX();
            }
            if (bounds.getMinY() < minY) {
                minY = bounds.getMinY();
            }
            if (bounds.getMaxX() > maxX) {
                maxX = bounds.getMaxX();
            }
            if (bounds.getMaxY() > maxY) {
                maxY = bounds.getMaxY();
            }
        }
        // Center the inner circle
        innerCircle.setCenterX(minX + (maxX - minX) / 2);
        innerCircle.setCenterY(minY + (maxY - minY) / 2);
        innerCircle.setRadius((maxX - minX) / 3.5);

        // Center the stack pane
        stackPane.setLayoutX(innerCircle.getCenterX() - stackPane.getWidth() / 2);
        stackPane.setLayoutY(innerCircle.getCenterY() - stackPane.getHeight() / 2);

        // background transparent
        stackPane.setStyle("-fx-background-color: transparent;");
    }

    public void setCenterLabelTextStyle(String text, String style) {
        centerLabel.setText(text);
        centerLabel.setStyle(style);

        centerLabel.setMinWidth(innerCircle.getRadius() * 2);
        centerLabel.setMinHeight(innerCircle.getRadius() * 2);
        centerLabel.setAlignment(javafx.geometry.Pos.CENTER);
    }
}
