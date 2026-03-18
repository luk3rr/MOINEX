/*
 * Filename: App.kt (original filename: MainApplication.java)
 * Created on: September  6, 2024
 * Author: Lucas Araújo <araujolucas@dcc.ufmg.br>
 *
 * Migrated to Kotlin on 18/03/2026
 */

package org.moinex.app

import javafx.application.Application

object App {
    @JvmStatic
    fun main(args: Array<String>) {
        Application.launch(JavaFXApp::class.java, *args)
    }
}
