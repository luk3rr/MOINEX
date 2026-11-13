/*
 * Filename: ImportExecutor.kt
 * Created on: November 12, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.dataexchange.imports

import org.moinex.dataexchange.spec.ApplyContext
import org.moinex.dataexchange.spec.EntityCatalog
import org.moinex.dataexchange.spec.ProgressSink
import org.moinex.service.NotificationService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate

@Service
class ImportExecutor(
    private val catalog: EntityCatalog,
    transactionManager: PlatformTransactionManager,
    private val notificationService: NotificationService,
) {
    private val logger = LoggerFactory.getLogger(ImportExecutor::class.java)
    private val template = TransactionTemplate(transactionManager)

    fun execute(
        plan: ImportPlan,
        progress: ProgressSink = ProgressSink.NONE,
    ): ImportResult {
        if (plan.hasErrors) throw ImportExecutionException("The plan has errors")
        for (entity in plan.entities) {
            val spec = catalog.find(entity.id) ?: throw ImportExecutionException("Unknown entity ${entity.id}")
            if (spec.existingKeys().values.sum() != entity.existingSnapshot) {
                throw ImportExecutionException("The data changed after the preview")
            }
        }
        return try {
            notificationService.withoutNotifications {
                template.execute {
                    val context = ApplyContext()
                    val added = linkedMapOf<String, Int>()
                    val ignored = linkedMapOf<String, Int>()
                    val rowsByEntity = plan.entities.associate { it.id to it.rows }
                    plan.entities.forEach { catalog.find(it.id)?.prepare(rowsByEntity, context) }
                    for (entity in plan.entities) {
                        val spec = catalog.find(entity.id)!!
                        var done = 0L
                        for (row in spec.applyOrder(entity.rows)) {
                            spec.apply(row, context)
                            done++
                            if (done % PROGRESS_STEP == 0L) progress.report(entity.id, done, entity.rows.size.toLong())
                        }
                        progress.report(entity.id, done, entity.rows.size.toLong())
                        added[entity.id] = entity.rows.size
                        ignored[entity.id] = entity.duplicates
                    }
                    ImportResult(added, ignored)
                }!!
            }
        } catch (e: ImportExecutionException) {
            throw e
        } catch (e: Exception) {
            logger.error("Import failed and was rolled back: {}", e.javaClass.simpleName)
            throw ImportExecutionException("The import failed and nothing was saved: ${e.message}", e)
        }.also { logger.info("Imported {} rows", it.added.values.sum()) }
    }

    private companion object {
        const val PROGRESS_STEP = 200L
    }
}
