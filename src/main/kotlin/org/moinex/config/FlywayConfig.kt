/*
 * Filename: FlywayConfig.kt (original filename: FlywayConfig.java)
 * Created on: March  5, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrated to Kotlin on 18/03/2026
 */

package org.moinex.config

import org.flywaydb.core.Flyway
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import javax.sql.DataSource

@Configuration
class FlywayConfig {
    @Bean
    fun flyway(dataSource: DataSource): Flyway {
        val flyway =
            Flyway
                .configure()
                .dataSource(dataSource)
                .baselineOnMigrate(true)
                .mixed(true)
                .load()
        flyway.migrate()
        return flyway
    }
}
