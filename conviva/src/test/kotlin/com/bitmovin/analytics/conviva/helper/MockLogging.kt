package com.bitmovin.analytics.conviva.helper

import android.util.Log
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic

fun mockLogging() {
    mockkStatic(Log::class)
    every { Log.v(any(), any()) } returns 0
    every { Log.d(any(), any()) } returns 0
    every { Log.i(any(), any()) } returns 0
    every { Log.e(any(), any()) } returns 0
    every { Log.w(any(), any<String>()) } returns 0
}

fun unmockLogging() {
    unmockkStatic(Log::class)
}
