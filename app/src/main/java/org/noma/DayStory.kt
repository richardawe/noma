package org.noma

/** Chapters group time only. They never attribute a DNS request to a nearby app. */
data class StoryChapter(val events: List<Event>, val graph: PersonalDataGraph,
                        val cumulativeRequests: Int, val cumulativeOwners: Int) {
    val start get() = events.first().time
    val end get() = events.last().time
}
data class DayStory(val summary: DailySummary, val chapters: List<StoryChapter>) {
    val frameCount get() = if(chapters.isEmpty()) 0 else chapters.size+2
    companion object {
        fun build(summary: DailySummary, registry: DomainRegistry): DayStory {
            var requests=0
            val owners=mutableSetOf<String>()
            val chapters=summary.facts.events.groupBy { Math.floorDiv(it.time,900_000L) }.toSortedMap().values.map { events ->
                val graph=PersonalDataGraph.build(events,registry)
                requests+=events.count { it.kind=="DNS_REQUEST" }
                owners+=graph.organisations.map { it.name }
                StoryChapter(events,graph,requests,owners.size)
            }
            return DayStory(summary,chapters)
        }
    }
}
