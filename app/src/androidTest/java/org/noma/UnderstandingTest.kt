package org.noma

import android.content.Intent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Fixtures are confined to the dedicated test device and erased even on failure. */
@RunWith(AndroidJUnit4::class)
class UnderstandingTest {
    @Test fun organisationToDomainToOriginalEvidenceAndUnknownView() {
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        val context=instrumentation.targetContext
        instrumentation.uiAutomation.executeShellCommand("input keyevent KEYCODE_WAKEUP").close()
        instrumentation.uiAutomation.executeShellCommand("wm dismiss-keyguard").close()
        val store=LocalStore.get(context)
        store.erase()
        val now=System.currentTimeMillis()
        store.add(Event("m1-known",now,"DNS_REQUEST","graph.facebook.com","M1 UI test fixture"),store.epoch())
        store.add(Event("m1-unknown",now,"DNS_REQUEST","unlisted.invalid","M1 UI test fixture"),store.epoch())
        val activity=instrumentation.startActivitySync(Intent(context,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        fun find(n: AccessibilityNodeInfo, text: String): AccessibilityNodeInfo? {
            if(n.text?.toString()?.contains(text)==true) return n
            for(i in 0 until n.childCount) n.getChild(i)?.let { find(it,text) }?.let { return it }
            return null
        }
        fun scroll(n: AccessibilityNodeInfo, action: Int): Boolean {
            if(n.isScrollable) return n.performAction(action)
            for(i in 0 until n.childCount) if(n.getChild(i)?.let { scroll(it,action) }==true) return true
            return false
        }
        fun node(text: String): AccessibilityNodeInfo {
            val deadline=System.currentTimeMillis()+10000
            var direction=AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
            var attempts=0
            while(System.currentTimeMillis()<deadline) {
                instrumentation.uiAutomation.rootInActiveWindow?.let { root ->
                    find(root,text)?.let { return it }
                    if(++attempts>3 && !scroll(root,direction)) direction=if(direction==AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
                        AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD else AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
                }
                Thread.sleep(300)
            }
            fun describe(n: AccessibilityNodeInfo): String = "[${n.text}; scroll=${n.isScrollable}]" +
                (0 until n.childCount).joinToString { n.getChild(it)?.let { child -> describe(child) } ?: "" }
            throw AssertionError("UI text not found: $text; ${instrumentation.uiAutomation.rootInActiveWindow?.let { describe(it) }}")
        }
        fun click(text: String) {
            var target: AccessibilityNodeInfo?=node(text)
            while(target!=null && !target.isClickable) target=target.parent
            assertTrue("Clickable control: $text",target?.performAction(AccessibilityNodeInfo.ACTION_CLICK)==true)
            instrumentation.waitForIdleSync()
            Thread.sleep(350) // Allow Compose's dialog/expand transition to finish before inspecting its tree.
        }
        try {
            click("Organisations")
            node("Understand your trail")
            node("Names without an organisation match: 1")
            val owner=RegistryLoader.load(context).lookup("graph.facebook.com")!!.organisation
            click(owner)
            click("graph.facebook.com")
            node("Version: 06c65bd1e7f214bca9553fb2430d2eae040276b2")
            click("m1-known")
            node("M1 UI test fixture")
            node("Originating app: UNKNOWN")
            click("Close")
            // Collapse the known record so the unknown record is visible on the small display.
            click(owner)
            click("unlisted.invalid")
            node("No exact dataset match")
        } finally {
            activity.finish()
            store.erase()
        }
    }
}
