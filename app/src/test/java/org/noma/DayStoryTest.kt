package org.noma

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class DayStoryTest {
    private val zone=ZoneId.of("UTC")
    private val date=LocalDate.of(2026,9,23)
    private val start=date.atStartOfDay(zone).toInstant().toEpochMilli()
    private val registry=DomainRegistry(listOf(Classification("known.test","Owner","Analytics","source","version")))
    private fun event(id: String,offset: Long,kind: String="DNS_REQUEST",value: String="known.test")=
        Event(id,start+offset,kind,value,"test")
    private fun story(events: List<Event>)=DayStory.build(DailySummarizer.build(events,registry,date,zone,start+3_600_000),registry)

    @Test fun chronologicalChaptersPreserveEveryRecordWithoutLinkingAppsToDns() {
        val records=listOf(event("later",900000),event("app",1,"APP_RESUMED","org.app"),event("dns",2),event("unknown",3,value="unknown.test"))
        val story=story(records+records.first())
        assertEquals(4,story.frameCount)
        assertEquals(listOf("app","dns","unknown","later"),story.chapters.flatMap { it.events }.map { it.id })
        assertEquals(2,story.chapters[0].cumulativeRequests)
        assertEquals(3,story.chapters[1].cumulativeRequests)
        assertEquals(1,story.chapters[1].cumulativeOwners)
        assertEquals(listOf("app"),story.chapters[0].graph.appEvents.map { it.id })
        assertEquals(listOf("dns"),story.chapters[0].graph.organisations.single().domains.single().events.map { it.id })
        assertEquals("unknown.test",story.chapters[0].graph.unknown.single().domain)
    }
    @Test fun emptyDayHasNoInventedPlaybackAndGapsHaveNoChapters() {
        assertEquals(0,story(emptyList()).frameCount)
        val story=story(listOf(event("first",1),event("last",3_000_000)))
        assertEquals(2,story.chapters.size)
        assertEquals(story.summary.facts.requests,story.chapters.last().cumulativeRequests)
    }
}
