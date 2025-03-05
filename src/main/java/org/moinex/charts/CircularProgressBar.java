/*
 * Filename: CircularProgressBar.java
 * Created on: December 15, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.charts;

import javafx.geometry.VPos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcType;
import javafx.scene.text.TextAlignment;
import org.moinex.util.UIUtils;

public class CircularProgressBar extends Canvas
{
    private Color  progressColor;
    private Color  backgroundColor;
    private Color  fontColor;
    private Double progressWidth;

    public CircularProgressBar(Double radius, Double progressWidth)
    {
        super(radius, radius);

        this.progressWidth = progressWidth;

        final GraphicsContext gc = getGraphicsContext2D();

        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.CENTER);

        // Set default colors
        progressColor   = Color.web("#3498db");
        backgroundColor = Color.web("#D3D3D3");
        fontColor       = Color.web("#000000");
    }

    public void draw(Double percent)
    {
        percent = Math.max(Math.min(percent, 100), 0);

        final GraphicsContext gc = getGraphicsContext2D();

        gc.clearRect(0, 0, getWidth(), getHeight());

        // Draw the background circle
        gc.setStroke(backgroundColor);
        gc.setLineWidth(progressWidth);
        gc.strokeArc(progressWidth / 2,
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
        gc.fillText(UIUtils.formatPercentage(percent), getWidth() / 2, getHeight() / 2);
    }
}
