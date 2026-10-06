package org.noma

import android.content.Context
import org.json.JSONObject
import java.security.MessageDigest

object RegistryLoader {
    fun load(context: Context): DomainRegistry = try {
        val bytes=context.assets.open("registry.json").use { it.readBytes() }
        val expected=context.assets.open("registry.sha256").bufferedReader().use { it.readText().trim() }
        decode(bytes,expected)
    } catch (_: Exception) { DomainRegistry(emptyList()) }

    internal fun decode(bytes: ByteArray, expected: String): DomainRegistry = try {
        val hash=MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
        check(hash == expected)
        val root=JSONObject(String(bytes));val rows=root.getJSONArray("records")
        val list=(0 until rows.length()).map { i ->
            val r=rows.getJSONObject(i)
            val domain=r.getString("domain");check(DomainRegistry.normalize(domain)==domain)
            Classification(domain,r.getString("organisation"),r.getString("categories"),r.getString("source"),root.getString("version"))
        }
        check(list.map { it.domain }.distinct().size == list.size)
        DomainRegistry(list)
    } catch (_: Exception) { DomainRegistry(emptyList()) }
}
