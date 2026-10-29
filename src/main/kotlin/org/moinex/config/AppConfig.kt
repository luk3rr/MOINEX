/*
 * Filename: AppConfig.kt (original filename: AppConfig.java)
 * Created on: [Original date]
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrated to Kotlin on 18/03/2026
 */

package org.moinex.config

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.persistence.autoconfigure.EntityScan
import org.springframework.context.annotation.Bean
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import java.time.Clock

@EntityScan(basePackages = ["org.moinex.model"])
@EnableJpaRepositories(basePackages = ["org.moinex.repository"])
@EnableConfigurationProperties(AiPromptProperties::class)
@SpringBootApplication(scanBasePackages = ["org.moinex"])
class AppConfig {
    @Bean
    fun clock(): Clock = Clock.systemDefaultZone()
}
