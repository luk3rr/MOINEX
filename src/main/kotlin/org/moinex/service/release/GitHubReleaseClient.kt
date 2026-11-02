/*
 * Filename: GitHubReleaseClient.kt
 * Created on: November 2, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.service.release

import org.json.JSONObject
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

data class LatestRelease(
    val tagName: String,
    val htmlUrl: String,
)

@Component
class GitHubReleaseClient(
    private val httpClient: HttpClient,
) {
    @Autowired
    constructor() : this(
        HttpClient
            .newBuilder()
            .connectTimeout(TIMEOUT)
            .build(),
    )

    private val logger = LoggerFactory.getLogger(GitHubReleaseClient::class.java)

    fun latest(): LatestRelease? =
        runCatching { fetch() }
            .onFailure { logger.debug("Could not check the latest release", it) }
            .getOrNull()

    private fun fetch(): LatestRelease? {
        val request =
            HttpRequest
                .newBuilder()
                .uri(URI.create(LATEST_RELEASE_URL))
                .timeout(TIMEOUT)
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "MOINEX")
                .GET()
                .build()

        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() != HTTP_OK) {
            logger.debug("Latest release check returned status {}", response.statusCode())
            return null
        }

        val json = JSONObject(response.body())
        val tagName = json.optString("tag_name", "")
        val htmlUrl = json.optString("html_url", "")
        if (tagName.isBlank() || !htmlUrl.startsWith(ALLOWED_URL_PREFIX)) {
            logger.debug("Latest release response rejected (tag='{}', url='{}')", tagName, htmlUrl)
            return null
        }
        return LatestRelease(tagName, htmlUrl)
    }

    companion object {
        const val LATEST_RELEASE_URL = "https://api.github.com/repos/luk3rr/MOINEX/releases/latest"
        const val ALLOWED_URL_PREFIX = "https://github.com/luk3rr/MOINEX/"
        val TIMEOUT: Duration = Duration.ofSeconds(10)
        private const val HTTP_OK = 200
    }
}
