package org.noma

/** Narrow IPv4/UDP decoder. No payload is persisted. Unsupported packets yield no evidence. */
data class DnsQuestion(val name: String, val type: Int, val end: Int)
data class DnsPacket(val ip: ByteArray, val sourcePort: Int, val payload: ByteArray, val question: DnsQuestion) {
    companion object {
        fun u16(b: ByteArray, i: Int) = ((b[i].toInt() and 255) shl 8) or (b[i + 1].toInt() and 255)
        fun question(b: ByteArray): DnsQuestion? {
            if (b.size < 17 || u16(b, 4) != 1) return null
            var i = 12
            val labels = mutableListOf<String>()
            while (i < b.size) {
                val n = b[i++].toInt() and 255
                if (n == 0) break
                if (n > 63 || i + n > b.size) return null // compressed/malformed questions not supported
                if ((i until i+n).any { (b[it].toInt() and 255) !in 33..126 }) return null
                labels += String(b, i, n, Charsets.US_ASCII); i += n
            }
            if (i + 4 > b.size || u16(b, i + 2) != 1) return null
            val name = DomainRegistry.normalize(labels.joinToString(".")) ?: return null
            return DnsQuestion(name, u16(b, i), i + 4)
        }
        fun parse(b: ByteArray): DnsPacket? {
            if (b.size < 40 || b[0].toInt() and 255 != 0x45 || b[9].toInt() != 17) return null
            if (u16(b, 2) != b.size || u16(b, 6) and 0x3fff != 0 || checksum(b.copyOfRange(0,20)) != 0) return null
            if (!b.copyOfRange(16,20).contentEquals(byteArrayOf(10,111,0,1))) return null
            if (u16(b,22) != 53 || u16(b,24) != b.size-20 || u16(b,20) == 0) return null
            if (u16(b,26) != 0) {
                val pseudo=ByteArray(12+b.size-20)
                b.copyInto(pseudo,0,12,20);pseudo[9]=17;put16(pseudo,10,b.size-20)
                b.copyInto(pseudo,12,20,b.size)
                if(checksum(pseudo)!=0) return null
            }
            val dns = b.copyOfRange(28,b.size)
            if (dns.size < 12 || u16(dns,2) and 0xf800 != 0) return null // query, standard opcode
            val q = question(dns) ?: return null
            return DnsPacket(b.copyOfRange(0,20),u16(b,20),dns,q)
        }
        fun checksum(bytes: ByteArray): Int {
            var sum = 0L
            for (i in bytes.indices step 2) sum += ((bytes[i].toInt() and 255) shl 8) +
                (if (i+1 < bytes.size) bytes[i+1].toInt() and 255 else 0)
            while (sum shr 16 != 0L) sum = (sum and 65535) + (sum shr 16)
            return sum.toInt().inv() and 65535
        }
        fun put16(b: ByteArray,i: Int,v: Int) { b[i]=(v ushr 8).toByte(); b[i+1]=v.toByte() }
    }
    fun accepts(response: ByteArray): Boolean = response.size >= 12 &&
        u16(response,0) == u16(payload,0) && u16(response,2) and 0xf800 == 0x8000 &&
        question(response)?.let { it.name == question.name && it.type == question.type } == true
    fun failure(): ByteArray = payload.copyOfRange(0,question.end).also {
        put16(it,2,0x8002 or (u16(payload,2) and 0x0100)); put16(it,6,0);put16(it,8,0);put16(it,10,0)
    }
    fun reply(response: ByteArray): ByteArray {
        require(response.size <= 65507)
        val out = ByteArray(28+response.size)
        out[0]=0x45; put16(out,2,out.size); out[8]=64;out[9]=17
        ip.copyInto(out,12,16,20);ip.copyInto(out,16,12,16)
        put16(out,10,checksum(out.copyOfRange(0,20)))
        put16(out,20,53);put16(out,22,sourcePort);put16(out,24,response.size+8)
        // IPv4 UDP checksum 0 is valid. DNS response is from a connected upstream socket.
        response.copyInto(out,28)
        return out
    }
}
