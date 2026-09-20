package org.moinex.ai.credentials

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.moinex.model.enums.LlmProviderType
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createTempDirectory

/**
 * Note: this spec intentionally does NOT use beforeTest/afterTest to reset [AiCredentialStore] -
 * Kotest fires those hooks before/after every TestCase, including nested When/Then containers,
 * which would wipe out state set up in an enclosing Given before its own nested tests run. Each
 * Given block instead creates and owns its own isolated temp directory.
 */
class AiCredentialStoreTest :
    BehaviorSpec({
        val tempDirs = mutableListOf<Path>()

        fun newStore(): AiCredentialStore {
            val dir = createTempDirectory("moinex-ai-credentials-test")
            tempDirs.add(dir)
            return AiCredentialStore(dir)
        }

        afterSpec {
            tempDirs.forEach { it.toFile().deleteRecursively() }
        }

        Given("no API key has been saved for a provider") {
            val store = newStore()

            When("checking whether a key is configured") {
                Then("hasApiKey is false and getApiKey is null") {
                    store.hasApiKey(LlmProviderType.OPENAI) shouldBe false
                    store.getApiKey(LlmProviderType.OPENAI).shouldBeNull()
                }
            }
        }

        Given("an API key saved for OpenAI") {
            val store = newStore()
            store.saveApiKey(LlmProviderType.OPENAI, "sk-test-123")

            When("reading it back") {
                Then("the exact key is returned") {
                    store.getApiKey(LlmProviderType.OPENAI) shouldBe "sk-test-123"
                    store.hasApiKey(LlmProviderType.OPENAI) shouldBe true
                }
            }

            When("inspecting the credentials file on disk") {
                Then("the plaintext key never appears in it") {
                    val credentialsFile = tempDirs.last().resolve("credentials.enc")
                    val raw = Files.readAllBytes(credentialsFile)
                    val rawAsLatin1 = String(raw, Charsets.ISO_8859_1)
                    rawAsLatin1.contains("sk-test-123") shouldBe false
                }
            }

            When("saving a key for a different provider") {
                store.saveApiKey(LlmProviderType.ANTHROPIC, "anthropic-key")

                Then("both provider keys coexist independently") {
                    store.getApiKey(LlmProviderType.OPENAI) shouldBe "sk-test-123"
                    store.getApiKey(LlmProviderType.ANTHROPIC) shouldBe "anthropic-key"
                }
            }

            When("clearing the OpenAI key") {
                store.clearApiKey(LlmProviderType.OPENAI)

                Then("it is no longer available") {
                    store.getApiKey(LlmProviderType.OPENAI).shouldBeNull()
                    store.hasApiKey(LlmProviderType.OPENAI) shouldBe false
                }
            }

            When("overwriting it with a new value") {
                store.saveApiKey(LlmProviderType.OPENAI, "sk-test-456")

                Then("the newest value wins") {
                    store.getApiKey(LlmProviderType.OPENAI) shouldBe "sk-test-456"
                }
            }
        }

        Given("a fresh store pointed at the same directory as one that already saved a key") {
            val store = newStore()
            store.saveApiKey(LlmProviderType.GEMINI, "gemini-key")
            val reopened = AiCredentialStore(tempDirs.last())

            When("reading the key from the new instance") {
                Then("it reuses the persisted encryption key and decrypts successfully") {
                    reopened.getApiKey(LlmProviderType.GEMINI) shouldBe "gemini-key"
                }
            }
        }
    })
