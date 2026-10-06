package org.noma

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StorageTest {
    @Test fun fullRetentionLimitAndFailedBatchStayConsistent() {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val store=LocalStore.get(context);store.erase()
        try {
            val token=store.epoch();val now=System.currentTimeMillis()
            val records=(0..20004).map { Event("load-$it",now-25000+it,"DNS_REQUEST","load.invalid","load fixture") }
            records.chunked(512).forEach { assertTrue(store.addBatch(it,token)) }
            val retained=store.read()
            assertEquals(20000,retained.size);assertEquals("load-20004",retained.first().id);assertEquals("load-5",retained.last().id)
            assertTrue(store.addBatch(records.takeLast(5),token));assertEquals(20000,store.read().size)
            // Force a write failure halfway through a batch. The earlier insert must roll back too.
            val db=context.openOrCreateDatabase("events.db",0,null)
            db.execSQL("CREATE TRIGGER fail_fixture BEFORE INSERT ON events WHEN NEW.id='fail-write' BEGIN SELECT RAISE(ABORT,'fixture'); END")
            assertThrows(android.database.sqlite.SQLiteException::class.java) {
                store.addBatch(listOf(records.last().copy(id="before-failure"),records.last().copy(id="fail-write")),token)
            }
            db.execSQL("DROP TRIGGER fail_fixture");db.close()
            assertFalse(store.read().any { it.id=="before-failure" })
            store.fencePendingWrites();assertFalse(store.addBatch(records.take(5),token))
        } finally { store.erase() }
    }
    @Test fun encryptedRoundtripRetentionAndErasureFence() {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val store=LocalStore.get(context);store.erase()
        val token=store.epoch();val now=System.currentTimeMillis()
        val event=Event("fixture",now,"DNS_REQUEST","private-fixture.example","instrumentation fixture",null)
        assertTrue(store.add(event,token));assertEquals(listOf(event),store.read())
        val bytes=context.getDatabasePath("events.db").readBytes()
        assertFalse(String(bytes,Charsets.ISO_8859_1).contains("private-fixture.example"))
        store.add(event.copy(id="expired",time=now-90_000_000),token)
        assertEquals(1,store.read().size)
        store.erase()
        assertFalse(store.add(event,token));assertTrue(store.read().isEmpty());assertFalse(store.usageEnabled())
        assertFalse(store.add(event.copy(kind="APP_RESUMED"),store.epoch()))
    }
    @Test fun bundledRegistryHasExpectedProvenance() {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val result=RegistryLoader.load(context).lookup("graph.facebook.com")
        assertNotNull(result);assertTrue(result!!.source.startsWith("https://raw.githubusercontent.com/duckduckgo/"))
        assertEquals(40,result.version.length)
    }
    @Test fun corruptDatasetAndCiphertextFailClosed() {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val bytes=context.assets.open("registry.json").use { it.readBytes() }
        assertNull(RegistryLoader.decode(bytes,"wrong digest").lookup("graph.facebook.com"))
        val store=LocalStore.get(context);store.erase()
        store.add(Event("tamper",System.currentTimeMillis(),"DNS_REQUEST","fixture.invalid","fixture"),store.epoch())
        val db=context.openOrCreateDatabase("events.db",0,null)
        db.execSQL("UPDATE events SET payload = ?",arrayOf(ByteArray(32)))
        db.close()
        var rejected=false
        try { store.read() } catch (_: Exception) { rejected=true }
        assertTrue("Corrupt evidence must not be rendered",rejected)
        store.erase();assertTrue(store.read().isEmpty())
    }

    @Test fun evidenceAndClassificationRemainAvailableOffline() {
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        val context=instrumentation.targetContext
        fun shell(command: String): String = instrumentation.uiAutomation.executeShellCommand(command).use { fd ->
            java.io.FileInputStream(fd.fileDescriptor).bufferedReader().use { it.readText().trim() }
        }
        val wasAirplane=shell("settings get global airplane_mode_on")=="1"
        val store=LocalStore.get(context);store.erase()
        val event=Event("offline-fixture",System.currentTimeMillis(),"DNS_REQUEST","graph.facebook.com","test fixture, not live observation")
        store.add(event,store.epoch())
        try {
            shell("cmd connectivity airplane-mode enable")
            val cm=context.getSystemService(android.net.ConnectivityManager::class.java)
            val deadline=System.currentTimeMillis()+10000
            while(cm.activeNetwork!=null && System.currentTimeMillis()<deadline) Thread.sleep(100)
            assertNull("Test requires radios offline",cm.activeNetwork)
            assertEquals(listOf(event),store.read())
            assertNotNull(RegistryLoader.load(context).lookup(event.value))
            val graph=PersonalDataGraph.build(store.read(),RegistryLoader.load(context))
            assertEquals(event,graph.organisations.single().domains.single().events.single())
            assertTrue(graph.unknown.isEmpty())
        } finally {
            if(!wasAirplane) shell("cmd connectivity airplane-mode disable")
            store.erase()
            assertTrue(PersonalDataGraph.build(store.read(),RegistryLoader.load(context)).organisations.isEmpty())
        }
    }

}
