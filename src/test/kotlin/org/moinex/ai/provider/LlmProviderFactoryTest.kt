package org.moinex.ai.provider

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import org.moinex.ai.credentials.AiCredentialStore
import org.moinex.model.enums.LlmProviderType

class LlmProviderFactoryTest :
    BehaviorSpec({
        val credentialStore = mockk<AiCredentialStore>()
        val factory = LlmProviderFactory(credentialStore)

        afterContainer { clearAllMocks() }

        Given("buildProvider is called for each supported provider type") {
            When("building OpenAI, Anthropic, Gemini and OpenRouter providers") {
                Then("each returns an instance of the matching provider class, carrying the right type") {
                    factory.buildProvider(LlmProviderType.OPENAI, "key").type shouldBe LlmProviderType.OPENAI
                    factory.buildProvider(LlmProviderType.ANTHROPIC, "key").type shouldBe LlmProviderType.ANTHROPIC
                    factory.buildProvider(LlmProviderType.GEMINI, "key").type shouldBe LlmProviderType.GEMINI
                    factory.buildProvider(LlmProviderType.OPENROUTER, "key").type shouldBe LlmProviderType.OPENROUTER
                }
            }
        }

        Given("no provider has been selected in Settings") {
            every { credentialStore.selectedProvider } returns null

            When("resolving the current provider") {
                Then("currentProvider returns null without looking up any API key") {
                    factory.currentProvider().shouldBeNull()
                }
            }
        }

        Given("OpenRouter is selected but no API key was saved for it") {
            every { credentialStore.selectedProvider } returns LlmProviderType.OPENROUTER
            every { credentialStore.getApiKey(LlmProviderType.OPENROUTER) } returns null

            When("resolving the current provider") {
                Then("currentProvider returns null") {
                    factory.currentProvider().shouldBeNull()
                }
            }
        }

        Given("OpenRouter is selected and has a saved API key, no custom model") {
            every { credentialStore.selectedProvider } returns LlmProviderType.OPENROUTER
            every { credentialStore.getApiKey(LlmProviderType.OPENROUTER) } returns "or-key-123"
            every { credentialStore.getModel(LlmProviderType.OPENROUTER) } returns null

            When("resolving the current provider") {
                val provider = factory.currentProvider()

                Then("it returns a configured OpenRouterProvider") {
                    provider.shouldBeInstanceOf<OpenRouterProvider>()
                    provider?.type shouldBe LlmProviderType.OPENROUTER
                }
            }
        }

        Given("a blank or absent model override") {
            When("building a Gemini provider") {
                Then("it falls back to Gemini's own default instead of an empty model id") {
                    (
                        factory.buildProvider(
                            LlmProviderType.GEMINI,
                            "key",
                            model = null,
                        ) as GeminiProvider
                    ).model shouldBe
                        GeminiProvider.DEFAULT_MODEL
                    (
                        factory.buildProvider(
                            LlmProviderType.GEMINI,
                            "key",
                            model = "  ",
                        ) as GeminiProvider
                    ).model shouldBe
                        GeminiProvider.DEFAULT_MODEL
                }
            }
        }

        Given("a custom model id stored for the selected provider") {
            When("building a Gemini provider with that override") {
                Then("the override wins over the built-in default") {
                    (
                        factory.buildProvider(
                            LlmProviderType.GEMINI,
                            "key",
                            model = "gemini-3.6-pro",
                        ) as GeminiProvider
                    ).model shouldBe
                        "gemini-3.6-pro"
                }
            }
        }
    })
