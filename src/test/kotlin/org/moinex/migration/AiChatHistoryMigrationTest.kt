/*
 * Filename: AiChatHistoryMigrationTest.kt
 * Created on: November 6, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.migration

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.shouldBe
import jakarta.persistence.Column
import jakarta.persistence.Table
import org.moinex.model.ai.AiChat
import org.moinex.model.ai.AiChatChange
import org.moinex.model.ai.AiChatChangeSet
import org.moinex.model.ai.AiChatMessage
import org.moinex.repository.ai.AI_CHAT_SEARCH_MESSAGES_SQL
import org.moinex.repository.ai.AI_CHAT_SEARCH_TITLES_SQL
import java.sql.Connection
import java.sql.DriverManager
import kotlin.reflect.KClass
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.jvm.javaField
import kotlin.system.measureTimeMillis

class AiChatHistoryMigrationTest :
    BehaviorSpec({
        val script =
            requireNotNull(
                AiChatHistoryMigrationTest::class.java.getResource("/db/migration/V038__create_ai_chat_history.sql"),
            ).readText()
                .split(";")
                .map { it.trim() }
                .filter { it.isNotEmpty() }

        fun Connection.columns(table: String): List<String> =
            createStatement().use { statement ->
                statement.executeQuery("PRAGMA table_info($table)").use { rs ->
                    buildList { while (rs.next()) add(rs.getString("name")) }
                }
            }

        fun Connection.tables(): List<String> =
            createStatement().use { statement ->
                statement.executeQuery("SELECT name FROM sqlite_master WHERE type = 'table'").use { rs ->
                    buildList { while (rs.next()) add(rs.getString(1)) }
                }
            }

        fun mappedColumns(type: KClass<*>): List<String> =
            type.declaredMemberProperties.mapNotNull { it.javaField?.getAnnotation(Column::class.java)?.name }

        fun Connection.migrated(): Connection {
            createStatement().use { statement -> script.forEach { statement.execute(it) } }
            return this
        }

        fun Connection.fillWith(
            chats: Int,
            messagesPerChat: Int,
        ) {
            autoCommit = false
            prepareStatement("INSERT INTO ai_chat (id, title, created_at, updated_at) VALUES (?, ?, ?, ?)").use { ps ->
                for (chat in 1..chats) {
                    ps.setInt(1, chat)
                    ps.setString(2, "Conversa $chat")
                    ps.setString(3, "2026-11-01T10:00:00")
                    ps.setString(4, "2026-11-0${1 + chat % 5}T10:00:00")
                    ps.addBatch()
                }
                ps.executeBatch()
            }
            prepareStatement(
                "INSERT INTO ai_chat_message (chat_id, position, role, content, display_text, created_at) VALUES (?, ?, ?, ?, ?, ?)",
            ).use { ps ->
                for (chat in 1..chats) {
                    for (position in 0 until messagesPerChat) {
                        ps.setInt(1, chat)
                        ps.setInt(2, position)
                        ps.setString(3, if (position % 2 == 0) "USER" else "ASSISTANT")
                        ps.setString(
                            4,
                            "mensagem $position da conversa $chat sobre gastos e categorias do mês " +
                                "texto ".repeat(30),
                        )
                        ps.setString(5, null)
                        ps.setString(6, "2026-11-01T10:00:00")
                        ps.addBatch()
                    }
                }
                ps.executeBatch()
            }
            commit()
            autoCommit = true
        }

        Given("an empty database") {
            DriverManager.getConnection("jdbc:sqlite::memory:").use { connection ->
                When("the migration runs") {
                    connection.migrated()

                    Then("the four chat history tables exist") {
                        connection.tables() shouldContainAll
                            listOf("ai_chat", "ai_chat_message", "ai_chat_change_set", "ai_chat_change")
                    }

                    Then("each entity maps only columns that the tables have") {
                        listOf(
                            AiChat::class to "ai_chat",
                            AiChatMessage::class to "ai_chat_message",
                            AiChatChangeSet::class to "ai_chat_change_set",
                            AiChatChange::class to "ai_chat_change",
                        ).forEach { (entity, table) ->
                            entity.findAnnotation<Table>()!!.name shouldBe table
                            connection.columns(table) shouldContainAll mappedColumns(entity)
                        }
                    }

                    Then("the migration can run again without error") {
                        connection.migrated()
                    }
                }
            }
        }

        Given("5000 stored messages") {
            DriverManager.getConnection("jdbc:sqlite::memory:").use { connection ->
                connection.migrated().fillWith(chats = 25, messagesPerChat = 200)

                fun search(term: String): Int {
                    val sql = AI_CHAT_SEARCH_MESSAGES_SQL.replace(":pattern", "?").replace(":limit", "?")
                    return connection.prepareStatement(sql).use { ps ->
                        ps.setString(1, "%$term%")
                        ps.setString(2, "%$term%")
                        ps.setInt(3, 50)
                        ps.executeQuery().use { rs ->
                            var rows = 0
                            while (rs.next()) rows++
                            rows
                        }
                    }
                }

                When("a text found in many messages is searched") {
                    var found = 0
                    val millis = measureTimeMillis { found = search("categorias") }

                    Then("the search returns the limit in under a second") {
                        found shouldBe 50
                        millis shouldBeLessThan 1000L
                    }
                }

                When("a text found in none is searched") {
                    var found = -1
                    val millis = measureTimeMillis { found = search("inexistente-xyz") }

                    Then("nothing is returned, in under a second") {
                        found shouldBe 0
                        millis shouldBeLessThan 1000L
                    }
                }

                When("a title is searched") {
                    val sql = AI_CHAT_SEARCH_TITLES_SQL.replace(":pattern", "?").replace(":limit", "?")
                    val titles =
                        connection.prepareStatement(sql).use { ps ->
                            ps.setString(1, "%Conversa 1%")
                            ps.setInt(2, 50)
                            ps.executeQuery().use { rs ->
                                buildList { while (rs.next()) add(rs.getString(2)) }
                            }
                        }

                    Then("the matching titles come back") {
                        titles.contains("Conversa 1") shouldBe true
                        titles.contains("Conversa 12") shouldBe true
                    }
                }

                When("a conversation of 200 messages is opened") {
                    var rows = 0
                    val millis =
                        measureTimeMillis {
                            connection
                                .prepareStatement(
                                    "SELECT * FROM ai_chat_message WHERE chat_id = ? ORDER BY position",
                                ).use { ps ->
                                    ps.setInt(1, 7)
                                    ps.executeQuery().use { rs -> while (rs.next()) rows++ }
                                }
                        }

                    Then("all messages are read in under 500 ms") {
                        rows shouldBe 200
                        millis shouldBeLessThan 500L
                    }
                }
            }
        }
    })
