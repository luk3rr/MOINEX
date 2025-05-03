/*
 * Filename: FlywayConfig.java
 * Created on: March  5, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.app;

import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FlywayConfig {
    @Bean
    public Flyway flyway(DataSource dataSource) {
        Flyway flyway = Flyway.configure().dataSource(dataSource).baselineOnMigrate(true).load();
        flyway.migrate();
        return flyway;
    }
}
