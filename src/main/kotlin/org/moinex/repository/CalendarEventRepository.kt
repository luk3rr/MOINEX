/*
 * Filename: CalendarEventRepository.kt (original filename: CalendarEventRepository.java)
 * Created on: March  1, 2025
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrate to Kotlin on 07/03/2026
 */

package org.moinex.repository

import org.moinex.model.CalendarEvent
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface CalendarEventRepository : JpaRepository<CalendarEvent, Int>
