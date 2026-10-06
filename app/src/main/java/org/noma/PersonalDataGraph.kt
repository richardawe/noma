package org.noma

/** A derived view, rebuilt from retained evidence. It never joins an app to a DNS query. */
data class DomainEvidence(val domain: String, val events: List<Event>, val classification: Classification?)
data class OrganisationEvidence(val name: String, val domains: List<DomainEvidence>) {
    val eventCount get() = domains.sumOf { it.events.size }
    val categories get() = domains.mapNotNull { it.classification?.categories }
        .flatMap { it.split(',').map(String::trim) }.filter { it.isNotEmpty() }.distinct().sorted()
}
data class PersonalDataGraph(val organisations: List<OrganisationEvidence>, val unknown: List<DomainEvidence>,
                             val appEvents: List<Event>) {
    companion object {
        fun build(events: List<Event>, registry: DomainRegistry): PersonalDataGraph {
            val retained = events.distinctBy { it.id }.sortedWith(compareBy<Event> { it.time }.thenBy { it.id })
            val domains = retained.filter { it.kind == "DNS_REQUEST" }
                .groupBy { DomainRegistry.normalize(it.value) ?: it.value }
                .map { (name, evidence) -> DomainEvidence(name, evidence, registry.lookup(name)) }
                .sortedBy { it.domain }
            return PersonalDataGraph(
                domains.filter { it.classification != null }.groupBy { it.classification!!.organisation }
                    .map { (owner, records) -> OrganisationEvidence(owner, records) }.sortedBy { it.name },
                domains.filter { it.classification == null },
                retained.filter { it.kind == "APP_RESUMED" || it.kind == "APP_PAUSED" })
        }
    }
}
