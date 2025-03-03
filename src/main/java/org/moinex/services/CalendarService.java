/*
 * Filename: CalendarService.java
 * Created on: March  1, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.services;

import java.time.LocalDateTime;
import java.util.List;
import java.util.logging.Logger;
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
public class CalendarService
{
    @Autowired
    private CalendarEventRepository calendarEventRepository;

    private static final Logger logger = LoggerConfig.GetLogger();

    public CalendarService() { }

    /**
     * Adds an event to the calendar
     * @param title The title of the event
     * @param description The description of the event
     * @param date The date of the event
     * @param eventType The type of the event
     * @throws RuntimeException If the title is empty
     */
    @Transactional
    public void AddEvent(String            title,
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

        CalendarEvent event = new CalendarEvent(date, title, description, eventType);
        calendarEventRepository.save(event);

        logger.info("Event '" + title + "' added to the calendar");
    }

    /**
     * Removes an event from the calendar
     * @param eventId The id of the event to be removed
     * @throws RuntimeException If the event with the given id is not found
     */
    @Transactional
    public void RemoveEvent(Long eventId)
    {
        CalendarEvent event = calendarEventRepository.findById(eventId).orElseThrow(
            () -> new RuntimeException("Event with id " + eventId + " not found"));

        calendarEventRepository.delete(event);

        logger.info("Event '" + event.GetTitle() + "' removed from the calendar");
    }

    /**
     * Updates an event in the calendar
     * @param event The event to be updated
     */
    @Transactional
    public void UpdateEvent(CalendarEvent event)
    {
        calendarEventRepository.save(event);

        logger.info("Event '" + event.GetTitle() + "' updated");
    }

    /**
     * Gets all events in the calendar
     * @return A list with all events in the calendar
     */
    public List<CalendarEvent> GetAllEvents()
    {
        return calendarEventRepository.findAll();
    }
}
