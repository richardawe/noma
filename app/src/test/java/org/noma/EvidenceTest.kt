package org.noma

import org.junit.Assert.*
import org.junit.Test

class EvidenceTest {
    private val entry=Classification("graph.facebook.com","Source owner","Social","fixture-source","fixture-revision")
    private val registry=DomainRegistry(listOf(entry))
    @Test fun exactRecordAndNormalization() {
        assertEquals(entry,registry.lookup("GRAPH.FACEBOOK.COM."))
        assertEquals("fixture-source",registry.lookup("graph.facebook.com")?.source)
    }
    @Test fun unknownIsNotGuessed() {
        listOf("facebook.com","evilgraph.facebook.com","graph.facebook.com.evil.test","new.graph.facebook.com",
            "https://graph.facebook.com","1.2.3.4","graph..facebook.com","-bad.com").forEach { assertNull(registry.lookup(it)) }
        assertNull(DomainRegistry(emptyList()).lookup("graph.facebook.com"))
    }
    @Test fun localeIndependentNormalization() {
        val before=java.util.Locale.getDefault()
        try { java.util.Locale.setDefault(java.util.Locale("tr"));assertEquals("i.example",DomainRegistry.normalize("I.EXAMPLE")) }
        finally { java.util.Locale.setDefault(before) }
    }
    private fun query(): ByteArray {
        val name="graph.facebook.com".split('.').flatMap { listOf(it.length.toByte())+it.toByteArray().toList() }.toByteArray()
        return ByteArray(12).also { DnsPacket.put16(it,0,123);DnsPacket.put16(it,2,256);DnsPacket.put16(it,4,1) }+name+byteArrayOf(0,0,1,0,1)
    }
    private fun packet(dns: ByteArray=query()): ByteArray {
        val b=ByteArray(28+dns.size);b[0]=0x45;b[8]=64;b[9]=17
        DnsPacket.put16(b,2,b.size);byteArrayOf(10,111,0,2).copyInto(b,12);byteArrayOf(10,111,0,1).copyInto(b,16)
        DnsPacket.put16(b,20,54321);DnsPacket.put16(b,22,53);DnsPacket.put16(b,24,dns.size+8)
        DnsPacket.put16(b,10,DnsPacket.checksum(b.copyOfRange(0,20)));dns.copyInto(b,28)
        return b
    }
    @Test fun decodeValidQuestionAndRoundtripReply() {
        val p=DnsPacket.parse(packet())!!
        assertEquals("graph.facebook.com",p.question.name);assertEquals(54321,p.sourcePort)
        val answer=query().also { DnsPacket.put16(it,2,0x8180) }
        assertTrue(p.accepts(answer))
        val out=p.reply(answer)
        assertEquals(0,DnsPacket.checksum(out.copyOfRange(0,20)))
        assertArrayEquals(byteArrayOf(10,111,0,1),out.copyOfRange(12,16))
        assertEquals(54321,DnsPacket.u16(out,22))
    }
    @Test fun truncatedAndMalformedPacketsAreNotEvidence() {
        val good=packet()
        for(i in good.indices) assertNull(DnsPacket.parse(good.copyOf(i)))
        assertNull(DnsPacket.parse(good.copyOf().also { it[9]=6 }))
        assertNull(DnsPacket.parse(good.copyOf().also { it[26]=1 }))
        assertNull(DnsPacket.parse(good.copyOf().also { it[6]=0x20 }))
        assertNull(DnsPacket.parse(packet(query().also { it[12]=0xc0.toByte() })))
        assertNull(DnsPacket.parse(packet(query().also { DnsPacket.put16(it,4,2) })))
        assertNull(DnsPacket.parse(packet(query().also { DnsPacket.put16(it,2,0x8180) })))
    }
    @Test fun wrongTransactionOrQuestionRejected() {
        val p=DnsPacket.parse(packet())!!
        val reply=query().also { DnsPacket.put16(it,2,0x8180) }
        assertFalse(p.accepts(reply.copyOf().also { it[0]=44 }))
        assertFalse(p.accepts(reply.copyOf().also { it[13]='x'.code.toByte() }))
        assertTrue(p.accepts(p.failure()))
    }
    @Test fun hostileBytesDoNotCrashDecoder() {
        val random=java.util.Random(1234)
        repeat(10000) { val b=ByteArray(random.nextInt(512));random.nextBytes(b);DnsPacket.parse(b);DnsPacket.question(b) }
    }
}
