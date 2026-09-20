/*
 * Filename: LlmHttpClient.kt
 * Created on: September 20, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.ai.provider

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

internal class LlmHttpClient(
    private val providerName: String,
    private val httpClient: HttpClient = HttpClient.newHttpClient(),
) {
    companion object {
        private val logger: Logger = LoggerFactory.getLogger(LlmHttpClient::class.java)
        private val DEFAULT_TIMEOUT: Duration = Duration.ofSeconds(60)

        internal fun sanitizePath(endpoint: String): String = endpoint.substringBefore("?")
    }

    fun post(
        model: String,
        endpoint: String,
        headers: Map<String, String>,
        body: String,
    ): String {
        val request =
            HttpRequest
                .newBuilder()
                .uri(URI.create(endpoint))
                .timeout(DEFAULT_TIMEOUT)
                .applyHeaders(headers)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build()
        return send("POST", model, endpoint, request)
    }

    fun get(
        model: String,
        endpoint: String,
        headers: Map<String, String>,
    ): String {
        val request =
            HttpRequest
                .newBuilder()
                .uri(URI.create(endpoint))
                .timeout(DEFAULT_TIMEOUT)
                .applyHeaders(headers)
                .GET()
                .build()
        return send("GET", model, endpoint, request)
    }

    private fun HttpRequest.Builder.applyHeaders(headers: Map<String, String>): HttpRequest.Builder {
        headers.forEach { (name, value) -> header(name, value) }
        return this
    }

    private fun send(
        method: String,
        model: String,
        endpoint: String,
        request: HttpRequest,
    ): String {
        val path = sanitizePath(endpoint)
        logger.info("Sending {} request to {} (model={}, path={})", method, providerName, model, path)

        val startNanos = System.nanoTime()
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        val elapsedMs = (System.nanoTime() - startNanos) / 1_000_000

        if (response.statusCode() !in 200..299) {
            logger.warn(
                "Received error response from {} (model={}, path={}, status={}, elapsedMs={})",
                providerName,
                model,
                path,
                response.statusCode(),
                elapsedMs,
            )
            throw LlmProviderException(
                "$providerName request failed with status ${response.statusCode()}: ${response.body()}",
            )
        }

        logger.info(
            "Received response from {} (model={}, path={}, status={}, elapsedMs={})",
            providerName,
            model,
            path,
            response.statusCode(),
            elapsedMs,
        )
        return response.body()
    }
}
