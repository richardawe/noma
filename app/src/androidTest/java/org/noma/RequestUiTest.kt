package org.noma

import android.content.Intent
import android.os.Bundle
import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RequestUiTest {
    @Test fun prepareAndTrackOfflineWithoutSubmittingAnything() {
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        val context=instrumentation.targetContext
        fun shell(command: String)=instrumentation.uiAutomation.executeShellCommand(command).use { fd ->
            java.io.FileInputStream(fd.fileDescriptor).bufferedReader().use { it.readText().trim() }
        }
        shell("input keyevent KEYCODE_WAKEUP");shell("wm dismiss-keyguard")
        val activity=instrumentation.startActivitySync(Intent(context,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        DnsVpnService.requestStop(context)
        fun until(check: () -> Boolean) {
            val end=System.currentTimeMillis()+12000
            while(!check() && System.currentTimeMillis()<end) Thread.sleep(100)
            assertTrue(check())
        }
        until { !DnsVpnService.running }
        val store=LocalStore.get(context);store.erase()
        val wasAirplane=shell("settings get global airplane_mode_on")=="1"
        fun find(n: AccessibilityNodeInfo,text: String,clickable: Boolean): AccessibilityNodeInfo? {
            if(n.text?.toString()?.contains(text)==true) {
                var parent: AccessibilityNodeInfo?=n
                while(parent!=null && !parent.isClickable) parent=parent.parent
                if(!clickable || parent?.isEnabled==true) return n
            }
            for(i in 0 until n.childCount) n.getChild(i)?.let { find(it,text,clickable) }?.let { return it }
            return null
        }
        fun scroll(n: AccessibilityNodeInfo,action: Int): Boolean {
            if(n.isScrollable && n.performAction(action)) return true
            for(i in 0 until n.childCount) if(n.getChild(i)?.let { scroll(it,action) }==true) return true
            return false
        }
        fun node(text: String,clickable: Boolean=false): AccessibilityNodeInfo {
            var direction=AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
            val end=System.currentTimeMillis()+14000
            while(System.currentTimeMillis()<end) {
                instrumentation.uiAutomation.rootInActiveWindow?.let { root ->
                    find(root,text,clickable)?.let { return it }
                    if(!scroll(root,direction)) direction=if(direction==AccessibilityNodeInfo.ACTION_SCROLL_FORWARD) AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD else AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
                }
                Thread.sleep(350)
            }
            throw AssertionError("UI text not found: $text")
        }
        fun click(text: String) {
            var target: AccessibilityNodeInfo?=node(text,true)
            while(target!=null && !target.isClickable) target=target.parent
            assertTrue(target?.performAction(AccessibilityNodeInfo.ACTION_CLICK)==true)
            instrumentation.waitForIdleSync();Thread.sleep(350)
        }
        fun reference(value: String) {
            node("Submission or response reference")
            fun edits(n: AccessibilityNodeInfo): List<AccessibilityNodeInfo> =
                (if(n.isEditable) listOf(n) else emptyList())+(0 until n.childCount).flatMap { n.getChild(it)?.let { child -> edits(child) } ?: emptyList() }
            val input=edits(instrumentation.uiAutomation.rootInActiveWindow!!).last()
            assertTrue(input.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT,Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,value)
            }))
            Thread.sleep(350)
        }
        try {
            shell("cmd connectivity airplane-mode enable")
            val cm=context.getSystemService(android.net.ConnectivityManager::class.java)
            until { cm.activeNetwork==null }
            click("Take Back")
            node("Take Back My Data")
            click("Google LLC")
            node("Prepare a privacy request")
            assertTrue(store.requests().isEmpty())
            click("Save locally")
            until { store.requests().size==1 }
            assertEquals(RequestStatus.PREPARED,store.requests().single().status)
            click("Review draft and request history")
            click("Prepared")
            click("Submitted — recorded by you")
            assertEquals(RequestStatus.PREPARED,store.requests().single().status)
            reference("Test-only receipt 42")
            click("Save locally")
            until { store.requests().single().status==RequestStatus.SUBMITTED }
            click("Review draft and request history")
            click("Submitted — recorded by you")
            click("Completion reported — not verified by Noma")
            reference("Test-only provider reply 43")
            click("Save locally")
            until { store.requests().single().status==RequestStatus.COMPLETED }
            assertEquals(3,store.requests().single().history.size)
            node("Completion reported — not verified by Noma")
            click("Review draft and request history")
            click("Remove local request record")
            click("Remove record")
            until { store.requests().isEmpty() }
            assertNull(cm.activeNetwork) // No outside form, email or API submission was invoked.
        } finally {
            store.erase();activity.finish()
            if(!wasAirplane) shell("cmd connectivity airplane-mode disable")
        }
    }
}
