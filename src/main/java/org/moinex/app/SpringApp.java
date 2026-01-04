package org.moinex.app;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.moinex.app.config.AppConfig;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

public class SpringApp {
    private SpringApp() {
        // Prevent instantiation
    }

    public static ConfigurableApplicationContext start(String[] args) {
        createApplicationDirectories();

        return new SpringApplicationBuilder().sources(AppConfig.class).run(args);
    }

    private static void createApplicationDirectories() {
        String userHome = System.getProperty("user.home");
        Path moinexDir = Paths.get(userHome, ".moinex");
        Path dataDir = Paths.get(userHome, ".moinex", "data");

        try {
            if (!Files.exists(moinexDir)) {
                Files.createDirectories(moinexDir);
            }
            if (!Files.exists(dataDir)) {
                Files.createDirectories(dataDir);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to create application directories", e);
        }
    }
}
