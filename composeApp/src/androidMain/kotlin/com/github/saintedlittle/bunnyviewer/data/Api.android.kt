package com.github.saintedlittle.bunnyviewer.data

import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.android.Android

internal actual fun platformEngine(): HttpClientEngineFactory<*> = Android
