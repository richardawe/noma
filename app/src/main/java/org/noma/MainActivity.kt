package org.noma

import android.Manifest
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class MainActivity: ComponentActivity() {
    private var events by mutableStateOf(emptyList<Event>())
    private var requests by mutableStateOf(emptyList<PrivacyRequest>())
    private var usage by mutableStateOf(false)
    private var dns by mutableStateOf(false)
    private var message by mutableStateOf("")
    private var error by mutableStateOf("")
    private var loading by mutableStateOf(false)
    private var refreshedAt by mutableLongStateOf(System.currentTimeMillis())
    private var operation=0L
    private lateinit var registry: DomainRegistry
    private val store by lazy { LocalStore.get(this) }
    private val consent=registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if(it.resultCode==RESULT_OK) startDns() else message="VPN consent was not granted."
    }
    private val notifications=registerForActivityResult(ActivityResultContracts.RequestPermission()) { requestVpn() }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        registry=RegistryLoader.load(this)
        UsageCollector.schedule(this)
        setContent {
            NomaTheme {
                Surface(Modifier.fillMaxSize()) { Screen() }
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                while(isActive) { refresh();delay(10_000) }
            }
        }
    }
    private suspend fun refresh() {
        if(loading) return
        val generation=operation
        try {
            val fresh=withContext(Dispatchers.IO) { UsageCollector.collect(this@MainActivity);store.read() to store.requests() }
            if(generation!=operation) return
            events=fresh.first;requests=fresh.second;refreshedAt=System.currentTimeMillis();usage=store.usageEnabled();dns=DnsVpnService.running;message=DnsVpnService.status;error=""
        } catch (_: Exception) { events=emptyList();requests=emptyList();error="Local data could not be read. No missing evidence has been guessed. Try again or erase to reset storage." }
    }
    private fun requestVpn() {
        val intent=VpnService.prepare(this)
        if(intent!=null) consent.launch(intent) else startDns()
    }
    private fun startDns() {
        try { startForegroundService(Intent(this,DnsVpnService::class.java));message="Starting DNS observation…" }
        catch (_: Exception) { message="Android could not start DNS observation. Try again with Noma open." }
    }
    @OptIn(ExperimentalLayoutApi::class)
    @Composable private fun Screen() {
        val screenScroll=rememberLazyListState()
        var detail by remember { mutableStateOf<Event?>(null) }
        var confirmDns by remember { mutableStateOf(false) }
        var confirmErase by remember { mutableStateOf(false) }
        var view by remember { mutableStateOf("Your Day") }
        var showArchive by remember { mutableStateOf(false) }
        LaunchedEffect(view) { screenScroll.scrollToItem(0) }
        var dayOffset by remember { mutableIntStateOf(0) }
        var selectedClaim by remember { mutableStateOf<String?>(null) }
        var playback by remember { mutableStateOf<DayStory?>(null) }
        var requestProfile by remember { mutableStateOf<PrivacyProfile?>(null) }
        var editingRequest by remember { mutableStateOf<PrivacyRequest?>(null) }
        var requestEpoch by remember { mutableLongStateOf(0) }
        var requestFailure by remember { mutableStateOf("") }
        fun persistRequest(request: PrivacyRequest?,deleteId: String?=null) {
            loading=true;requestFailure=""
            val token=requestEpoch
            val expected=editingRequest
            lifecycleScope.launch {
                try {
                    requests=withContext(Dispatchers.IO) {
                        val saved=if(request!=null) store.saveRequest(request,token,expected) else store.deleteRequest(deleteId!!,token)
                        check(saved) { "Storage changed. Reopen the request and try again." }
                        store.requests()
                    }
                    requestProfile=null;editingRequest=null;screenScroll.scrollToItem(0)
                } catch (_: Exception) { requestFailure="Could not save. Storage or this request changed, or the local limit was reached. Close and reopen before retrying." }
                finally { loading=false }
            }
        }
        val zone=ZoneId.systemDefault()
        val today=Instant.ofEpochMilli(refreshedAt).atZone(zone).toLocalDate()
        val selectedDate=today.plusDays(dayOffset.toLong())
        val summary=remember(events,selectedDate,zone,refreshedAt) { DailySummarizer.build(events,registry,selectedDate,zone,refreshedAt) }
        // Close snapshots when their evidence is erased, expires, or becomes unreadable.
        LaunchedEffect(events,requests,error,refreshedAt) {
            val ids=events.filter { it.time>=refreshedAt-86_400_000 }.map { it.id }.toSet()
            if(error.isNotEmpty() || detail?.id?.let { it !in ids }==true) detail=null
            if(error.isNotEmpty() || editingRequest?.id?.let { id -> requests.none { it.id==id } }==true) { requestProfile=null;editingRequest=null }
            if(error.isNotEmpty() || playback?.summary?.facts?.events?.any { it.id !in ids }==true) playback=null
        }
        var selectedOwner by remember { mutableStateOf<String?>(null) }
        var selectedDomain by remember { mutableStateOf<String?>(null) }
        val graph=remember(events) { PersonalDataGraph.build(events,registry) }
        val apps=events.filter { it.kind=="APP_RESUMED" }.map { it.value }.distinct().size
        val names=events.filter { it.kind=="DNS_REQUEST" }.map { it.value }.distinct()
        val owners=graph.organisations.size
        if(showArchive) EvidenceSceneScreen(summary,usage,dns,message,error,onClose={showArchive=false},onEvidence={ id ->
            summary.facts.events.firstOrNull { it.id==id }?.let { detail=it }
        }) else Scaffold(containerColor=Cream, bottomBar={
            NavigationBar(containerColor=Color(0xFFFEFCF7),tonalElevation=0.dp) {
                listOf("Your Day","Timeline","Organisations","Take Back").forEachIndexed { index,tab ->
                    NavigationBarItem(selected=view==tab,onClick={view=tab},icon={NavMark(index)},
                        label={Text(tab,style=MaterialTheme.typography.labelSmall)},alwaysShowLabel=true)
                }
            }
        }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(horizontal=22.dp),state=screenScroll,verticalArrangement=Arrangement.spacedBy(20.dp)) {
            item {
                Row(Modifier.fillMaxWidth().padding(top=16.dp),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {
                    Text("noma",style=MaterialTheme.typography.headlineMedium,color=Pine)
                    TextButton(onClick={view="Controls"}) { Text(if(dns || usage) "●  Collecting · Controls" else "○  Collection off · Controls") }
                }
                if(error.isNotEmpty()) Text(error,color=MaterialTheme.colorScheme.error)
            }
            if(view=="Your Day") {
                item { OutlinedButton(onClick={showArchive=true}) { Text("Explore Your Day in 3D  ↗") } }
                yourDayItems(summary,today,onDate={date -> dayOffset=if(date==today) 0 else -1;selectedClaim=null},onClaim={selectedClaim=it},onPlay={playback=DayStory.build(summary,registry)},onStart={view="Controls"})
            } else if(view=="Take Back") {
                privacyItems(requests,graph.organisations.map { it.name }.toSet(),onPrepare={ profile ->
                    editingRequest=null;requestEpoch=store.erasureEpoch();requestFailure="";requestProfile=profile
                },onRequest={ request ->
                    editingRequest=request;requestEpoch=store.erasureEpoch();requestFailure="";requestProfile=PrivacyDirectory.find(request.organisation)
                })
            } else if(view=="Organisations") {
                item {
                    Text("Understand your trail",style=MaterialTheme.typography.titleLarge)
                    Text("Recorded name → dataset owner → category → evidence. These matches do not show which app sent a request or what data was shared.",style=MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(12.dp))
                    Text("Names without an organisation match: ${graph.unknown.size}")
                    Text("Coverage: a small, pinned Tracker Radar sample. Categories describe domains, not your behaviour.",style=MaterialTheme.typography.bodySmall)
                    if(names.isEmpty()) Text("No DNS evidence yet. Open Controls to begin.",Modifier.padding(vertical=24.dp))
                }
                graph.organisations.forEach { owner ->
                    item(key="owner:${owner.name}") {
                    Card(onClick={selectedOwner=if(selectedOwner==owner.name) null else owner.name},modifier=Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                            Text("CLASSIFIED",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.primary)
                            Text(owner.name,style=MaterialTheme.typography.titleMedium)
                            Text("Names: ${owner.domains.size} · Recorded requests: ${owner.eventCount}")
                            Text("Dataset categories: ${owner.categories.joinToString().ifEmpty { "Unknown" }}",style=MaterialTheme.typography.bodySmall)
                            Text(if(selectedOwner==owner.name) "Hide names ↑" else "Inspect names ↓",style=MaterialTheme.typography.labelSmall)
                        }
                    }
                    }
                    if(selectedOwner==owner.name) items(owner.domains,key={"domain:${it.domain}"}) { domain ->
                        TextButton(onClick={selectedDomain=domain.domain},modifier=Modifier.fillMaxWidth()) {
                            Text("${domain.domain} · ${domain.events.size} requests →")
                        }
                    }
                }
                if(graph.unknown.isNotEmpty()) item {
                    Text("Unknown organisations",style=MaterialTheme.typography.titleMedium)
                    Text("No exact record in the bundled dataset. Unknown does not mean safe or unsafe.",style=MaterialTheme.typography.bodySmall)
                }
                items(graph.unknown,key={"unknown:${it.domain}"}) { domain ->
                    OutlinedButton(onClick={selectedDomain=domain.domain},modifier=Modifier.fillMaxWidth()) {
                        Text("${domain.domain} · ${domain.events.size} requests →")
                    }
                }
            } else if(view=="Controls") {
            item {
                Text("You’re in control",style=MaterialTheme.typography.headlineLarge)
                Text("Choose what becomes part of your day. You can pause or erase it at any time.",Modifier.padding(vertical=12.dp))
                Text("What Noma can see",style=MaterialTheme.typography.titleLarge)
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                    Text("App activity",Modifier.padding(top=16.dp))
                    Switch(checked=usage,enabled=!loading,onCheckedChange={ enabled ->
                        if(enabled && !UsageCollector.allowed(this@MainActivity)) {
                            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));message="Grant usage access, then turn App activity on."
                        } else { store.enableUsage(enabled);usage=enabled }
                    })
                }
                Text("App names and resume/pause times only. Starts when you opt in; background import is best-effort. No typed content.",style=MaterialTheme.typography.bodySmall)
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                    Text("Network metadata · DNS only",Modifier.padding(top=16.dp))
                    Switch(checked=dns,enabled=!loading,onCheckedChange={ enabled ->
                        if(enabled) confirmDns=true else { store.fencePendingWrites();DnsVpnService.requestStop(this@MainActivity);dns=false }
                    })
                }
                Text("Standard DNS names only. Encrypted DNS, cached names and traffic that bypasses Noma are not visible. App ownership stays unknown.",style=MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(12.dp))
                Text("Location: off · not collected in M0")
                Text("Stored encrypted here. Retained up to 24 hours when Noma next runs, capped at 20,000 events. Android may delay cleanup.",style=MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(12.dp))
                Text(message,color=MaterialTheme.colorScheme.primary)
                if(error.isNotEmpty()) Text(error,color=MaterialTheme.colorScheme.error)
            }
            item {
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(enabled=!loading,onClick={ lifecycleScope.launch { refresh() } }) { Text("Refresh") }
                    TextButton(enabled=!loading,onClick={ confirmErase=true }) { Text("Erase my data") }
                }
            }
            } else if(view=="Timeline") {
            item {
                Text("Your digital trail",style=MaterialTheme.typography.headlineLarge)
                Text("$apps apps · ${names.size} names · $owners organisations",Modifier.padding(vertical=8.dp),style=MaterialTheme.typography.bodyMedium)
                Text("Observed timeline",style=MaterialTheme.typography.titleLarge)
                Text("Oldest first. Nearby events do not prove an app made a request.",style=MaterialTheme.typography.bodySmall)
                if(events.isEmpty()) Text("Your trail starts with your permission. Nothing has been recorded yet.",Modifier.padding(vertical=24.dp))
            }
            items(events.sortedBy { it.time },key={it.id}) { e ->
                Card(onClick={detail=e},modifier=Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                        Text(time(e.time)+"  ·  OBSERVED",style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.primary)
                        Text(when(e.kind) { "APP_RESUMED" -> "App resumed";"APP_PAUSED" -> "App paused";else -> "DNS name requested" },style=MaterialTheme.typography.titleSmall)
                        Text(e.value)
                        if(e.kind=="DNS_REQUEST") Text(registry.lookup(e.value)?.let { "CLASSIFIED · ${it.organisation}" } ?: "UNKNOWN · organisation",style=MaterialTheme.typography.bodySmall)
                        Text("Inspect evidence →",style=MaterialTheme.typography.labelSmall)
                    }
                }
            }
            }
            item { Text("Private by design. Kept on this device.",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant);Spacer(Modifier.height(16.dp)) }
        }
        }
        if(confirmDns) AlertDialog(onDismissRequest={confirmDns=false},title={Text("Observe standard DNS?")},text={Text("Noma uses Android’s VPN slot and can replace another VPN. It relays DNS to this network’s existing resolver, which still sees queries. IPv4 TCP and UDP traffic passes through a local relay on your device; there is no remote proxy. Apps may choose another network and bypass observation. IPv6 also bypasses observation. Active Private DNS and IPv6-only resolvers are unsupported. Noma excludes its own traffic. No content is decrypted or stored. Stop observation if an app loses connectivity.")},confirmButton={TextButton(onClick={confirmDns=false;if(Build.VERSION.SDK_INT>=33) notifications.launch(Manifest.permission.POST_NOTIFICATIONS) else requestVpn()}) {Text("Continue")}},dismissButton={TextButton(onClick={confirmDns=false}) {Text("Cancel")}})
        if(confirmErase) AlertDialog(onDismissRequest={confirmErase=false},title={Text("Erase all Noma data?")},text={Text("Stops both collectors and deletes all Noma events, privacy drafts, request history and the encryption key. Public organisation data stays. Android’s own usage history is not deleted. Requests already sent to organisations are not withdrawn.")},confirmButton={TextButton(onClick={
            confirmErase=false;detail=null;selectedDomain=null;selectedOwner=null;selectedClaim=null;playback=null;requestProfile=null;editingRequest=null;requests=emptyList();loading=true;operation++;events=emptyList();usage=false;dns=false
            DnsVpnService.requestStop(this@MainActivity)
            lifecycleScope.launch {
                try { withContext(Dispatchers.IO) { store.erase() };message="Noma data erased. Collectors are off." }
                catch (_: Exception) { error="Erasure failed. Keep collection off and retry." }
                finally { loading=false }
            }
        }) {Text("Erase")}},dismissButton={TextButton(onClick={confirmErase=false}) {Text("Cancel")}})
        selectedClaim?.let { id ->
            summary.claims.firstOrNull { it.id==id }?.let { claim ->
                AlertDialog(onDismissRequest={selectedClaim=null},title={Text("Summary evidence")},text={
                    LazyColumn(verticalArrangement=Arrangement.spacedBy(12.dp)) {
                        item {
                            Text("${claim.level.name} · ${claim.text}")
                            Text("These original records support the statement. Select one to inspect its observation and dataset source.",style=MaterialTheme.typography.bodySmall)
                        }
                        items(summary.evidence(claim),key={it.id}) { e ->
                            TextButton(onClick={selectedClaim=null;detail=e}) { Text("${time(e.time)}\n${e.value}\n${e.id.take(8)} →") }
                        }
                    }
                },confirmButton={TextButton(onClick={selectedClaim=null}) {Text("Close")}})
            }
        }
        selectedDomain?.let { name ->
            val domain=(graph.organisations.flatMap { it.domains }+graph.unknown).firstOrNull { it.domain==name }
            if(domain!=null) AlertDialog(onDismissRequest={selectedDomain=null},title={Text(domain.domain)},text={
                LazyColumn(verticalArrangement=Arrangement.spacedBy(12.dp)) {
                    item {
                        Text(domain.classification?.let { "CLASSIFIED · ${it.organisation}\nCategories: ${it.categories}\nSource: ${it.source}\nVersion: ${it.version}" }
                            ?: "UNKNOWN · No exact dataset match. No owner or category has been guessed.")
                        Text("${domain.events.size} recorded requests. Choose one to inspect its original evidence.")
                    }
                    items(domain.events,key={it.id}) { e ->
                        TextButton(onClick={selectedDomain=null;detail=e}) { Text("${time(e.time)} · ${e.id.take(8)} →") }
                    }
                }
            },confirmButton={TextButton(onClick={selectedDomain=null}) {Text("Close")}})
        }
        requestProfile?.let { profile ->
            key(profile.organisation,editingRequest?.id) {
                RequestEditor(profile,editingRequest,loading,requestFailure,onSave={persistRequest(it)},onDelete={persistRequest(null,it)},onClose={requestProfile=null;editingRequest=null})
            }
        }
        playback?.takeIf { it.frameCount>0 }?.let { story -> DayPlayer(story,registry,onClose={playback=null;lifecycleScope.launch { screenScroll.scrollToItem(0) }}) }
        detail?.let { e ->
            val mapping=if(e.kind=="DNS_REQUEST") registry.lookup(e.value) else null
            AlertDialog(onDismissRequest={detail=null},title={Text("Why is this here?")},text={
                LazyColumn(verticalArrangement=Arrangement.spacedBy(12.dp)) {
                    item { Text("OBSERVED · ${e.kind}\n${e.value}\n${time(e.time)}\nSource: ${e.source}\nEvent: ${e.id}") }
                    if(e.kind=="DNS_REQUEST") item { Text("Originating app: UNKNOWN. Resolver socket UID: ${e.uid ?: "unknown"}. This UID is not used to name an app.") }
                    item { Text(mapping?.let { "CLASSIFIED · ${it.organisation}\nDataset categories: ${it.categories}\nExact matched record: ${it.domain}\nSource: ${it.source}\nVersion: ${it.version}\nThese categories do not prove the purpose of this request." } ?: "No organisation classification for this event.") }
                }
            },confirmButton={TextButton(onClick={detail=null}) {Text("Close")}})
        }
    }
    private fun time(ms: Long)=DateTimeFormatter.ofPattern("dd MMM HH:mm:ss").withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(ms))
}
