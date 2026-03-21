package org.moinex.config

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.javafx.JavaFx

object AppDispatchers {
    val UI: CoroutineDispatcher by lazy {
        runCatching {
            Dispatchers.JavaFx
        }.getOrElse {
            // Fallback para Unconfined quando JavaFX não está disponível (testes/CI)
            Dispatchers.Unconfined
        }
    }
    val IO = Dispatchers.IO
    val CPU = Dispatchers.Default
}
