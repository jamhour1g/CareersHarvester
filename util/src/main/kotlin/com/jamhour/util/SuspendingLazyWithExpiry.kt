package com.jamhour.util

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration
import kotlin.time.DurationUnit

data class SuspendingLazyWithExpiry<out T>(
    val expiryDuration: Duration,
    private val initializer: suspend () -> T
) {
    private var cachedValue: T? = null
    private var lastInitTime: Long = Long.MIN_VALUE
    private val mutex = Mutex()

    suspend fun getValue(): T = mutex.withLock {
        val now = System.currentTimeMillis()
        val isPastExpiryTime = (now - lastInitTime) > expiryDuration.toLong(DurationUnit.MILLISECONDS)
        if (cachedValue == null || isPastExpiryTime) {
            cachedValue = initializer()
            lastInitTime = now
        }
        cachedValue!!
    }
}