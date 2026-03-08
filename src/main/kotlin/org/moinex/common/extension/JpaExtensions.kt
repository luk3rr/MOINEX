/*
 * Filename: JpaExtensions.kt
 * Created on: February 28, 2026
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 */

package org.moinex.common.extension

import jakarta.persistence.EntityNotFoundException
import org.springframework.data.jpa.repository.JpaRepository

inline fun <reified T, ID : Any> JpaRepository<T, ID>.findByIdOrThrow(id: ID): T =
    findById(id).orElseThrow {
        EntityNotFoundException("${T::class.simpleName} with id $id does not exist")
    }
