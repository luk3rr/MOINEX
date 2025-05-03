/*
 * Filename: CalendarEvent.java
 * Created on: March  1, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.moinex.util.Constants;
import org.moinex.util.enums.CalendarEventType;

/**
 * Represents an event in the calendar
 */
@Entity
@Table(name = "calendar_event")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class CalendarEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    @Setter(AccessLevel.NONE)
    private Long id;

    @Column(name = "date", nullable = false)
    private String date;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private CalendarEventType eventType;

    public abstract static class CalendarEventBuilder<
            C extends CalendarEvent, B extends CalendarEventBuilder<C, B>> {
        public B date(LocalDateTime date) {
            this.date = date.format(Constants.DB_DATE_FORMATTER);
            return self();
        }
    }

    public LocalDateTime getDate() {
        return LocalDateTime.parse(date, Constants.DB_DATE_FORMATTER);
    }

    public void setDate(LocalDateTime date) {
        this.date = date.format(Constants.DB_DATE_FORMATTER);
    }
}
