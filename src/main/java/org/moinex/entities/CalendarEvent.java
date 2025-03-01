/*
 * Filename: CalendarEvent.java
 * Created on: March  1, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import org.moinex.util.CalendarEventType;
import org.moinex.util.Constants;

/**
 * Represents an event in the calendar
 */
@Entity
@Table(name = "calendar_event")
public class CalendarEvent
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "date", nullable = false)
    private String date;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description", nullable = true)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private CalendarEventType eventType;

    /**
     * Default constructor for JPA
     */
    public CalendarEvent() { }

    /**
     * Constructor for CalendarEvent
     * @param date The date of the event
     * @param title The title of the event
     * @param description A description of the event
     * @param eventType The type of the event
     */
    public CalendarEvent(LocalDateTime     date,
                         String            title,
                         String            description,
                         CalendarEventType eventType)
    {
        this.date        = date.format(Constants.DB_DATE_FORMATTER);
        this.title       = title;
        this.description = description;
        this.eventType   = eventType;
    }

    /**
     * Get the id of the event
     * @return The id of the event
     */
    public Long GetId()
    {
        return id;
    }

    /**
     * Get the date of the event
     * @return The date of the event
     */
    public LocalDateTime GetDate()
    {
        return LocalDateTime.parse(date, Constants.DB_DATE_FORMATTER);
    }

    /**
     * Get the title of the event
     * @return The title of the event
     */
    public String GetTitle()
    {
        return title;
    }

    /**
     * Get the description of the event
     * @return The description of the event
     */
    public String GetDescription()
    {
        return description;
    }

    /**
     * Get the type of the event
     * @return The type of the event
     */
    public CalendarEventType GetEventType()
    {
        return eventType;
    }

    /**
     * Set the date of the event
     * @param date The date of the event
     */
    public void SetDate(LocalDateTime date)
    {
        this.date = date.format(Constants.DB_DATE_FORMATTER);
    }

    /**
     * Set the title of the event
     * @param title The title of the event
     */
    public void SetTitle(String title)
    {
        this.title = title;
    }

    /**
     * Set the description of the event
     * @param description The description of the event
     */
    public void SetDescription(String description)
    {
        this.description = description;
    }

    /**
     * Set the type of the event
     * @param eventType The type of the event
     */
    public void SetEventType(CalendarEventType eventType)
    {
        this.eventType = eventType;
    }
}
