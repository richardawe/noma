package org.noma

import android.app.*
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.*
import android.os.*
import android.system.OsConstants
import java.io.*
import java.net.*
import java.util.UUID

class DnsVpnService: VpnService() {
    @Volatile private var active=false
    private var generation=0L
    private var tunnel: ParcelFileDescriptor?=null
    private var reader: Thread?=null
    private var relay: LocalRelay?=null
    private var config: File?=null
    private val main=Handler(Looper.getMainLooper())
    private var callback: ConnectivityManager.NetworkCallback?=null
    private var defaultCallback: ConnectivityManager.NetworkCallback?=null
    private val cm by lazy { getSystemService(ConnectivityManager::class.java) }
    private val store by lazy { LocalStore.get(this) }
    override fun onStartCommand(intent: Intent?,flags: Int,startId: Int): Int {
        if (intent?.action == "STOP") { stopCapture();return START_NOT_STICKY }
        if (active) return START_NOT_STICKY
        try { startCapture() } catch (_: Exception) {
            status="DNS stopped: consent, storage or network unavailable. Restart from Noma."
            stopCapture()
        }
        return START_NOT_STICKY
    }
    @Synchronized private fun startCapture() {
        check(prepare(this)==null)
        val network=cm.activeNetwork ?: error("No network")
        val caps=cm.getNetworkCapabilities(network) ?: error("No capabilities")
        check(!caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN))
        val links=cm.getLinkProperties(network) ?: error("No links")
        // Never downgrade active encrypted DNS, including opportunistic Private DNS.
        if (links.isPrivateDnsActive || links.privateDnsServerName != null) {
            status="DNS unavailable with active Private DNS. Your DNS setting was not changed."
            stopCapture();return
        }
        val resolver=links.dnsServers.firstOrNull { it is Inet4Address } ?: run {
            status="DNS unavailable: no IPv4 network resolver.";stopCapture();return
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel("observe","Noma observation",NotificationManager.IMPORTANCE_LOW))
        val open=PendingIntent.getActivity(this,0,Intent(this,MainActivity::class.java),PendingIntent.FLAG_IMMUTABLE)
        val stop=PendingIntent.getService(this,1,Intent(this,DnsVpnService::class.java).setAction("STOP"),PendingIntent.FLAG_IMMUTABLE)
        val n=Notification.Builder(this,"observe").setSmallIcon(android.R.drawable.ic_menu_view)
            .setContentTitle("Noma is observing DNS requests").setContentText("Local storage • tap to inspect or stop")
            .setContentIntent(open).setOngoing(true).addAction(Notification.Action.Builder(null,"Stop",stop).build()).build()
        if(Build.VERSION.SDK_INT>=34) startForeground(1,n,ServiceInfo.FOREGROUND_SERVICE_TYPE_SYSTEM_EXEMPTED)
        else startForeground(1,n)
        val token=store.epoch()
        val session=++generation
        val local=LocalRelay(this,network,resolver,observe={ query ->
            if(active && generation==session) {
                packetsSeen.incrementAndGet()
                val question=if(query.size>=12 && DnsPacket.u16(query,2) and 0xf800==0) DnsPacket.question(query) else null
                if(question!=null) store.add(Event(UUID.randomUUID().toString(),System.currentTimeMillis(),"DNS_REQUEST",
                    question.name,"Local VPN · standard DNS question (not a connection)",null),token)
                else packetsRejected.incrementAndGet()
            }
        },failed={ main.post { if(active && generation==session) { status="Local forwarding failed. Observation stopped.";stopCapture() } } })
        relay=local
        val configuration=File(noBackupFilesDir,"tunnel.yml")
        configuration.writeText("""
            tunnel:
              mtu: 1500
              ipv4: 10.111.0.2
            socks5:
              address: 127.0.0.1
              port: ${local.port}
              username: noma
              password: '${local.password}'
              udp: tcp
            misc:
              max-session-count: 48
              connect-timeout: 10000
              tcp-read-write-timeout: 300000
              udp-read-write-timeout: 60000
              log-level: error
        """.trimIndent())
        config=configuration
        local.start()
        tunnel=Builder().setSession("Noma · local observation").setMtu(1500)
            .addAddress("10.111.0.2",32).addRoute("0.0.0.0",0).addDnsServer("10.111.0.1")
            // Observation is not a traffic lockdown. Android's automatic DNS selection needs
            // bypassable policy on the API 35 emulator; explicit-network apps may bypass us.
            .addDisallowedApplication(packageName).allowBypass()
            .allowFamily(OsConstants.AF_INET6).setUnderlyingNetworks(arrayOf(network)).establish()
        val fd=tunnel ?: error("VPN consent lost")
        check(hev.htproxy.TProxyService.TProxyStartService(configuration.absolutePath,fd.fd))
        packetsSeen.set(0);packetsRejected.set(0)
        active=true;running=true;status="Observing standard DNS • app ownership unknown"
        callback=object: ConnectivityManager.NetworkCallback() {
            override fun onLost(n: Network) { if(n==network) networkChanged() }
            override fun onLinkPropertiesChanged(n: Network,lp: LinkProperties) {
                if(n==network && (lp.dnsServers != links.dnsServers || lp.privateDnsServerName != links.privateDnsServerName || lp.isPrivateDnsActive != links.isPrivateDnsActive)) networkChanged()
            }
            private fun networkChanged() { endSession(fd,"Network changed. Restart DNS observation.") }
        }.also { cm.registerNetworkCallback(NetworkRequest.Builder().addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN).build(),it) }
        defaultCallback=object: ConnectivityManager.NetworkCallback() {
            override fun onAvailable(n: Network) {
                val c=cm.getNetworkCapabilities(n) ?: return
                if(n!=network && !c.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) endSession(fd,"Network changed. Restart DNS observation.")
            }
        }.also { cm.registerDefaultNetworkCallback(it) }
        reader=Thread({
            try {
                while(active && tunnel === fd) {
                    Thread.sleep(500)
                    if(!hev.htproxy.TProxyService.TProxyIsRunning()) {
                        endSession(fd,"Local forwarding stopped. Restart observation.");break
                    }
                }
            } catch (_: InterruptedException) { }
        },"noma-vpn-health").also { it.start() }
    }
    private fun endSession(fd: ParcelFileDescriptor, reason: String) {
        main.post { if(active && tunnel === fd) { status=reason;stopCapture() } }
    }
    override fun onRevoke() { main.post { status="VPN permission revoked. DNS observation stopped.";stopCapture() } }
    @Synchronized private fun cleanup() {
        active=false;running=false;generation++
        store.fencePendingWrites()
        if(status.startsWith("Observing")) status="DNS observation is off"
        callback?.let { runCatching { cm.unregisterNetworkCallback(it) } };callback=null
        defaultCallback?.let { runCatching { cm.unregisterNetworkCallback(it) } };defaultCallback=null
        runCatching { relay?.close() };relay=null
        if(tunnel!=null) runCatching { hev.htproxy.TProxyService.TProxyStopService() }
        config?.delete();config=null
        runCatching { tunnel?.close() };tunnel=null
        reader?.interrupt();reader=null
        stopForeground(STOP_FOREGROUND_REMOVE)
    }
    private fun stopCapture() { cleanup();stopSelf() }
    override fun onDestroy() { cleanup();super.onDestroy() }
    companion object {
        fun requestStop(context: android.content.Context) {
            context.startService(Intent(context,DnsVpnService::class.java).setAction("STOP"))
        }
        val packetsSeen=java.util.concurrent.atomic.AtomicLong()
        val packetsRejected=java.util.concurrent.atomic.AtomicLong()
        @Volatile var running=false
        @Volatile var status="DNS observation is off"
    }
}
