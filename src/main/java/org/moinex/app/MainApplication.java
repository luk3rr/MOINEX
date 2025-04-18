/*
 * Filename: MainApplication.java
 * Created on: September  6, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.app;

import javafx.application.Application;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Main application class
 */
@EntityScan(basePackages = "org.moinex.model")
@EnableJpaRepositories(basePackages = "org.moinex.repository")
@SpringBootApplication(scanBasePackages = "org.moinex")
public class MainApplication
{
    public static void main(String[] args)
    {
        Application.launch(JavaFXApp.class, args);
    }
}
