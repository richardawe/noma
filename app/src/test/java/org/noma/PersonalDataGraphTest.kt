package org.noma

import org.junit.Assert.*
import org.junit.Test

class PersonalDataGraphTest {
    private val record = Classification("known.test", "Owner", "Analytics, Advertising", "source", "revision")
    private val registry = DomainRegistry(listOf(record))
    private fun event(id: String, value: String, kind: String = "DNS_REQUEST") = Event(id, 100, kind, value, "observed")

    @Test fun evidenceIsDeduplicatedAndSourceIsPreserved() {
        val e = event("1", "KNOWN.TEST.")
        val graph = PersonalDataGraph.build(listOf(e, e, event("2", "known.test")), registry)
        val owner = graph.organisations.single()
        assertEquals(2, owner.eventCount)
        assertEquals(listOf("Advertising", "Analytics"), owner.categories)
        assertEquals(record, owner.domains.single().classification)
        assertEquals(setOf("1", "2"), owner.domains.single().events.map { it.id }.toSet())
    }

    @Test fun unknownNamesAndNearbyAppsNeverAcquireAnOwnershipEdge() {
        val graph = PersonalDataGraph.build(listOf(event("1", "known.test"),
            event("2", "new.known.test"), event("3", "org.app", "APP_RESUMED")), registry)
        assertEquals("new.known.test", graph.unknown.single().domain)
        assertNull(graph.unknown.single().classification)
        assertEquals("org.app", graph.appEvents.single().value)
        assertEquals(listOf("1"), graph.organisations.single().domains.single().events.map { it.id })
    }

    @Test fun erasureRemovesAllDerivedNodes() {
        val graph = PersonalDataGraph.build(emptyList(), registry)
        assertTrue(graph.organisations.isEmpty())
        assertTrue(graph.unknown.isEmpty())
        assertTrue(graph.appEvents.isEmpty())
    }
}
