/*
 * Filename: Animation.java
 * Created on: October 12, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.util;

import java.util.List;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * Static methods to create animations
 */
public final class Animation
{
    /**
     * Animates a bar chart
     * @param data The data to be animated
     * @param targetValue The target value for the bar
     */
    static public void xyChartAnimation(XYChart.Data<String, Number> data,
                                        Double                       targetValue)
    {
        // Property to store the current value being animated
        DoubleProperty currentValue = new SimpleDoubleProperty(0.0);

        // Create a timeline to animate the value from 0 to targetValue over the
        // specified duration
        Timeline timeline = new Timeline();

        // Define the key frame with a proportional interpolation
        KeyFrame keyFrame = new KeyFrame(
            Duration.seconds(Constants.XYBAR_CHART_ANIMATION_DURATION),
            new KeyValue(currentValue, targetValue, Interpolator.EASE_BOTH));

        // Listener to update the Y value of the bar in each frame
        currentValue.addListener((obs, oldVal, newVal) -> data.setYValue(newVal));

        timeline.getKeyFrames().add(keyFrame);
        timeline.play();
    }

    /**
     * Animates a stacked bar chart
     * @param data The data to be animated
     * @param targetValues The target values for each part of the stacked bar
     */
    static public void stackedXYChartAnimation(List<XYChart.Data<String, Number>> data,
                                               List<Double> targetValues)
    {
        if (data.size() != targetValues.size())
        {
            throw new IllegalArgumentException(
                "Data and targetValues lists must have the same size.");
        }

        for (XYChart.Data<String, Number> item : data)
        {
            item.setYValue(0.0);
        }

        // Increments for each part of the stacked bar
        Double[] increments = new Double[data.size()];

        for (int i = 0; i < data.size(); i++)
        {
            increments[i] =
                targetValues.get(i) / Constants.XYBAR_CHART_ANIMATION_FRAMES;
        }

        // Animation timeline
        Timeline timeline = new Timeline();

        // For each frame, update the value of each part of the stacked bar
        for (int frame = 0; frame < Constants.XYBAR_CHART_ANIMATION_FRAMES; frame++)
        {
            KeyFrame keyFrame = new KeyFrame(
                Duration.seconds(Constants.XYBAR_CHART_ANIMATION_DURATION /
                                 Constants.XYBAR_CHART_ANIMATION_FRAMES * (frame + 1)),
                event -> {
                    Double accumulatedValue = 0.0;

                    // Update the value of each part of the stacked bar
                    for (int i = 0; i < data.size(); i++)
                    {
                        XYChart.Data<String, Number> item = data.get(i);

                        // Accumulate the value of the previous parts
                        accumulatedValue += increments[i];

                        Double newYValue = accumulatedValue;

                        // Limit the value to the target value
                        if (newYValue > targetValues.get(i))
                        {
                            newYValue = targetValues.get(i);
                        }

                        item.setYValue(newYValue);
                    }
                });

            timeline.getKeyFrames().add(keyFrame);
        }

        // Set the final value of each part of the stacked bar
        timeline.setOnFinished(event -> {
            for (int i = 0; i < data.size(); i++)
            {
                XYChart.Data<String, Number> item = data.get(i);
                item.setYValue(targetValues.get(i));
            }
        });

        timeline.play();
    }

    /**
     * Sets the bounds of the Y axis of a bar chart dynamically
     * @param numberAxis The Y axis to be updated
     * @param maxValue The maximum value of the data
     */
    static public void setDynamicYAxisBounds(NumberAxis numberAxis, Double maxValue)
    {
        numberAxis.setAutoRanging(false);
        // Define the lower bound as 0
        numberAxis.setLowerBound(0);

        double upperBound = Math.ceil(maxValue / 10) * 10;
        numberAxis.setUpperBound(upperBound);

        double tickUnit = Math.ceil(upperBound / Constants.XYBAR_CHART_TICKS / 10) * 10;
        numberAxis.setTickUnit(tickUnit);
    }

    /**
     * Applies a fade-in animation to the window
     * @param stage The stage to apply the animation
     */
    public static void applyFadeInAnimation(Stage stage)
    {
        FadeTransition fadeIn =
            new FadeTransition(Duration.seconds(Constants.FADE_IN_ANIMATION_DURATION),
                               stage.getScene().getRoot());
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        fadeIn.play();
    }

    /**
     * Applies a fade-out animation to the window
     * @param stage The stage to apply the animation
     */
    public static void applyFadeOutAnimation(Stage stage)
    {
        FadeTransition fadeOut =
            new FadeTransition(Duration.seconds(Constants.FADE_OUT_ANIMATION_DURATION),
                               stage.getScene().getRoot());
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        fadeOut.play();
    }

    /**
     * Applies a slide-in animation to the window
     * @param stage The stage to apply the animation
     */
    public static void applySlideInAnimation(Stage stage)
    {
        TranslateTransition slideIn = new TranslateTransition(
            Duration.seconds(Constants.SLIDE_ANIMATION_DURATION),
            stage.getScene().getRoot());
        slideIn.setFromX(-stage.getWidth());
        slideIn.setToX(0);
        slideIn.play();
    }
}
