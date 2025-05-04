package org.moinex.app;

import org.moinex.app.config.AppConfig;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

public class SpringApp {
    private SpringApp() {
        // Prevent instantiation
    }

    public static ConfigurableApplicationContext start(String[] args) {
        return new SpringApplicationBuilder().sources(AppConfig.class).run(args);
    }
}
