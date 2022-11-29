package com.example.hw8

import java.io.IOException
import java.io.Writer
import kotlin.jvm.Throws

interface EventsStatistic {

    fun incEvent(name: String)

    fun getEventStatisticByName(name: String): EventStatisticSummary

    fun getAllEventsStatistic(): List<EventStatisticSummary>

    @Throws(IOException::class)
    fun printStatistic(writer: Writer)
}