package org.noma

import java.time.LocalDate
import java.time.ZoneId

enum class EvidenceLevel { OBSERVED, CLASSIFIED, UNKNOWN }

data class SummaryClaim(val id: String, val level: EvidenceLevel, val text: String,
                        val evidenceIds: List<String>)

/** Structured, bounded input for narration. Counts and ownership are never delegated to a model. */
data class DayFacts(val date: LocalDate, val zone: ZoneId, val generatedAt: Long,
                    val start: Long, val endExclusive: Long, val retainedFrom: Long,
                    val events: List<Event>, val graph: PersonalDataGraph) {
    val appsResumed = events.filter { it.kind == "APP_RESUMED" }.map { it.value }.distinct().size
    val requests = events.count { it.kind == "DNS_REQUEST" }
    val names = graph.organisations.sumOf { it.domains.size } + graph.unknown.size
    val partialRetention = retainedFrom > start
}

data class DailySummary(val facts: DayFacts, val claims: List<SummaryClaim>) {
    fun evidence(claim: SummaryClaim): List<Event> {
        val ids=claim.evidenceIds.toSet()
        return facts.events.filter { it.id in ids }
    }
}

object DailySummarizer {
    fun build(events: List<Event>, registry: DomainRegistry, date: LocalDate, zone: ZoneId,
              now: Long = System.currentTimeMillis()): DailySummary {
        val start=date.atStartOfDay(zone).toInstant().toEpochMilli()
        val end=date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val retainedFrom=maxOf(start,now-86_400_000)
        val retained=events.filter { it.time >= retainedFrom && it.time < end && it.time <= now &&
            it.kind in setOf("APP_RESUMED","APP_PAUSED","DNS_REQUEST") }
            .distinctBy { it.id }.sortedWith(compareBy<Event> { it.time }.thenBy { it.id })
        val graph=PersonalDataGraph.build(retained,registry)
        val facts=DayFacts(date,zone,now,start,end,retainedFrom,retained,graph)
        val claims=mutableListOf<SummaryClaim>()
        val resumes=retained.filter { it.kind == "APP_RESUMED" }
        if(resumes.isNotEmpty()) claims+=SummaryClaim("apps",EvidenceLevel.OBSERVED,
            "Noma recorded ${facts.appsResumed} ${plural(facts.appsResumed,"app")} resuming.",resumes.map { it.id })
        val dns=retained.filter { it.kind == "DNS_REQUEST" }
        if(dns.isNotEmpty()) claims+=SummaryClaim("dns",EvidenceLevel.OBSERVED,
            "Noma recorded ${dns.size} DNS ${plural(dns.size,"request")} for ${facts.names} distinct ${plural(facts.names,"name")}.",dns.map { it.id })
        graph.organisations.forEach { owner ->
            claims+=SummaryClaim("owner:${owner.name}",EvidenceLevel.CLASSIFIED,
                "${owner.domains.size} recorded ${plural(owner.domains.size,"name")} matched ${owner.name} in the bundled dataset.",
                owner.domains.flatMap { it.events }.map { it.id })
        }
        if(graph.unknown.isNotEmpty()) claims+=SummaryClaim("unknown",EvidenceLevel.UNKNOWN,
            "${graph.unknown.size} requested ${plural(graph.unknown.size,"name")} had no exact organisation match in the bundled dataset.",
            graph.unknown.flatMap { it.events }.map { it.id })
        // Pause-only records still have a truthful summary; never invent an opening or duration.
        if(claims.isEmpty() && retained.isNotEmpty()) claims+=SummaryClaim("pauses",EvidenceLevel.OBSERVED,
            "Noma retained ${retained.size} app pause ${plural(retained.size,"event")} without a recorded resume in this period.",retained.map { it.id })
        return DailySummary(facts,claims)
    }

    private fun plural(n: Int, word: String) = if(n==1) word else word+"s"
}
