package org.noma

import java.util.UUID

data class PrivacyProfile(val organisation: String, val policyUrl: String, val choicesUrl: String?,
                          val checked: String, val notes: String,
                          val contact: String = "Use the contact route in the official privacy policy.",
                          val deletionSupported: String = "Check the official policy for your service and region.",
                          val verification: String = "The organisation may require sign-in or identity verification.",
                          val automationSupported: Boolean = false)

/** Separate curated directory. An owner label is not proof of a relationship with the user. */
object PrivacyDirectory {
    val profiles=listOf(
        PrivacyProfile("Google LLC","https://policies.google.com/privacy","https://myadcenter.google.com/",
            "2026-09-23","Google's policy links to export, deletion, contact and advertising controls. Select the relevant service and region."),
        PrivacyProfile("Facebook, Inc.","https://www.facebook.com/privacy/policy/",null,
            "2026-09-23","This is Meta's official policy entry for the legacy dataset owner name. It may require sign-in. Specific request routes were not verified."),
        PrivacyProfile("Amazon Technologies, Inc.","https://advertising.amazon.com/en-gb/legal/privacy-notice",null,
            "2026-09-23","Amazon Ads' privacy-notice entry. Check the applicable service, controller and region; the dataset owner is not necessarily your legal contact.")
    )
    fun find(owner: String)=profiles.firstOrNull { it.organisation==owner }
}

enum class RequestKind(val label: String, val wording: String) {
    ACCESS("Request my data","Please provide a copy of the personal information associated with my account, including the purposes of processing and recipients where applicable."),
    DELETE("Delete my data","Please delete the personal information associated with my account where applicable. Explain any information you must retain and the reason. Please explain any effect on my account before taking irreversible action."),
    OBJECT("Object to processing","I object to the processing described below. Please review my objection and explain the available options and any processing you intend to continue."),
    WITHDRAW("Withdraw consent","I wish to withdraw consent for the processing described below. Please explain how to apply this change and any processing that relies on a different basis."),
    OPT_OUT("Opt out of personalised ads","Please explain and apply the available options to opt out of personalised advertising for my account or device.")
}
enum class RequestStatus(val label: String) {
    PREPARED("Prepared"), SUBMITTED("Submitted — recorded by you"), ACKNOWLEDGED("Acknowledged — recorded by you"),
    COMPLETED("Completion reported — not verified by Noma"), REJECTED("Rejected — recorded by you"), USER_ACTION_REQUIRED("Your action required")
}
data class RequestUpdate(val status: RequestStatus, val time: Long, val reference: String)
data class PrivacyRequest(val id: String, val organisation: String, val kind: RequestKind,
                          val body: String, val createdAt: Long, val history: List<RequestUpdate>) {
    val status get()=history.last().status
    fun revised(body: String, next: RequestStatus, reference: String, now: Long): PrivacyRequest {
        require(body.isNotBlank() && body.length<=6000)
        require(next==status || next in nextStatuses()) { "Invalid request status change" }
        require(next==status || reference.trim().length in 3..1000) { "Add a dated response or submission reference" }
        require(now>=history.last().time)
        // Preserve the prepared/sent wording after a recorded submission.
        require(status==RequestStatus.PREPARED || body==this.body)
        return copy(body=body,history=if(next==status) history else history+RequestUpdate(next,now,reference.trim()))
    }
    fun nextStatuses(): List<RequestStatus> = when(status) {
        RequestStatus.PREPARED -> listOf(RequestStatus.SUBMITTED)
        RequestStatus.SUBMITTED -> listOf(RequestStatus.ACKNOWLEDGED,RequestStatus.COMPLETED,RequestStatus.REJECTED,RequestStatus.USER_ACTION_REQUIRED)
        RequestStatus.ACKNOWLEDGED -> listOf(RequestStatus.COMPLETED,RequestStatus.REJECTED,RequestStatus.USER_ACTION_REQUIRED)
        RequestStatus.USER_ACTION_REQUIRED -> listOf(RequestStatus.SUBMITTED,RequestStatus.ACKNOWLEDGED,RequestStatus.COMPLETED,RequestStatus.REJECTED)
        RequestStatus.COMPLETED,RequestStatus.REJECTED -> listOf(RequestStatus.USER_ACTION_REQUIRED)
    }
    companion object {
        fun prepare(profile: PrivacyProfile,kind: RequestKind,now: Long=System.currentTimeMillis()): PrivacyRequest = PrivacyRequest(
            UUID.randomUUID().toString(),profile.organisation,kind,
            "To the privacy team,\n\n${kind.wording}\n\nService/account identifier: [add only what the official route requires]\nScope or processing concerned: [describe your request]\nCountry/region: [your location]\n\nPlease confirm receipt and explain any further verification through your official secure channel.\n\n[Your name]",
            now,listOf(RequestUpdate(RequestStatus.PREPARED,now,"Draft prepared locally; not sent")))
    }
}
