package org.noma

import org.junit.Assert.*
import org.junit.Test
import java.net.URI

class PrivacyRequestTest {
    private val profile=PrivacyDirectory.profiles.first()
    @Test fun preparingDoesNotSubmitOrClaimAnOrganisationHoldsData() {
        RequestKind.entries.forEach { kind ->
            val request=PrivacyRequest.prepare(profile,kind,100)
            assertEquals(RequestStatus.PREPARED,request.status)
            assertTrue(request.body.contains("["))
            assertFalse(request.body.contains("Noma observed"))
            assertFalse(request.body.contains("graph.facebook.com"))
        }
        assertNull(PrivacyDirectory.find("Unknown Company"))
    }
    @Test fun completionNeedsSubmissionAndAReference() {
        val draft=PrivacyRequest.prepare(profile,RequestKind.DELETE,100)
        assertThrows(IllegalArgumentException::class.java) { draft.revised(draft.body,RequestStatus.COMPLETED,"receipt",101) }
        assertThrows(IllegalArgumentException::class.java) { draft.revised(draft.body,RequestStatus.SUBMITTED,"",101) }
        val submitted=draft.revised(draft.body,RequestStatus.SUBMITTED,"Case 42, submitted on test date",101)
        assertThrows(IllegalArgumentException::class.java) { submitted.revised(submitted.body,RequestStatus.COMPLETED,"",102) }
        val complete=submitted.revised(submitted.body,RequestStatus.COMPLETED,"Provider response on test date",102)
        assertEquals(3,complete.history.size)
        assertTrue(complete.status.label.contains("not verified"))
        assertEquals("Provider response on test date",complete.history.last().reference)
    }
    @Test fun sentWordingAndHistoryCannotBeSilentlyChanged() {
        val draft=PrivacyRequest.prepare(profile,RequestKind.ACCESS,100)
        val edited=draft.revised("Account: test\nMy request",RequestStatus.PREPARED,"",101)
        assertEquals(draft.history,edited.history)
        val sent=edited.revised(edited.body,RequestStatus.SUBMITTED,"Receipt 1",102)
        assertThrows(IllegalArgumentException::class.java) { sent.revised("different",sent.status,"",103) }
        assertThrows(IllegalArgumentException::class.java) { sent.revised(sent.body,RequestStatus.PREPARED,"reset",103) }
    }
    @Test fun directoryRoutesAreOfficialHttpsAndAutomationIsDisabled() {
        val hosts=setOf("policies.google.com","myadcenter.google.com","www.facebook.com","advertising.amazon.com")
        PrivacyDirectory.profiles.forEach { p ->
            assertFalse(p.automationSupported)
            listOfNotNull(p.policyUrl,p.choicesUrl).forEach { url ->
                val uri=URI(url);assertEquals("https",uri.scheme);assertTrue(uri.host in hosts)
                assertNull(uri.userInfo);assertNull(uri.query)
            }
        }
    }
}
