/*
 * Filename: ExportService.kt
 * Created on: November 12, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.dataexchange.export

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import org.moinex.common.util.AppVersion
import org.moinex.dataexchange.spec.EntityCatalog
import org.moinex.dataexchange.spec.ProgressSink
import org.moinex.dataexchange.tabular.ExportFormat
import org.moinex.dataexchange.tabular.TabularFiles
import org.moinex.dataexchange.tabular.TabularLayout
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant

data class ExportResult(
    val rows: Map<String, Long>,
)

@Service
class ExportService(
    private val catalog: EntityCatalog,
) {
    private val logger = LoggerFactory.getLogger(ExportService::class.java)

    fun counts(): Map<String, Long> = catalog.ordered().associate { it.id to it.count() }

    suspend fun export(
        selection: Set<String>,
        format: ExportFormat,
        target: Path,
        progress: ProgressSink = ProgressSink.NONE,
    ): ExportResult {
        val specs = catalog.ordered().filter { it.id in selection }
        require(specs.isNotEmpty()) { "Nothing selected to export" }
        val written = mutableMapOf<String, Long>()
        try {
            Files.newOutputStream(target).use { output ->
                TabularFiles.writer(format, output).use { writer ->
                    writer.writeMeta(
                        linkedMapOf(
                            TabularLayout.META_APP to TabularLayout.META_APP_VALUE,
                            TabularLayout.META_LAYOUT_VERSION to TabularLayout.CURRENT_LAYOUT_VERSION.toString(),
                            TabularLayout.META_EXPORTED_AT to Instant.now().toString(),
                            TabularLayout.META_APP_VERSION to AppVersion.installedText(),
                            TabularLayout.META_ENTITIES to specs.joinToString(",") { it.id },
                        ),
                    )
                    for (spec in specs) {
                        currentCoroutineContext().ensureActive()
                        val total = spec.count()
                        var done = 0L
                        progress.report(spec.id, 0, total)
                        writer.writeSheet(spec.id, spec.columns, spec.export()) {
                            done++
                            if (done % PROGRESS_STEP == 0L) progress.report(spec.id, done, total)
                        }
                        progress.report(spec.id, done, total)
                        written[spec.id] = done
                    }
                }
            }
        } catch (e: Throwable) {
            Files.deleteIfExists(target)
            throw e
        }
        logger.info("Exported {} entities, {} rows", written.size, written.values.sum())
        return ExportResult(written)
    }

    fun exportTemplate(
        format: ExportFormat,
        target: Path,
    ) {
        try {
            Files.newOutputStream(target).use { output ->
                TabularFiles.writer(format, output).use { writer ->
                    val specs = catalog.ordered()
                    writer.writeMeta(
                        linkedMapOf(
                            TabularLayout.META_APP to TabularLayout.META_APP_VALUE,
                            TabularLayout.META_LAYOUT_VERSION to TabularLayout.CURRENT_LAYOUT_VERSION.toString(),
                            TabularLayout.META_EXPORTED_AT to Instant.now().toString(),
                            TabularLayout.META_APP_VERSION to AppVersion.installedText(),
                            TabularLayout.META_ENTITIES to specs.joinToString(",") { it.id },
                        ),
                    )
                    specs.forEach { writer.writeSheet(it.id, it.columns, emptySequence()) }
                }
            }
        } catch (e: Throwable) {
            Files.deleteIfExists(target)
            throw e
        }
        logger.info("Saved the empty template")
    }

    private companion object {
        const val PROGRESS_STEP = 200L
    }
}
