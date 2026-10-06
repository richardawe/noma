package org.noma

import java.util.Locale

/** Observation and classification deliberately have separate types. */
data class Event(val id: String, val time: Long, val kind: String, val value: String,
                 val source: String, val uid: Int? = null)
data class Classification(val domain: String, val organisation: String, val categories: String,
                          val source: String, val version: String)

class DomainRegistry(private val entries: List<Classification>) {
    private val byDomain = entries.associateBy { it.domain }
    fun lookup(raw: String): Classification? {
        val domain = normalize(raw) ?: return null
        // Entries are exact recorded domains/subdomains, never guessed suffix ownership.
        return byDomain[domain]
    }
    companion object {
        fun normalize(raw: String): String? {
            val name = raw.removeSuffix(".").lowercase(Locale.ROOT)
            if (name.length !in 1..253 || name.all { it.isDigit() || it == '.' }) return null
            if (name.split('.').any { it.length !in 1..63 ||
                !it.matches(Regex("[a-z0-9](?:[a-z0-9-]*[a-z0-9])?")) }) return null
            return name
        }
    }
}
