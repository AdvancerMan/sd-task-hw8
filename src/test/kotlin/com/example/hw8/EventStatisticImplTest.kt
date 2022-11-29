package com.example.hw8

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import java.io.StringWriter
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.util.*

@SpringBootTest
class EventStatisticImplTest {

    lateinit var clock: DelegatedMutableClock

    lateinit var eventsStatisticImpl: EventsStatisticImpl

    lateinit var random: Random

    @BeforeEach
    fun init() {
        clock = DelegatedMutableClock(Clock.fixed(Instant.EPOCH, ZoneOffset.UTC))
        eventsStatisticImpl = EventsStatisticImpl(clock)
        random = Random(42)
    }

    private fun assertPrintsOutput(expectedTrimmed: String) {
        val buffer = StringWriter()
        eventsStatisticImpl.printStatistic(buffer)
        Assertions.assertEquals(expectedTrimmed + System.lineSeparator(), buffer.toString())
    }

    @Test
    fun testNoEventsStatistic() {
        Assertions.assertEquals(emptyList<EventStatisticSummary>(), eventsStatisticImpl.getAllEventsStatistic())
        Assertions.assertEquals(EventStatisticSummary("asd", 0.0), eventsStatisticImpl.getEventStatisticByName("asd"))
        assertPrintsOutput("No events happened for the past 60 minutes :(")
    }

    @Test
    fun testOneEventHappened() {
        val eventName = "asd"
        val requestsPerMinute = 1.0 / 60.0
        val expectedSummary = EventStatisticSummary(eventName, requestsPerMinute)

        eventsStatisticImpl.incEvent(eventName)

        Assertions.assertEquals(listOf(expectedSummary), eventsStatisticImpl.getAllEventsStatistic())
        Assertions.assertEquals(expectedSummary, eventsStatisticImpl.getEventStatisticByName(eventName))
        assertPrintsOutput(
            """
            Event statistics for the past 60 minutes:
            Request rate is $requestsPerMinute requests per minute for '$eventName' event.
        """.trimIndent()
        )
    }

    @Test
    fun testManyEventsCalculated() {
        val eventName1 = "asd1"
        val eventName2 = "asd2"
        val requestsPerMinute = 1.0 / 60.0
        val expectedSummary1 = EventStatisticSummary(eventName1, requestsPerMinute)
        val expectedSummary2 = EventStatisticSummary(eventName2, requestsPerMinute)

        eventsStatisticImpl.incEvent(eventName1)
        eventsStatisticImpl.incEvent(eventName2)

        Assertions.assertEquals(listOf(expectedSummary1, expectedSummary2), eventsStatisticImpl.getAllEventsStatistic())
        Assertions.assertEquals(expectedSummary1, eventsStatisticImpl.getEventStatisticByName(eventName1))
        Assertions.assertEquals(expectedSummary2, eventsStatisticImpl.getEventStatisticByName(eventName2))
        assertPrintsOutput(
            """
            Event statistics for the past 60 minutes:
            Request rate is $requestsPerMinute requests per minute for '$eventName1' event.
            Request rate is $requestsPerMinute requests per minute for '$eventName2' event.
        """.trimIndent()
        )
    }

    @Test
    fun testEventsAreClearedAfterAnHour() {
        val eventName1 = "asd1"
        val eventName2 = "asd2"
        eventsStatisticImpl.incEvent(eventName1)
        eventsStatisticImpl.incEvent(eventName2)

        clock.tick(Duration.ofHours(1L))

        Assertions.assertEquals(emptyList<EventStatisticSummary>(), eventsStatisticImpl.getAllEventsStatistic())
        Assertions.assertEquals(
            EventStatisticSummary(eventName1, 0.0),
            eventsStatisticImpl.getEventStatisticByName(eventName1)
        )
        Assertions.assertEquals(
            EventStatisticSummary(eventName2, 0.0),
            eventsStatisticImpl.getEventStatisticByName(eventName2)
        )
        assertPrintsOutput("No events happened for the past 60 minutes :(")
    }

    @Test
    fun testEventsAreNotClearedAfterJustBelowAnHour() {
        val eventName1 = "asd1"
        val eventName2 = "asd2"
        val requestsPerMinute = 1.0 / 60.0
        val expectedSummary1 = EventStatisticSummary(eventName1, requestsPerMinute)
        val expectedSummary2 = EventStatisticSummary(eventName2, requestsPerMinute)

        eventsStatisticImpl.incEvent(eventName1)
        eventsStatisticImpl.incEvent(eventName2)

        clock.tick(Duration.ofMinutes(59L))

        Assertions.assertEquals(listOf(expectedSummary1, expectedSummary2), eventsStatisticImpl.getAllEventsStatistic())
        Assertions.assertEquals(expectedSummary1, eventsStatisticImpl.getEventStatisticByName(eventName1))
        Assertions.assertEquals(expectedSummary2, eventsStatisticImpl.getEventStatisticByName(eventName2))
        assertPrintsOutput(
            """
            Event statistics for the past 60 minutes:
            Request rate is $requestsPerMinute requests per minute for '$eventName1' event.
            Request rate is $requestsPerMinute requests per minute for '$eventName2' event.
        """.trimIndent()
        )
    }

    @Test
    fun testOneRpmEvent() {
        val eventName = "asd"
        val requestsPerMinute = 1.0
        val expectedSummary = EventStatisticSummary(eventName, requestsPerMinute)

        repeat(60 * 2) {
            clock.tick(Duration.ofMinutes(1L))
            eventsStatisticImpl.incEvent(eventName)
        }

        Assertions.assertEquals(listOf(expectedSummary), eventsStatisticImpl.getAllEventsStatistic())
        Assertions.assertEquals(expectedSummary, eventsStatisticImpl.getEventStatisticByName(eventName))
        assertPrintsOutput(
            """
            Event statistics for the past 60 minutes:
            Request rate is $requestsPerMinute requests per minute for '$eventName' event.
        """.trimIndent()
        )
    }

    @Test
    fun testManyEventsInMinute() {
        val eventName = "asd"
        val requestsPerMinute = 1.0
        val expectedSummary = EventStatisticSummary(eventName, requestsPerMinute)

        repeat(60) {
            clock.tick(Duration.ofMillis(1L))
            eventsStatisticImpl.incEvent(eventName)
        }

        Assertions.assertEquals(listOf(expectedSummary), eventsStatisticImpl.getAllEventsStatistic())
        Assertions.assertEquals(expectedSummary, eventsStatisticImpl.getEventStatisticByName(eventName))
        assertPrintsOutput(
            """
            Event statistics for the past 60 minutes:
            Request rate is $requestsPerMinute requests per minute for '$eventName' event.
        """.trimIndent()
        )
    }

    @Test
    fun testRandomEvents() {
        val nonExistentEventName = "nonExistentEventName"
        val events = (1..10)
            .map { "event$it" }
            .associateWith { List(60) { random.nextInt(5) } }
        val requestsPerMinute = events.mapValues { it.value.sum() / 60.0 }
        val expectedSummaries = requestsPerMinute
            .map { EventStatisticSummary(it.key, it.value) }
            .sortedBy { it.eventName }

        repeat(60) { i ->
            clock.tick(Duration.ofMinutes(1L))
            events.entries
                .shuffled(random)
                .forEach { (name, rpm) ->
                    repeat(rpm[i]) {
                        eventsStatisticImpl.incEvent(name)
                    }
                }
        }

        Assertions.assertEquals(expectedSummaries, eventsStatisticImpl.getAllEventsStatistic())
        Assertions.assertEquals(EventStatisticSummary(nonExistentEventName, 0.0), eventsStatisticImpl.getEventStatisticByName(nonExistentEventName))
        expectedSummaries.forEach {
            Assertions.assertEquals(it, eventsStatisticImpl.getEventStatisticByName(it.eventName))
        }
    }
}
