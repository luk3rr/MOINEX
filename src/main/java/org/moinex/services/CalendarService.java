/*
 * Filename: CalendarService.java
 * Created on: March  1, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.services;

import java.time.LocalDateTime;
import java.util.List;
import java.util.logging.Logger;
import lombok.NoArgsConstructor;
import org.moinex.entities.CalendarEvent;
import org.moinex.repositories.CalendarEventRepository;
import org.moinex.util.CalendarEventType;
import org.moinex.util.LoggerConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service class for the Calendar interface
 *
 * Stores the events of the calendar and allows them to be accessed by other
 * classes
 */
@Service
@NoArgsConstructor
public class CalendarService
{
    @Autowired
    private CalendarEventRepository calendarEventRepository;

    private static final Logger logger = LoggerConfig.getLogger();

    /**
     * Adds an event to the calendar
     * @param title The title of the event
     * @param description The description of the event
     * @param date The date of the event
     * @param eventType The type of the event
     * @throws RuntimeException If the title is empty
     */
    @Transactional
    public void addEvent(String            title,
                         String            description,
                         LocalDateTime     date,
                         CalendarEventType eventType)
    {
        // Remove leading and trailing whitespaces
        title = title.strip();

        if (title.isBlank())
        {
            throw new RuntimeException("Title cannot be empty");
        }

        CalendarEvent event = CalendarEvent.builder()
                                  .title(title)
                                  .description(description)
                                  .date(date)
                                  .eventType(eventType)
                                  .build();

        calendarEventRepository.save(event);

        logger.info("Event '" + title + "' added to the calendar");
    }

    /**
     * Deletes an event from the calendar
     * @param eventId The id of the event to be removed
     * @throws RuntimeException If the event with the given id is not found
     */
    @Transactional
    public void deleteEvent(Long eventId)
    {
        CalendarEvent event = calendarEventRepository.findById(eventId).orElseThrow(
            () -> new RuntimeException("Event with id " + eventId + " not found"));

        calendarEventRepository.delete(event);

        logger.info("Event '" + event.getTitle() + "' removed from the calendar");
    }

    /**
     * Updates an event in the calendar
     * @param event The event to be updated
     */
    @Transactional
    public void updateEvent(CalendarEvent event)
    {
        calendarEventRepository.save(event);

        logger.info("Event '" + event.getTitle() + "' updated");
    }

    /**
     * Gets all events in the calendar
     * @return A list with all events in the calendar
     */
    public List<CalendarEvent> getAllEvents()
    {
        return calendarEventRepository.findAll();
    }
}
