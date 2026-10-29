/*
 * Filename: FlywayConfig.kt (original filename: FlywayConfig.java)
 * Created on: March  5, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrated to Kotlin on 18/03/2026
 */

package org.moinex.config

import org.flywaydb.core.Flyway
import org.springframework.beans.factory.config.BeanFactoryPostProcessor
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ClassPathResource
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator
import javax.sql.DataSource

@Configuration
class FlywayConfig {
    companion object {
        private const val INITIAL_DATA_SCRIPT = "data.sql"
        private const val FLYWAY_BEAN_NAME = "flyway"
        private const val ENTITY_MANAGER_FACTORY_BEAN_NAME = "entityManagerFactory"

        @JvmStatic
        @Bean
        fun entityManagerFactoryDependsOnFlyway(): BeanFactoryPostProcessor =
            BeanFactoryPostProcessor { beanFactory ->
                val definition = beanFactory.getBeanDefinition(ENTITY_MANAGER_FACTORY_BEAN_NAME)
                val dependencies = definition.dependsOn.orEmpty().toList() + FLYWAY_BEAN_NAME
                definition.setDependsOn(*dependencies.toTypedArray())
            }
    }

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
        ResourceDatabasePopulator(ClassPathResource(INITIAL_DATA_SCRIPT)).execute(dataSource)
        return flyway
    }
}
