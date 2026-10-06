package org.noma

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Software renders of this test's UI only. Production FLAG_SECURE stays enabled. */
@RunWith(AndroidJUnit4::class)
class DesignReviewTest {
    @Test fun renderEmptyAndPopulatedScreensWithPersistentNavigation() {
        val i=InstrumentationRegistry.getInstrumentation();val context=i.targetContext
        val store=LocalStore.get(context)
        DnsVpnService.requestStop(context);Thread.sleep(1000);store.erase()
        val activity=i.startActivitySync(Intent(context,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        fun find(n: AccessibilityNodeInfo,text: String): AccessibilityNodeInfo? {
            if(n.text?.toString()?.contains(text)==true) return n
            for(j in 0 until n.childCount) n.getChild(j)?.let { find(it,text) }?.let { return it }
            return null
        }
        fun node(text: String): AccessibilityNodeInfo {
            val deadline=System.currentTimeMillis()+16000
            while(System.currentTimeMillis()<deadline) {
                i.uiAutomation.rootInActiveWindow?.let { find(it,text) }?.let { return it };Thread.sleep(200)
            }
            error("UI not found: $text")
        }
        fun click(text: String) {
            var n: AccessibilityNodeInfo?=node(text)
            while(n!=null && !n.isClickable) n=n.parent
            assertTrue(n?.performAction(AccessibilityNodeInfo.ACTION_CLICK)==true);Thread.sleep(700)
        }
        fun render(name: String) {
            i.runOnMainSync {
                val view=activity.window.decorView
                val bitmap=Bitmap.createBitmap(view.width,view.height,Bitmap.Config.ARGB_8888)
                view.draw(Canvas(bitmap))
                context.openFileOutput("design-$name.png",0).use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it) }
                bitmap.recycle()
                assertTrue(activity.window.attributes.flags and android.view.WindowManager.LayoutParams.FLAG_SECURE != 0)
            }
        }
        try {
            node("Choose what to record");render("empty")
            val now=System.currentTimeMillis()
            store.addBatch(listOf(Event("design-only-1",now,"DNS_REQUEST","graph.facebook.com","Design review fixture"),
                Event("design-only-2",now,"DNS_REQUEST","www.google-analytics.com","Design review fixture")),store.epoch())
            node("Play My Day");render("day")
            click("Organisations");node("Understand your trail");render("organisations")
            click("Take Back");node("Take Back My Data");render("requests")
            click("Controls");node("What Noma can see");render("controls")
            click("Timeline");node("Your digital trail");render("trail")
        } finally { activity.finish();store.erase() }
    }
}
