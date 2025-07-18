/*
 * Filename: CalendarService.java
 * Created on: March  1, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.service;

import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.List;
import lombok.NoArgsConstructor;
import org.moinex.model.CalendarEvent;
import org.moinex.repository.CalendarEventRepository;
import org.moinex.util.enums.CalendarEventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service class for the Calendar interface
 * <p>
 * Stores the events of the calendar and allows them to be accessed by other
 * classes
 */
@Service
@NoArgsConstructor
public class CalendarService {
    private CalendarEventRepository calendarEventRepository;

    private static final Logger logger = LoggerFactory.getLogger(CalendarService.class);

    @Autowired
    public CalendarService(CalendarEventRepository calendarEventRepository) {
        this.calendarEventRepository = calendarEventRepository;
    }

    /**
     * Adds an event to the calendar
     * @param title The title of the event
     * @param description The description of the event
     * @param date The date of the event
     * @param eventType The type of the event
     * @throws IllegalArgumentException If the title is empty
     */
    @Transactional
    public void addEvent(
            String title, String description, LocalDateTime date, CalendarEventType eventType) {
        // Remove leading and trailing whitespaces
        title = title.strip();

        if (title.isBlank()) {
            throw new IllegalArgumentException("Title cannot be empty");
        }

        CalendarEvent event =
                CalendarEvent.builder()
                        .title(title)
                        .description(description)
                        .date(date)
                        .eventType(eventType)
                        .build();

        calendarEventRepository.save(event);

        logger.info("Event '{}' added to the calendar", title);
    }

    /**
     * Deletes an event from the calendar
     * @param eventId The id of the event to be removed
     * @throws EntityNotFoundException If the event is not found
     */
    @Transactional
    public void deleteEvent(Integer eventId) {
        CalendarEvent event =
                calendarEventRepository
                        .findById(eventId)
                        .orElseThrow(
                                () ->
                                        new EntityNotFoundException(
                                                "Event with id " + eventId + " not found"));

        calendarEventRepository.delete(event);

        logger.info("Event '{}' removed from the calendar", event.getTitle());
    }

    /**
     * Updates an event in the calendar
     * @param event The event to be updated
     */
    @Transactional
    public void updateEvent(CalendarEvent event) {
        calendarEventRepository.save(event);

        logger.info("Event '{}' updated", event.getTitle());
    }

    /**
     * Gets all events in the calendar
     * @return A list with all events in the calendar
     */
    public List<CalendarEvent> getAllEvents() {
        return calendarEventRepository.findAll();
    }
}
