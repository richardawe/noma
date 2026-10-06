package org.noma

import android.content.Intent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.ZoneId

@RunWith(AndroidJUnit4::class)
class DaySummaryUiTest {
    @Test fun dailySummaryWorksOfflineAndOpensOriginalEvidence() {
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        val context=instrumentation.targetContext
        fun shell(command: String): String = instrumentation.uiAutomation.executeShellCommand(command).use { fd ->
            java.io.FileInputStream(fd.fileDescriptor).bufferedReader().use { it.readText().trim() }
        }
        shell("input keyevent KEYCODE_WAKEUP");shell("wm dismiss-keyguard")
        val activity=instrumentation.startActivitySync(Intent(context,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        DnsVpnService.requestStop(context)
        val stopDeadline=System.currentTimeMillis()+5000
        while(DnsVpnService.running && System.currentTimeMillis()<stopDeadline) Thread.sleep(100)
        assertFalse(DnsVpnService.running)
        val store=LocalStore.get(context);store.erase()
        val now=System.currentTimeMillis()
        val event=Event("day-fixture",now,"DNS_REQUEST","graph.facebook.com","M2 offline test fixture")
        store.add(event,store.epoch())
        val wasAirplane=shell("settings get global airplane_mode_on")=="1"
        fun find(n: AccessibilityNodeInfo,text: String,clickable: Boolean): AccessibilityNodeInfo? {
            if(n.isVisibleToUser && n.text?.toString()?.contains(text)==true) {
                var control: AccessibilityNodeInfo?=n
                while(control!=null && !control.isClickable) control=control.parent
                if(!clickable || control?.isEnabled==true) return n
            }
            for(i in 0 until n.childCount) n.getChild(i)?.let { find(it,text,clickable) }?.let { return it }
            return null
        }
        fun scroll(n: AccessibilityNodeInfo,action: Int): Boolean {
            if(n.isScrollable) return n.performAction(action)
            for(i in 0 until n.childCount) if(n.getChild(i)?.let { scroll(it,action) }==true) return true
            return false
        }
        fun node(text: String,clickable: Boolean=false): AccessibilityNodeInfo {
            val deadline=System.currentTimeMillis()+16000
            var direction=AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
            var attempts=0
            while(System.currentTimeMillis()<deadline) {
                instrumentation.uiAutomation.rootInActiveWindow?.let { root ->
                    find(root,text,clickable)?.let { return it }
                    if(++attempts>3) {
                        if(attempts%4==0) direction=if(direction==AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
                            AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD else AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
                        scroll(root,direction)
                    }
                }
                Thread.sleep(300)
            }
            throw AssertionError("UI text not found: $text")
        }
        fun click(text: String) {
            // Scrolling/recomposition can invalidate a node between discovery and its action.
            // Reacquire it rather than treating a stale accessibility handle as an app failure.
            repeat(3) {
                var target: AccessibilityNodeInfo?=node(text,true)
                while(target!=null && !target.isClickable) target=target.parent
                if(target?.performAction(AccessibilityNodeInfo.ACTION_CLICK)==true) {
                    instrumentation.waitForIdleSync();Thread.sleep(350);return
                }
                Thread.sleep(200)
            }
            throw AssertionError("Could not click visible control: $text")
        }

        try {
            shell("cmd connectivity airplane-mode enable")
            val cm=context.getSystemService(android.net.ConnectivityManager::class.java)
            val deadline=System.currentTimeMillis()+10000
            while(cm.activeNetwork!=null && System.currentTimeMillis()<deadline) Thread.sleep(100)
            assertNull(cm.activeNetwork)
            // Activity refresh reads the fixture locally; no network is available.
            node("Here is what Noma recorded.")
            click("Noma recorded 1 DNS request for 1 distinct name.")
            node("Summary evidence")
            click("day-fixt")
            node("M2 offline test fixture")
            node("Originating app: UNKNOWN")
            click("Close")
            click("Play My Day")
            click("Pause")
            node("1 of 3 · Paused")
            click("Play")
            node("2 of 3 · Playing") // Timed advancement, not just manual navigation.
            click("Pause")
            click("Back")
            node("1 of 3 · Paused")
            click("Next")
            node("Recorded activity")
            click("Inspect playback evidence")
            click("day-fixt")
            node("Original playback record")
            node("M2 offline test fixture")
            node("Originating app: UNKNOWN")
            click("Return to playback")
            click("Skip to recap")
            node("Your day,\nin evidence.")
            click("Replay")
            click("Pause")
            node("1 of 3 · Paused")
            click("Close playback")
            click("Yesterday")
            node("Your summary is waiting for evidence.")
            node("Partial day")
            val zone=ZoneId.systemDefault()
            val date=Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
            val summary=DailySummarizer.build(store.read(),RegistryLoader.load(context),date,zone)
            assertEquals(event,summary.evidence(summary.claims.first()).single())
            click("Today")
            click("Play My Day")
            click("Pause")
            store.erase()
            node("Your summary is waiting for evidence.") // Erasure closes an open playback snapshot.
            assertTrue(DailySummarizer.build(store.read(),RegistryLoader.load(context),date,zone).claims.isEmpty())
        } finally {
            store.erase();activity.finish()
            if(!wasAirplane) shell("cmd connectivity airplane-mode disable")
        }
    }
}
