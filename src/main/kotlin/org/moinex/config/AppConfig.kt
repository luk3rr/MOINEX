/*
 * Filename: AppConfig.kt (original filename: AppConfig.java)
 * Created on: [Original date]
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrated to Kotlin on 18/03/2026
 */

package org.moinex.config

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

@EntityScan(basePackages = ["org.moinex.model"])
@EnableJpaRepositories(basePackages = ["org.moinex.repository"])
@SpringBootApplication(scanBasePackages = ["org.moinex"])
class AppConfig
