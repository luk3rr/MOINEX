/*
 * Filename: CalendarService.java
 * Created on: March  1, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.services;

import java.util.List;
import java.util.logging.Logger;
import org.moinex.entities.CalendarEvent;
import org.moinex.repositories.CalendarEventRepository;
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

    @Transactional
    public void AddEvent(CalendarEvent event)
    { }

    @Transactional
    public void RemoveEvent(CalendarEvent event)
    { }

    @Transactional
    public void UpdateEvent(CalendarEvent event)
    { }

    public List<CalendarEvent> GetAllEvents()
    {
        return calendarEventRepository.findAll();
    }
}
