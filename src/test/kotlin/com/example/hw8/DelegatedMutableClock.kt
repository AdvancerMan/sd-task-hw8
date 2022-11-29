package com.example.hw8

import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

class DelegatedMutableClock(

    private val delegateClock: Clock
) : Clock() {

    private var offsetDuration: Duration = Duration.ZERO

    override fun getZone(): ZoneId {
        return ZoneOffset.UTC
    }

    override fun withZone(p0: ZoneId?): Clock {
        throw UnsupportedOperationException("Zone change is unsupported")
    }

    override fun instant(): Instant {
        return delegateClock.instant().plus(offsetDuration)
    }

    fun tick(duration: Duration) {
        offsetDuration += duration
    }
}
