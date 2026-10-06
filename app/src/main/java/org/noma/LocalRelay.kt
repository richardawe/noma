package org.noma

import android.net.Network
import android.net.VpnService
import java.io.*
import java.net.*
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.*

/** Authenticated loopback transport for the native TCP/IP stack. No remote proxy. */
internal class LocalRelay(private val vpn: VpnService, private val network: Network,
                          private val resolver: InetAddress, private val observe: (ByteArray) -> Unit,
                          private val failed: () -> Unit): Closeable {
    val password=UUID.randomUUID().toString()
    private val listener=ServerSocket(0,32,InetAddress.getByName("127.0.0.1"))
    val port get()=listener.localPort
    private val sockets=ConcurrentHashMap.newKeySet<Closeable>()
    private val sessions=Semaphore(48)
    private val workers=Executors.newCachedThreadPool()
    @Volatile private var closed=false
    fun start() { workers.execute {
        try { while(!closed) {
            val socket=listener.accept()
            if(!sessions.tryAcquire()) { socket.close(); continue }
            sockets+=socket
            try { workers.execute { try { serve(socket) } catch (_: Exception) { /* A failed flow ends only that flow. */ }
                finally { sockets-=socket;socket.close();sessions.release() } } }
            catch (_: RejectedExecutionException) { sockets-=socket;socket.close();sessions.release() }
        } } catch (_: IOException) { if(!closed) failed() }
    } }
    private fun address(input: DataInputStream): InetSocketAddress {
        check(input.readUnsignedByte()==1) // native stack provides numeric IPv4; never resolve names here
        val bytes=ByteArray(4);input.readFully(bytes)
        return InetSocketAddress(InetAddress.getByAddress(bytes),input.readUnsignedShort())
    }
    private fun destination(target: InetSocketAddress)=if(target.address.hostAddress=="10.111.0.1" && target.port==53)
        InetSocketAddress(resolver,53) else target
    private fun record(bytes: ByteArray) {
        try { observe(bytes) } catch (e: Exception) { failed();throw e }
    }
    private fun serve(client: Socket) {
        client.soTimeout=10_000
        val input=DataInputStream(client.getInputStream());val output=DataOutputStream(client.getOutputStream())
        check(input.readUnsignedByte()==5)
        val methods=ByteArray(input.readUnsignedByte());input.readFully(methods)
        check(2.toByte() in methods);output.write(byteArrayOf(5,2));output.flush()
        check(input.readUnsignedByte()==1)
        val user=ByteArray(input.readUnsignedByte());input.readFully(user)
        val secret=ByteArray(input.readUnsignedByte());input.readFully(secret)
        check(String(user,Charsets.US_ASCII)=="noma" && MessageDigest.isEqual(secret,password.toByteArray()))
        output.write(byteArrayOf(1,0));output.flush()
        check(input.readUnsignedByte()==5);val command=input.readUnsignedByte();check(input.readUnsignedByte()==0)
        val target=address(input)
        check(command==1 || command==5)
        if(command==1) tcp(client,input,output,target) else {
            output.write(byteArrayOf(5,0,0,1,127,0,0,1,0,0));output.flush()
            udp(client,input,output)
        }
    }
    private fun tcp(client: Socket,input: DataInputStream,output: DataOutputStream,target: InetSocketAddress) {
        val remote=Socket();sockets+=remote
        try {
            remote.bind(InetSocketAddress(0)) // Materialise the lazy TCP file descriptor before protect().
            check(vpn.protect(remote));network.bindSocket(remote)
            try { remote.connect(destination(target),10_000) } catch(e: IOException) {
                if(target.address.hostAddress=="10.111.0.1" && target.port==53) {
                    remote.close();tcpDnsViaUdp(client,input,output);return
                }
                throw e
            }
            client.soTimeout=300_000;remote.soTimeout=300_000
            output.write(byteArrayOf(5,0,0,1,127,0,0,1,0,0));output.flush()
            val incoming=workers.submit { try { remote.getInputStream().copyTo(output,16_384);output.flush();client.shutdownOutput() }
                catch (_: Exception) { runCatching { client.close() } } }
            try {
                val outgoing=DataOutputStream(remote.getOutputStream())
                if(target.port==53) while(true) {
                    val length=try { input.readUnsignedShort() } catch (_: EOFException) { break }
                    val query=ByteArray(length);input.readFully(query);record(query)
                    outgoing.writeShort(length);outgoing.write(query);outgoing.flush()
                } else input.copyTo(outgoing,16_384)
                outgoing.flush();remote.shutdownOutput();incoming.get(300,TimeUnit.SECONDS)
            } finally { incoming.cancel(true) }
        } finally { sockets-=remote;remote.close() }
    }
    /** Some network DNS proxies (including Android Emulator) expose only UDP. */
    private fun tcpDnsViaUdp(client: Socket,input: DataInputStream,output: DataOutputStream) {
        val socket=DatagramSocket(null);sockets+=socket
        try {
            check(vpn.protect(socket));network.bindSocket(socket);socket.bind(InetSocketAddress(0))
            socket.connect(resolver,53);socket.soTimeout=10_000;client.soTimeout=60_000
            output.write(byteArrayOf(5,0,0,1,127,0,0,1,0,0));output.flush()
            while(!closed) {
                val length=try { input.readUnsignedShort() } catch (_: EOFException) { break }
                check(length in 12..65507)
                val query=ByteArray(length);input.readFully(query);record(query)
                socket.send(DatagramPacket(query,query.size))
                val response=DatagramPacket(ByteArray(65507),65507);socket.receive(response)
                check(response.length>=12 && DnsPacket.u16(response.data,0)==DnsPacket.u16(query,0))
                output.writeShort(response.length);output.write(response.data,0,response.length);output.flush()
            }
        } finally { sockets-=socket;socket.close() }
    }
    private fun udp(client: Socket,input: DataInputStream,output: DataOutputStream) {
        client.soTimeout=60_000
        // Each native UDP association belongs to one flow. Cap destinations defensively.
        val remotes=mutableMapOf<InetSocketAddress,DatagramSocket>()
        try { while(!closed) {
            // Pinned 2.17.1 wire format uses payload length (its README says total length).
            val length=input.readUnsignedShort();val header=input.readUnsignedByte()
            check(header==10 && length in 0..65507)
            val target=address(input);val bytes=ByteArray(length);input.readFully(bytes)
            val remote=remotes[target] ?: run {
                check(remotes.size<4)
                val socket=DatagramSocket(null);sockets+=socket
                try {
                    check(vpn.protect(socket));network.bindSocket(socket);socket.bind(InetSocketAddress(0))
                    socket.connect(destination(target));socket.soTimeout=60_000
                } catch(e: Exception) { sockets-=socket;socket.close();throw e }
                remotes[target]=socket
                workers.execute { try {
                    val buffer=ByteArray(65507)
                    while(!socket.isClosed) {
                        val packet=DatagramPacket(buffer,buffer.size);socket.receive(packet)
                        synchronized(output) {
                            output.writeShort(packet.length);output.writeByte(10);output.writeByte(1)
                            output.write(target.address.address);output.writeShort(target.port)
                            output.write(packet.data,0,packet.length);output.flush()
                        }
                    }
                } catch (_: Exception) { runCatching { client.close() } } }
                socket
            }
            if(target.port==53) record(bytes)
            remote.send(DatagramPacket(bytes,bytes.size))
        } } finally { remotes.values.forEach { sockets-=it;it.close() } }
    }
    override fun close() {
        closed=true;listener.close();sockets.forEach { runCatching { it.close() } };sockets.clear();workers.shutdownNow()
    }
}
