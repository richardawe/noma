package org.noma

import android.content.*
import android.net.*
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.*

/** Dedicated emulator: real traffic originates in the separate test APK UID. */
@RunWith(AndroidJUnit4::class)
class CollectionTest {
    private val instrumentation=InstrumentationRegistry.getInstrumentation()
    private val context=instrumentation.targetContext
    private fun shell(command: String): String=instrumentation.uiAutomation.executeShellCommand(command).use { fd ->
        java.io.FileInputStream(fd.fileDescriptor).bufferedReader().use { it.readText().trim() }
    }
    private fun until(check: () -> Boolean) {
        val deadline=System.currentTimeMillis()+20_000
        while(!check() && System.currentTimeMillis()<deadline) Thread.sleep(100)
        assertTrue("Condition failed: ${DnsVpnService.status}",check())
    }
    private fun probe(mode: String) {
        val done=CountDownLatch(1);var success=false;var detail=""
        val receiver=object: BroadcastReceiver() {
            override fun onReceive(c: Context,i: Intent) { success=i.getBooleanExtra("success",false);detail=i.getStringExtra("detail") ?: "";done.countDown() }
        }
        context.registerReceiver(receiver,IntentFilter("org.noma.test.DNS_RESULT"),Context.RECEIVER_EXPORTED)
        try {
            shell("am start -n org.noma.test/org.noma.ResolverProbeActivity --ez async_dns ${mode.startsWith("async")} --es mode $mode")
            assertTrue("$mode timed out",done.await(30,TimeUnit.SECONDS))
            assertTrue("$mode: $detail; ${DnsVpnService.status}; seen=${DnsVpnService.packetsSeen}",success)
        } finally { context.unregisterReceiver(receiver) }
    }
    @Test fun usageConsentRevocationAndFreshOptInDoNotImportHistory() {
        val previous=Regex("GET_USAGE_STATS: (\\w+)").find(shell("appops get org.noma GET_USAGE_STATS"))?.groupValues?.get(1) ?: "default"
        val store=LocalStore.get(context);store.erase()
        val activity=instrumentation.startActivitySync(Intent(context,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        try {
            shell("appops set org.noma GET_USAGE_STATS allow")
            val since=System.currentTimeMillis();store.enableUsage(true)
            shell("am start -a android.settings.SETTINGS");Thread.sleep(1200)
            shell("am start -n org.noma/.MainActivity");Thread.sleep(1200)
            UsageCollector.collect(context)
            assertTrue(store.read().any { it.kind=="APP_RESUMED" && it.value=="com.android.settings" })
            assertTrue(store.read().all { it.time>=since })
            shell("appops set org.noma GET_USAGE_STATS ignore");UsageCollector.collect(context)
            assertFalse(store.usageEnabled())
            store.erase();shell("appops set org.noma GET_USAGE_STATS allow")
            val fresh=System.currentTimeMillis();store.enableUsage(true);UsageCollector.collect(context)
            assertTrue(store.read().all { it.time>=fresh })
        } finally { store.erase();activity.finish();shell("appops set org.noma GET_USAGE_STATS $previous") }
    }
    @Test fun asyncResolverBaselineWithoutVpn() {
        DnsVpnService.requestStop(context);until { !DnsVpnService.running }
        Thread.sleep(1000);probe("async")
    }
    @Test fun realResolversTcpDnsAndHttpsWorkWithLocalForwarding() {
        shell("appops set org.noma ACTIVATE_VPN allow")
        val previousDns=shell("settings get global private_dns_mode")
        shell("settings put global private_dns_mode off")
        val activity=instrumentation.startActivitySync(Intent(context,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        val store=LocalStore.get(context)
        DnsVpnService.requestStop(context);until { !DnsVpnService.running };store.erase()
        try {
            Thread.sleep(1500)
            assertNull(VpnService.prepare(context))
            activity.runOnUiThread { context.startForegroundService(Intent(context,DnsVpnService::class.java)) }
            until { DnsVpnService.running };Thread.sleep(2000)
            val failures=mutableListOf<String>()
            for(mode in listOf("udp_dns","tcp_dns","async","async_a","async_explicit","normal","https")) try {
                val before=store.read().count { it.value=="www.google-analytics.com" }
                probe(mode)
                if(mode.startsWith("async")) assertTrue("$mode must produce observed evidence",store.read().count { it.value=="www.google-analytics.com" }>before)
            } catch(e: AssertionError) { failures+=e.message ?: mode }
            assertTrue(failures.joinToString("\n"),failures.isEmpty())
            val records=store.read()
            assertTrue(records.any { it.value=="graph.facebook.com" })
            assertTrue(records.any { it.value=="www.google-analytics.com" })
            assertTrue(records.all { it.uid==null })
            DnsVpnService.requestStop(context);until { !DnsVpnService.running }
            val count=store.read().size
            probe("https");assertEquals(count,store.read().size)
            repeat(3) {
                shell("am start -n org.noma/.MainActivity")
                activity.runOnUiThread { context.startForegroundService(Intent(context,DnsVpnService::class.java)) }
                until { DnsVpnService.running };Thread.sleep(2000);probe("async")
                DnsVpnService.requestStop(context);until { !DnsVpnService.running }
            }
        } finally {
            DnsVpnService.requestStop(context);until { !DnsVpnService.running };store.erase();activity.finish()
            if(previousDns=="null") shell("settings delete global private_dns_mode") else shell("settings put global private_dns_mode $previousDns")
        }
    }
    @Test fun networkLossAndRevocationStopCleanlyWithoutAutomaticRestart() {
        val previousDns=shell("settings get global private_dns_mode")
        val previousAirplane=shell("settings get global airplane_mode_on")=="1"
        shell("cmd connectivity airplane-mode disable");shell("settings put global private_dns_mode off")
        shell("appops set org.noma ACTIVATE_VPN allow")
        val activity=instrumentation.startActivitySync(Intent(context,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        val store=LocalStore.get(context);DnsVpnService.requestStop(context);until { !DnsVpnService.running };store.erase()
        val cm=context.getSystemService(ConnectivityManager::class.java)
        fun start() { activity.runOnUiThread { context.startForegroundService(Intent(context,DnsVpnService::class.java)) };until { DnsVpnService.running };Thread.sleep(2000) }
        try {
            until { cm.activeNetwork!=null };Thread.sleep(1500);start();probe("async")
            shell("cmd connectivity airplane-mode enable");until { !DnsVpnService.running }
            val count=store.read().size
            shell("cmd connectivity airplane-mode disable");until { cm.activeNetwork!=null };Thread.sleep(1500)
            assertFalse(DnsVpnService.running);assertEquals(count,store.read().size)
            shell("am start -n org.noma/.MainActivity");start();probe("async")
            shell("appops set org.noma.test ACTIVATE_VPN allow")
            probe("revoke") // Preparing another authorised VPN invokes the real system revoke path.
            until { !DnsVpnService.running };Thread.sleep(1000);assertFalse(DnsVpnService.running)
        } finally {
            DnsVpnService.requestStop(context);until { !DnsVpnService.running };store.erase();activity.finish()
            shell("appops set org.noma ACTIVATE_VPN allow")
            shell("appops set org.noma.test ACTIVATE_VPN default")
            if(previousAirplane) shell("cmd connectivity airplane-mode enable") else shell("cmd connectivity airplane-mode disable")
            if(previousDns=="null") shell("settings delete global private_dns_mode") else shell("settings put global private_dns_mode $previousDns")
        }
    }
}
