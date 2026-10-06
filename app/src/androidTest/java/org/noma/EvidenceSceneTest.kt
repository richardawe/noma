package org.noma

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.ZoneOffset

@RunWith(AndroidJUnit4::class)
class EvidenceSceneTest {
    @Test fun bundledPageReceivesRealSnapshotAndReturnsToNativeEvidence() {
        org.junit.Assume.assumeTrue("Dedicated emulator only",android.os.Build.HARDWARE == "ranchu")
        val instrumentation=androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
        val context=instrumentation.targetContext
        val store=LocalStore.get(context)
        DnsVpnService.requestStop(context)
        store.erase()
        val now=System.currentTimeMillis()
        store.add(Event("scene-native-fixture",now,"DNS_REQUEST","example.test","SCENE LOCAL TEST FIXTURE"),store.epoch())
        val activity=instrumentation.startActivitySync(android.content.Intent(context,MainActivity::class.java).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
        fun find(node: android.view.accessibility.AccessibilityNodeInfo, text: String): android.view.accessibility.AccessibilityNodeInfo? {
            if(node.text?.toString()?.contains(text)==true) return node
            for(i in 0 until node.childCount) node.getChild(i)?.let { find(it,text) }?.let { return it }
            return null
        }
        fun waitFor(test: ()->Boolean) {
            val deadline=System.currentTimeMillis()+30_000
            while(System.currentTimeMillis()<deadline) { if(test()) return;Thread.sleep(250) }
            fail("Timed out waiting for local scene")
        }
        fun web(view:android.view.View): android.webkit.WebView? {
            if(view is android.webkit.WebView) return view
            if(view is android.view.ViewGroup) for(i in 0 until view.childCount) web(view.getChildAt(i))?.let { return it }
            return null
        }
        try {
            waitFor {
                var button=instrumentation.uiAutomation.rootInActiveWindow?.let { find(it,"Explore Your Day in 3D") }
                while(button!=null && !button.isClickable) button=button.parent
                button?.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK)==true
            }
            var browser:android.webkit.WebView?=null
            waitFor { instrumentation.runOnMainSync { browser=web(activity.window.decorView) };browser!=null }
            fun js(script:String):String {
                val latch=java.util.concurrent.CountDownLatch(1);var result=""
                instrumentation.runOnMainSync { browser!!.evaluateJavascript(script) { result=it;latch.countDown() } }
                assertTrue(latch.await(5,java.util.concurrent.TimeUnit.SECONDS));return result
            }
            waitFor { js("document.querySelector('h1')?.textContent || ''").contains("Your day, in perspective.") }
            waitFor { js("window.nomaSnapshot?.events.some(e=>e.id==='scene-native-fixture') || false")=="true" }
            js("window.location.href='https://noma.local/evidence?id=scene-native-fixture'")
            waitFor { instrumentation.uiAutomation.rootInActiveWindow?.let { find(it,"Why is this here?") }!=null }
            assertTrue(instrumentation.uiAutomation.rootInActiveWindow?.let { find(it,"SCENE LOCAL TEST FIXTURE") }!=null)
            assertTrue(activity.window.attributes.flags and android.view.WindowManager.LayoutParams.FLAG_SECURE != 0)
        } finally {
            instrumentation.runOnMainSync { activity.finish() }
            store.erase()
        }
    }

    @Test fun snapshotKeepsEvidenceAsDataAndDropsUnreadableRecords() {
        val now=System.currentTimeMillis()
        val event=Event("scene-fixture",now,"DNS_REQUEST","example.test","\"</script>\\\nTEST FIXTURE")
        val registry=DomainRegistry(listOf(Classification("example.test","Fixture owner","test","fixture registry","1")))
        val summary=DailySummarizer.build(listOf(event),registry,Instant.ofEpochMilli(now).atZone(ZoneOffset.UTC).toLocalDate(),ZoneOffset.UTC,now)
        val data=JSONObject(sceneSnapshot(summary,true,true,"Observing standard DNS",""))
        assertEquals(event.source,data.getJSONArray("events").getJSONObject(0).getString("source"))
        assertEquals("Fixture owner",data.getJSONArray("matches").getJSONObject(0).getString("organisation"))
        assertFalse(data.has("requests"))
        val failed=JSONObject(sceneSnapshot(summary,false,false,"","Local data could not be read."))
        assertEquals(0,failed.getJSONArray("events").length())
        assertEquals(0,failed.getJSONArray("matches").length())
    }
}
