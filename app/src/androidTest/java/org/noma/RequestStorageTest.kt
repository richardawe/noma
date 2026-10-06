package org.noma

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RequestStorageTest {
    @Test fun draftEditsCannotOverwriteOrResurrectAndCollectorStopDoesNotInvalidateThem() {
        val store=LocalStore.get(InstrumentationRegistry.getInstrumentation().targetContext);store.erase()
        try {
            val token=store.erasureEpoch()
            val original=PrivacyRequest.prepare(PrivacyDirectory.profiles.first(),RequestKind.ACCESS,System.currentTimeMillis())
            store.saveRequest(original,token)
            store.fencePendingWrites()
            val edited=original.copy(body="My first edit")
            assertTrue(store.saveRequest(edited,token,original))
            assertThrows(IllegalArgumentException::class.java) { store.saveRequest(original.copy(body="Stale edit"),token,original) }
            assertEquals(edited,store.requests().single())
            store.deleteRequest(edited.id,token)
            assertThrows(IllegalArgumentException::class.java) { store.saveRequest(edited,token,edited) }
            assertTrue(store.requests().isEmpty())
        } finally { store.erase() }
    }
    @Test fun interruptedErasureFinishesBeforeReadsAndWrites() {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val store=LocalStore.get(context);store.erase()
        try {
            val old=store.epoch();val requestEpoch=store.erasureEpoch()
            val event=Event("interrupted",System.currentTimeMillis(),"DNS_REQUEST","fixture.invalid","test")
            store.add(event,old)
            store.saveRequest(PrivacyRequest.prepare(PrivacyDirectory.profiles.first(),RequestKind.ACCESS,event.time),requestEpoch)
            // Durable state at the crash boundary: intent committed, database/key not yet removed.
            context.getSharedPreferences("consent",0).edit().clear().putLong("epoch",old+1)
                .putLong("erasureEpoch",requestEpoch+1).putBoolean("erasePending",true).commit()
            assertFalse(store.add(event,old))
            assertTrue(store.requests().isEmpty());assertTrue(store.read().isEmpty())
            assertFalse(context.getSharedPreferences("consent",0).getBoolean("erasePending",true))
            assertTrue(store.add(event.copy(id="after-erasure"),store.epoch()))
            assertEquals("after-erasure",store.read().single().id)
        } finally { store.erase() }
    }
    @Test fun missingKeyDoesNotCreateAReplacementOverRequestOnlyData() {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val store=LocalStore.get(context);store.erase()
        try {
            val now=System.currentTimeMillis()
            store.saveRequest(PrivacyRequest.prepare(PrivacyDirectory.profiles.first(),RequestKind.ACCESS,now),store.erasureEpoch())
            java.security.KeyStore.getInstance("AndroidKeyStore").apply { load(null);deleteEntry("noma.events.v1") }
            assertThrows(IllegalStateException::class.java) { store.requests() }
            assertThrows(IllegalStateException::class.java) { store.add(Event("key-test",now,"DNS_REQUEST","fixture.invalid","test"),store.epoch()) }
        } finally { store.erase() }
    }
    @Test fun requestsAreEncryptedSurviveEventRetentionAndEraseWithEverything() {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val store=LocalStore.get(context);store.erase()
        try {
            val token=store.erasureEpoch()
            val draft=PrivacyRequest.prepare(PrivacyDirectory.profiles.first(),RequestKind.ACCESS,System.currentTimeMillis()-172800000)
                .copy(body="private-request-marker-49312")
            assertTrue(store.saveRequest(draft,token))
            store.purge()
            assertTrue(store.read().isEmpty())
            assertEquals(listOf(draft),store.requests())
            assertFalse(String(context.getDatabasePath("events.db").readBytes(),Charsets.ISO_8859_1).contains(draft.body))
            store.add(Event("after-request",System.currentTimeMillis(),"DNS_REQUEST","fixture.invalid","test"),store.epoch())
            assertEquals(listOf(draft),store.requests()) // Same key remains usable with request-only data.
            store.erase()
            assertTrue(store.requests().isEmpty())
            assertFalse(store.saveRequest(draft,token))
        } finally { store.erase() }
    }
    @Test fun staleEditsAndCrossTableCiphertextCannotOverwriteRequestHistory() {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val store=LocalStore.get(context);store.erase()
        try {
            val token=store.erasureEpoch();val now=System.currentTimeMillis()
            val draft=PrivacyRequest.prepare(PrivacyDirectory.profiles.first(),RequestKind.DELETE,now)
            store.saveRequest(draft,token)
            val submitted=draft.revised(draft.body,RequestStatus.SUBMITTED,"Receipt test-1",now+1)
            store.saveRequest(submitted,token,draft)
            assertThrows(IllegalArgumentException::class.java) { store.saveRequest(draft,token) }
            assertEquals(submitted,store.requests().single())
            store.add(Event("cipher",now,"DNS_REQUEST","fixture.invalid","test"),store.epoch())
            val db=context.openOrCreateDatabase("events.db",0,null)
            db.execSQL("UPDATE requests SET payload = (SELECT payload FROM events LIMIT 1)")
            db.close()
            var rejected=false
            try { store.requests() } catch (_: Exception) { rejected=true }
            assertTrue("Request AAD must reject ciphertext from an event",rejected)
        } finally { store.erase() }
    }
}
