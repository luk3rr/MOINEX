/*
 * Filename: CalendarEvent.kt (original filename: CalendarEvent.java)
 * Created on: March  1, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 07/03/2026
 */

package org.moinex.model

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.moinex.common.converter.LocalDateStringConverter
import org.moinex.model.enums.CalendarEventType
import java.time.LocalDate

@Entity
@Table(name = "calendar_event")
class CalendarEvent(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Int? = null,
    @Column(name = "date", nullable = false)
    @Convert(converter = LocalDateStringConverter::class)
    var date: LocalDate,
    @Column(name = "title", nullable = false)
    var title: String,
    @Column(name = "description")
    var description: String? = null,
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    var eventType: CalendarEventType,
) {
    init {
        title = title.trim()
        require(title.isNotEmpty()) {
            "Event title cannot be empty"
        }
    }

    override fun toString(): String = "CalendarEvent [id=$id, title='$title', eventType=$eventType]"
}
