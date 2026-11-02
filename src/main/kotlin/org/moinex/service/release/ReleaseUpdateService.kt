/*
 * Filename: ReleaseUpdateService.kt
 * Created on: November 2, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.service.release

import org.moinex.common.constant.TranslationKeys
import org.moinex.common.util.AppVersion
import org.moinex.model.SemanticVersion
import org.moinex.model.enums.NotificationType
import org.moinex.service.NotificationService
import org.moinex.service.PreferencesService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class ReleaseUpdateService(
    private val releaseClient: GitHubReleaseClient,
    private val notificationService: NotificationService,
    private val preferencesService: PreferencesService,
    private val installedVersion: () -> SemanticVersion?,
) {
    @Autowired
    constructor(
        releaseClient: GitHubReleaseClient,
        notificationService: NotificationService,
        preferencesService: PreferencesService,
    ) : this(releaseClient, notificationService, preferencesService, { AppVersion.installed() })

    private val logger = LoggerFactory.getLogger(ReleaseUpdateService::class.java)

    /**
     * Checks GitHub for a release newer than the installed one and notifies the user once per version.
     *
     * @return true when a notification was sent
     */
    fun checkForUpdate(): Boolean {
        if (!preferencesService.updateCheckEnabled) {
            logger.debug("Update check disabled by the user")
            return false
        }

        val installed = installedVersion() ?: return false
        val release = releaseClient.latest() ?: return false
        val remote = SemanticVersion.parse(release.tagName) ?: return false

        if (remote <= installed) return false
        if (preferencesService.lastNotifiedVersion == remote.toString()) return false

        notificationService.send(
            NotificationType.INFO,
            preferencesService.translate(TranslationKeys.NOTIFICATION_UPDATE_TITLE),
            preferencesService
                .translate(TranslationKeys.NOTIFICATION_UPDATE_MESSAGE)
                .replace("{0}", remote.toString())
                .replace("{1}", installed.toString()),
            actionUrl = release.htmlUrl,
        )
        preferencesService.lastNotifiedVersion = remote.toString()
        logger.info("Notified about new release {}", remote)
        return true
    }
}
