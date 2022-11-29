package com.example.hw8

import java.io.Writer
import java.time.Clock
import java.util.concurrent.TimeUnit

class EventsStatisticImpl(

    private val clock: Clock,

    private val minutesTracking: Long = 60,
) : EventsStatistic {

    private val eventNameToRpm = mutableMapOf<String, ArrayDeque<RpmInfo>>()

    private fun clearIrrelevantRpm() {
        val epochMinutes = TimeUnit.MILLISECONDS.toMinutes(clock.millis())
        val iterator = eventNameToRpm.iterator()
        while (iterator.hasNext()) {
            val (_, rpmInfos) = iterator.next()

            while (rpmInfos.isNotEmpty() && rpmInfos.first().epochMinutes <= epochMinutes - minutesTracking) {
                rpmInfos.removeFirst()
            }

            if (rpmInfos.isEmpty()) {
                iterator.remove()
            }
        }
    }

    override fun incEvent(name: String) {
        clearIrrelevantRpm()
        val epochMinutes = TimeUnit.MILLISECONDS.toMinutes(clock.millis())
        val rpmInfos = eventNameToRpm.computeIfAbsent(name) { ArrayDeque() }

        if (rpmInfos.lastOrNull()?.epochMinutes == epochMinutes) {
            rpmInfos.last().requests++
        } else {
            rpmInfos.addLast(RpmInfo(epochMinutes, 1))
        }
    }

    override fun getEventStatisticByName(name: String): EventStatisticSummary {
        clearIrrelevantRpm()
        return getEventStatisticByInfoList(name, eventNameToRpm[name])
    }

    private fun getEventStatisticByInfoList(name: String, infos: List<RpmInfo>?): EventStatisticSummary {
        val requestsPerMinute = infos
            ?.sumOf { it.requests }
            ?.div(60.0)
            ?: 0.0
        return EventStatisticSummary(name, requestsPerMinute)
    }

    override fun getAllEventsStatistic(): List<EventStatisticSummary> {
        clearIrrelevantRpm()
        return eventNameToRpm
            .map { (eventName, infos) -> getEventStatisticByInfoList(eventName, infos) }
            .sortedBy { it.eventName }
    }

    override fun printStatistic(writer: Writer) {
        getAllEventsStatistic()
            .also {
                if (it.isEmpty()) {
                    writer.write("No events happened for the past $minutesTracking minutes :(")
                    writer.write(System.lineSeparator())
                    return
                }

                writer.write("Event statistics for the past $minutesTracking minutes:")
                writer.write(System.lineSeparator())
            }
            .sortedBy { it.eventName }
            .forEach {
                writer.write(
                    "Request rate is ${it.requestsPerMinute} requests per minute for '${it.eventName}' event."
                )
                writer.write(System.lineSeparator())
            }
    }

    private data class RpmInfo(
        val epochMinutes: Long,
        var requests: Long,
    )
}
