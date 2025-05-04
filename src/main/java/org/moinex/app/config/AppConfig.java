package org.moinex.app.config;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@EntityScan(basePackages = "org.moinex.model")
@EnableJpaRepositories(basePackages = "org.moinex.repository")
@SpringBootApplication(scanBasePackages = "org.moinex")
public class AppConfig {}
