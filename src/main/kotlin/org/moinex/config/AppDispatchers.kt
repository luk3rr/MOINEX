package org.moinex.config

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.javafx.JavaFx

object AppDispatchers {
    val UI = Dispatchers.JavaFx
    val IO = Dispatchers.IO
    val CPU = Dispatchers.Default
}
