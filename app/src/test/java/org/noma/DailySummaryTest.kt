package org.noma

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class DailySummaryTest {
    private val zone=ZoneId.of("Europe/London")
    private val date=LocalDate.of(2026,9,22)
    private val start=date.atStartOfDay(zone).toInstant().toEpochMilli()
    private val now=start+12*3_600_000
    private val record=Classification("known.test","Dataset owner","Analytics","source","revision")
    private val registry=DomainRegistry(listOf(record))
    private fun event(id: String, value: String="known.test", kind: String="DNS_REQUEST", time: Long=now-1000)=
        Event(id,time,kind,value,"fixture")

    @Test fun countsAreGroundedAndEveryClaimResolvesToOriginalEvidence() {
        val dns=event("dns")
        val summary=DailySummarizer.build(listOf(dns,dns,event("dns2","KNOWN.TEST."),
            event("app","org.app","APP_RESUMED"),event("pause","org.app","APP_PAUSED"),
            event("unknown","other.test")),registry,date,zone,now)
        assertEquals(1,summary.facts.appsResumed)
        assertEquals(3,summary.facts.requests)
        assertEquals(2,summary.facts.names)
        assertEquals(setOf("dns","dns2"),summary.claims.single { it.level==EvidenceLevel.CLASSIFIED }.evidenceIds.toSet())
        summary.claims.forEach { claim ->
            assertTrue(claim.evidenceIds.isNotEmpty())
            assertEquals(claim.evidenceIds.toSet(),summary.evidence(claim).map { it.id }.toSet())
        }
        assertFalse(summary.claims.any { "tracking" in it.text || "shared" in it.text })
    }

    @Test fun dayBoundariesRetentionAndFutureRecordsAreExcluded() {
        val events=listOf(event("before",time=start-1),event("start",time=start),
            event("now",time=now),event("future",time=now+1),event("next",time=start+86_400_000))
        assertEquals(setOf("start","now"),DailySummarizer.build(events,registry,date,zone,now).facts.events.map { it.id }.toSet())
        val yesterday=date.minusDays(1)
        val summary=DailySummarizer.build(listOf(event("expired",time=now-86_400_001),
            event("boundary",time=now-86_400_000)),registry,yesterday,zone,now)
        assertTrue(summary.facts.partialRetention)
        assertEquals(listOf("boundary"),summary.facts.events.map { it.id })
    }

    @Test fun localCalendarDaysHandleDstAndTimezone() {
        for((d,hours) in listOf(LocalDate.of(2026,3,29) to 23,LocalDate.of(2026,10,25) to 25)) {
            val clock=d.atStartOfDay(zone).toInstant().toEpochMilli()+1000
            val facts=DailySummarizer.build(emptyList(),registry,d,zone,clock).facts
            assertEquals(hours*3_600_000L,facts.endExclusive-facts.start)
        }
        val pacific=ZoneId.of("America/Los_Angeles")
        val midnight=date.atStartOfDay(pacific).toInstant().toEpochMilli()
        assertTrue(DailySummarizer.build(listOf(event("previous",time=midnight-1)),registry,date,pacific,midnight).facts.events.isEmpty())
    }

    @Test fun absentEvidenceIsNotAZeroActivityClaim() {
        val empty=DailySummarizer.build(emptyList(),registry,date,zone,now)
        assertTrue(empty.claims.isEmpty())
        val paused=DailySummarizer.build(listOf(event("p","org.app","APP_PAUSED")),registry,date,zone,now)
        assertEquals(0,paused.facts.appsResumed)
        assertTrue(paused.claims.single().text.contains("without a recorded resume"))
        assertTrue(DailySummarizer.build(listOf(event("unsupported",kind="LOCATION")),registry,date,zone,now).claims.isEmpty())
    }

    @Test fun unknownAndErasureCannotCreateOwnersOrSurvivingClaims() {
        val summary=DailySummarizer.build(listOf(event("1","new.known.test")),registry,date,zone,now)
        assertFalse(summary.claims.any { it.level==EvidenceLevel.CLASSIFIED })
        assertTrue(summary.claims.any { it.level==EvidenceLevel.UNKNOWN })
        assertTrue(DailySummarizer.build(emptyList(),registry,date,zone,now).claims.isEmpty())
    }
}
