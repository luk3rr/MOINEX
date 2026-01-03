/*
 * Filename: CircularProgressBar.java
 * Created on: December 15, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.chart;

import javafx.geometry.VPos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcType;
import javafx.scene.text.TextAlignment;
import org.moinex.service.I18nService;
import org.moinex.util.UIUtils;

public class CircularProgressBar extends Canvas {
    private final Color progressColor;
    private final Color backgroundColor;
    private final Color fontColor;
    private final Double progressWidth;
    private I18nService i18nService;

    public CircularProgressBar(Double radius, Double progressWidth) {
        super(radius, radius);

        this.progressWidth = progressWidth;

        final GraphicsContext gc = getGraphicsContext2D();

        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.CENTER);

        // Set default colors
        progressColor = Color.web("#3498db");
        backgroundColor = Color.web("#D3D3D3");
        fontColor = Color.web("#000000");
    }

    public void setI18nService(I18nService i18nService) {
        this.i18nService = i18nService;
    }

    public void draw(Double percent) {
        percent = Math.clamp(percent, 0, 100);

        final GraphicsContext gc = getGraphicsContext2D();

        gc.clearRect(0, 0, getWidth(), getHeight());

        // Draw the background circle
        gc.setStroke(backgroundColor);
        gc.setLineWidth(progressWidth);
        gc.strokeArc(
                progressWidth / 2,
                progressWidth / 2,
                getWidth() - progressWidth,
                getHeight() - progressWidth,
                0,
                360,
                ArcType.OPEN);

        // Draw the progress circle
        gc.setStroke(progressColor);
        gc.setLineWidth(progressWidth);
        gc.strokeArc(
                progressWidth / 2,
                progressWidth / 2,
                getWidth() - progressWidth,
                getHeight() - progressWidth,
                90,
                -(percent / 100 * 360), // Negative angle to draw the circle clockwise
                ArcType.OPEN);

        gc.setFill(fontColor);
        String formattedPercent =
                i18nService != null
                        ? UIUtils.formatPercentage(percent, i18nService)
                        : String.format("%.1f %%", percent);
        gc.fillText(formattedPercent, getWidth() / 2, getHeight() / 2);
    }
}
