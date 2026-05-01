/*
 * Filename: CalendarService.kt (original filename: CalendarService.java)
 * Created on: March  1, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 03/08/2026
 */

package org.moinex.service

import org.moinex.common.constant.TranslationKeys
import org.moinex.common.extension.findByIdOrThrow
import org.moinex.model.CalendarEvent
import org.moinex.model.enums.NotificationType
import org.moinex.repository.CalendarEventRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CalendarService(
    private val calendarEventRepository: CalendarEventRepository,
    private val notificationService: NotificationService,
    private val preferencesService: PreferencesService,
) {
    private val logger = LoggerFactory.getLogger(CalendarService::class.java)

    @Transactional
    fun createEvent(event: CalendarEvent) {
        val newEvent = calendarEventRepository.save(event)

        logger.info("$newEvent created successfully")

        notificationService.send(
            type = NotificationType.SUCCESS,
            title =
                preferencesService.translate(TranslationKeys.CALENDAR_DIALOG_EVENT_CREATED_TITLE),
            message =
                preferencesService.translate(TranslationKeys.CALENDAR_DIALOG_EVENT_CREATED_MESSAGE),
            relatedEntityId = newEvent.id!!,
        )
    }

    @Transactional
    fun deleteEvent(id: Int) {
        val eventFromDatabase = calendarEventRepository.findByIdOrThrow(id)

        calendarEventRepository.delete(eventFromDatabase)

        logger.info("$eventFromDatabase deleted successfully")
    }

    @Transactional
    fun updateEvent(updatedEvent: CalendarEvent) {
        val eventFromDatabase = calendarEventRepository.findByIdOrThrow(updatedEvent.id!!)

        eventFromDatabase.apply {
            title = updatedEvent.title
            description = updatedEvent.description
            date = updatedEvent.date
            eventType = updatedEvent.eventType
        }

        logger.info("$eventFromDatabase updated successfully")
    }

    fun getAllEvents(): List<CalendarEvent> = calendarEventRepository.findAll()
}
