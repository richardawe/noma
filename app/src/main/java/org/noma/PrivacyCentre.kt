package org.noma

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PersistableBundle
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

internal fun LazyListScope.privacyItems(requests: List<PrivacyRequest>,observedOwners: Set<String>,
                                       onPrepare: (PrivacyProfile) -> Unit,onRequest: (PrivacyRequest) -> Unit) {
    item {
        Text("Take Back My Data",style=MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(10.dp))
        Text("Prepare a request, use the organisation's official route, then record its response. Noma does not send requests or verify deletion.")
        Spacer(Modifier.height(20.dp))
        Card(colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.secondaryContainer)) {
            Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                Text("One step at a time",style=MaterialTheme.typography.titleLarge)
                Text("01  Prepare your words\n02  Send through the official route\n03  Keep a record of the response",style=MaterialTheme.typography.bodyLarge)
            }
        }
    }
    item { Text("Your requests",style=MaterialTheme.typography.titleLarge)
        if(requests.isEmpty()) Text("No requests prepared. Choose an organisation below.") }
    items(requests,key={"request:${it.id}"}) { request ->
        Card(onClick={onRequest(request)},modifier=Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                Text(request.organisation,style=MaterialTheme.typography.titleMedium)
                Text(request.kind.label)
                Text(request.status.label,color=MaterialTheme.colorScheme.primary)
                Text("Review draft and request history →",style=MaterialTheme.typography.labelSmall)
            }
        }
    }
    item { Text("Privacy directory",style=MaterialTheme.typography.titleLarge)
        Text("Public entries are not evidence that these organisations hold your data. Check the correct service, region and legal entity.",style=MaterialTheme.typography.bodySmall) }
    items(PrivacyDirectory.profiles,key={"privacy:${it.organisation}"}) { profile ->
        Card(onClick={onPrepare(profile)},modifier=Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                Text(profile.organisation,style=MaterialTheme.typography.titleMedium)
                Text(if(profile.organisation in observedOwners) "Has a dataset match in your retained DNS records" else "Public directory entry · no current observed match",style=MaterialTheme.typography.bodySmall)
                Text("Review privacy routes and prepare →")
            }
        }
    }
    item { Text("Drafts and request history stay encrypted here until you remove them, beyond the timeline’s 24-hour limit.",style=MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(8.dp))
        Text("Unknown organisations have no guessed contact. Rights and available options vary by service and location. No automated submission is supported.",style=MaterialTheme.typography.bodySmall) }
}

@Composable internal fun RequestEditor(profile: PrivacyProfile, existing: PrivacyRequest?, busy: Boolean,
                                       failure: String, onSave: (PrivacyRequest) -> Unit,
                                       onDelete: (String) -> Unit, onClose: () -> Unit) {
    val context=LocalContext.current
    var request by remember { mutableStateOf(existing ?: PrivacyRequest.prepare(profile,RequestKind.ACCESS)) }
    var body by remember { mutableStateOf(request.body) }
    var status by remember { mutableStateOf(request.status) }
    var reference by remember { mutableStateOf("") }
    var feedback by remember { mutableStateOf("") }
    var kinds by remember { mutableStateOf(false) }
    var statuses by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    fun open(url: String) {
        try { context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse(url))) }
        catch (_: Exception) { feedback="No browser could open this official page." }
    }
    AlertDialog(onDismissRequest={if(!busy) onClose()},title={Text(if(existing==null) "Prepare a privacy request" else "Request details")},text={
        LazyColumn(verticalArrangement=Arrangement.spacedBy(12.dp)) {
            item {
                Text(profile.organisation,style=MaterialTheme.typography.titleMedium)
                Text(profile.notes)
                Text("Directory checked: ${profile.checked}\n${profile.contact}\nDeletion: ${profile.deletionSupported}\n${profile.verification}",style=MaterialTheme.typography.bodySmall)
                Text("GDPR / CCPA routes: consult the official policy for your applicable rights and controller. No direct regional form or DPO email is assumed.",style=MaterialTheme.typography.bodySmall)
                TextButton(onClick={open(profile.policyUrl)}) { Text("Open official privacy page ↗") }
                Text(profile.policyUrl,style=MaterialTheme.typography.bodySmall)
                profile.choicesUrl?.let { url -> TextButton(onClick={open(url)}) { Text("Open advertising choices ↗") } }
                Text("Opening a site does not submit this draft or update its status.",style=MaterialTheme.typography.bodySmall)
            }
            item {
                Box {
                    OutlinedButton(enabled=existing==null && !busy,onClick={kinds=true}) { Text(request.kind.label) }
                    DropdownMenu(expanded=kinds,onDismissRequest={kinds=false}) {
                        RequestKind.entries.forEach { kind -> DropdownMenuItem(text={Text(kind.label)},onClick={
                            request=PrivacyRequest.prepare(profile,kind);body=request.body;status=request.status;reference="";kinds=false
                        }) }
                    }
                }
                OutlinedTextField(value=body,onValueChange={if(it.length<=6000) body=it},label={Text("Request draft")},
                    modifier=Modifier.fillMaxWidth(),minLines=5,maxLines=10,readOnly=request.status!=RequestStatus.PREPARED,enabled=!busy)
                Text("Replace bracketed fields before using the draft. Do not put passwords or identity documents here. After recording submission, this wording is kept as your sent-copy record.",style=MaterialTheme.typography.bodySmall)
                TextButton(enabled=body.isNotBlank() && !busy,onClick={
                    val clip=ClipData.newPlainText("Noma privacy request",body)
                    if(Build.VERSION.SDK_INT>=33) clip.description.extras=PersistableBundle().apply { putBoolean(ClipDescription.EXTRA_IS_SENSITIVE,true) }
                    (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(clip)
                    feedback="Copied to the system clipboard. Nothing was sent."
                }) { Text("Copy draft") }
                Text("Copying puts this text on the system clipboard. Submit it yourself through the appropriate official route.",style=MaterialTheme.typography.bodySmall)
            }
            if(existing!=null) item {
                Text("Track your request",style=MaterialTheme.typography.titleMedium)
                Box {
                    OutlinedButton(enabled=!busy,onClick={statuses=true}) { Text(status.label) }
                    DropdownMenu(expanded=statuses,onDismissRequest={statuses=false}) {
                        (listOf(request.status)+request.nextStatuses()).forEach { next ->
                            DropdownMenuItem(text={Text(next.label)},onClick={status=next;statuses=false})
                        }
                    }
                }
                if(status!=request.status) OutlinedTextField(value=reference,onValueChange={if(it.length<=1000) reference=it},
                    label={Text("Submission or response reference")},placeholder={Text("Dated receipt, case number or response summary")},modifier=Modifier.fillMaxWidth(),minLines=2)
                Text("Updates are your records, not confirmation from the organisation. Completion requires a response reference and is not proof that all data was deleted.",style=MaterialTheme.typography.bodySmall)
            }
            item {
                Text("History",style=MaterialTheme.typography.titleMedium)
                val date=DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm").withZone(ZoneId.systemDefault())
                request.history.forEach { update -> Text("${date.format(Instant.ofEpochMilli(update.time))} · ${update.status.label}\n${update.reference}",Modifier.padding(vertical=6.dp),style=MaterialTheme.typography.bodySmall) }
                if(feedback.isNotEmpty()) Text(feedback)
                if(failure.isNotEmpty()) Text(failure,color=MaterialTheme.colorScheme.error)
                if(existing!=null) TextButton(enabled=!busy,onClick={deleting=true}) { Text("Remove local request record") }
            }
        }
    },confirmButton={TextButton(enabled=!busy && body.isNotBlank() && (status==request.status || reference.trim().length>=3),onClick={
        try { onSave(request.revised(body,status,reference,System.currentTimeMillis())) }
        catch (_: IllegalArgumentException) { feedback="Check the draft and add a dated reference for the status change." }
    }) { Text(if(busy) "Saving…" else "Save locally") }},dismissButton={TextButton(enabled=!busy,onClick=onClose) {Text("Close")}})
    if(deleting) AlertDialog(onDismissRequest={deleting=false},title={Text("Remove this local record?")},text={Text("This removes the saved draft and its history from Noma. It does not withdraw a request you sent to the organisation.")},
        confirmButton={TextButton(enabled=!busy,onClick={deleting=false;onDelete(request.id)}) {Text("Remove record")}},dismissButton={TextButton(onClick={deleting=false}) {Text("Cancel")}})
}
