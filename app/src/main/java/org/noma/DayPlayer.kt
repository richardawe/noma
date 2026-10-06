package org.noma

import android.content.Context
import android.view.accessibility.AccessibilityManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.SecureFlagPolicy
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.format.DateTimeFormatter

/** No media files or extra persistence: playback is a disposable view of retained evidence. */
@Composable internal fun DayPlayer(story: DayStory, registry: DomainRegistry, onClose: () -> Unit) {
    var frame by remember { mutableIntStateOf(0) }
    val accessibility=LocalContext.current.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
    var playing by remember { mutableStateOf(!accessibility.isTouchExplorationEnabled) }
    var evidence by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<Event?>(null) }
    val scroll=rememberLazyListState()
    val fade=remember { Animatable(1f) }
    LaunchedEffect(frame) { scroll.scrollToItem(0);fade.snapTo(0f);fade.animateTo(1f,tween(300)) }
    val lifecycle=LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer=LifecycleEventObserver { _,event -> if(event==Lifecycle.Event.ON_STOP) playing=false }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(playing,frame) {
        if(playing) {
            if(frame>=story.frameCount-1) playing=false
            else { delay(7000);frame++ }
        }
    }
    val facts=story.summary.facts
    val chapter=story.chapters.getOrNull(frame-1)
    val last=frame==story.frameCount-1
    val records=chapter?.events ?: facts.events
    val time=DateTimeFormatter.ofPattern("HH:mm:ss XXX").withZone(facts.zone)
    Dialog(onDismissRequest=onClose,properties=DialogProperties(usePlatformDefaultWidth=false,securePolicy=SecureFlagPolicy.SecureOn,decorFitsSystemWindows=false)) {
        MaterialTheme(colorScheme=darkColorScheme(primary=Color(0xFFA3E6CE),background=Color(0xFF102E2B),surface=Color(0xFF163B35),onSurface=Color(0xFFF4F1E6))) {
            Surface(Modifier.fillMaxSize()) {
                Column(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF102E2B),Color(0xFF214C43)))).safeDrawingPadding().padding(24.dp)) {
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                        Text("N O M A  /  YOUR DAY",Modifier.padding(top=12.dp),style=MaterialTheme.typography.labelLarge)
                        TextButton(onClick=onClose) { Text("Close playback") }
                    }
                    LinearProgressIndicator(progress={ (frame+1).toFloat()/story.frameCount },modifier=Modifier.fillMaxWidth())
                    Text("${frame+1} of ${story.frameCount} · ${if(playing) "Playing" else "Paused"}",Modifier.padding(top=12.dp),style=MaterialTheme.typography.labelMedium)
                    LazyColumn(Modifier.weight(1f).fillMaxWidth().graphicsLayer { alpha=fade.value },state=scroll,verticalArrangement=Arrangement.spacedBy(20.dp)) {
                        item { Spacer(Modifier.height(16.dp)) }
                        if(frame==0) {
                            item {
                                Text(facts.date.format(DateTimeFormatter.ofPattern("EEEE\nd MMMM")),style=MaterialTheme.typography.displaySmall)
                                Spacer(Modifier.height(24.dp))
                                Text("One day.\nA visible trail.",style=MaterialTheme.typography.headlineLarge)
                                Spacer(Modifier.height(20.dp))
                                Text("${facts.events.size} retained records, told in time order.")
                                Text("${facts.zone.id} · A snapshot when you pressed Play. New records appear next time.",style=MaterialTheme.typography.bodySmall)
                            }
                        } else if(last) {
                            item {
                                Text("Your day,\nin evidence.",style=MaterialTheme.typography.displaySmall)
                                Spacer(Modifier.height(24.dp))
                                Text("${facts.appsResumed}",style=MaterialTheme.typography.displayLarge)
                                Text("apps recorded resuming")
                                Spacer(Modifier.height(16.dp))
                                Text("${facts.requests} DNS requests\n${facts.names} distinct names\n${facts.graph.organisations.size} organisations matched",style=MaterialTheme.typography.headlineSmall)
                            }
                            item { Text("This is the trail Noma retained, not a complete account of your day.") }
                        } else if(chapter!=null) {
                            item {
                                Text(time.format(Instant.ofEpochMilli(chapter.start)),style=MaterialTheme.typography.displaySmall)
                                Text("Recorded activity",style=MaterialTheme.typography.headlineMedium)
                                Text("Through ${time.format(Instant.ofEpochMilli(chapter.end))} · grouped within a 15-minute window",style=MaterialTheme.typography.bodySmall)
                            }
                            if(chapter.graph.appEvents.isNotEmpty()) item {
                                Text("APP ACTIVITY · OBSERVED",style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.primary)
                                val resumes=chapter.graph.appEvents.filter { it.kind=="APP_RESUMED" }.map { it.value }.distinct()
                                val pauses=chapter.graph.appEvents.count { it.kind=="APP_PAUSED" }
                                if(resumes.isNotEmpty()) Text("Recorded resuming:\n"+resumes.take(3).joinToString("\n") + if(resumes.size>3) "\n+ ${resumes.size-3} more in evidence" else "")
                                if(pauses>0) Text("Pause events: $pauses")
                            }
                            val requests=chapter.events.count { it.kind=="DNS_REQUEST" }
                            if(requests>0) item {
                                Text("DNS REQUESTS · OBSERVED",style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.primary)
                                Text("$requests",style=MaterialTheme.typography.displayLarge)
                                Text("name requests recorded in this window")
                                if(chapter.graph.organisations.isNotEmpty()) {
                                    Spacer(Modifier.height(12.dp))
                                    Text("DATASET MATCHES · CLASSIFIED",style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.primary)
                                    Text(chapter.graph.organisations.take(4).joinToString("\n") { it.name })
                                }
                                Text("Names with no owner match: ${chapter.graph.unknown.size}")
                            }
                            item { Text("SO FAR\n${chapter.cumulativeRequests} DNS requests · ${chapter.cumulativeOwners} organisations matched",style=MaterialTheme.typography.titleMedium) }
                        }
                        item {
                            Text("Nearby app and DNS events do not prove a connection. DNS requests do not prove visits, tracking or data shared.",style=MaterialTheme.typography.bodySmall)
                            if(facts.partialRetention) Text("Partial day: records older than 24 hours are not retained.",style=MaterialTheme.typography.bodySmall)
                            Text("Collection has gaps. No location or app durations are inferred.",style=MaterialTheme.typography.bodySmall)
                        }
                        item { OutlinedButton(onClick={playing=false;evidence=true}) { Text("Inspect playback evidence") } }
                        item { Spacer(Modifier.height(8.dp)) }
                    }
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                        TextButton(enabled=frame>0,onClick={playing=false;frame--}) { Text("Back") }
                        Button(onClick={if(last) {frame=0;playing=true} else playing=!playing}) { Text(if(last) "Replay" else if(playing) "Pause" else "Play") }
                        TextButton(enabled=!last,onClick={playing=false;frame++}) { Text("Next") }
                    }
                    TextButton(enabled=!last,onClick={playing=false;frame=story.frameCount-1},modifier=Modifier.fillMaxWidth()) { Text("Skip to recap") }
                }
            }
            if(evidence) AlertDialog(onDismissRequest={evidence=false},title={Text("Playback evidence")},text={
                LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    items(records,key={it.id}) { e -> TextButton(onClick={evidence=false;selected=e}) { Text("${time.format(Instant.ofEpochMilli(e.time))}\n${e.value}\n${e.id.take(8)} →") } }
                }
            },confirmButton={TextButton(onClick={evidence=false}) {Text("Close evidence")}})
            selected?.let { e ->
                val mapping=if(e.kind=="DNS_REQUEST") registry.lookup(e.value) else null
                AlertDialog(onDismissRequest={selected=null},title={Text("Original playback record")},text={
                    LazyColumn(verticalArrangement=Arrangement.spacedBy(12.dp)) {
                        item { Text("OBSERVED · ${e.kind}\n${e.value}\n${time.format(Instant.ofEpochMilli(e.time))}\nSource: ${e.source}\nEvent: ${e.id}") }
                        if(e.kind=="DNS_REQUEST") item { Text("Originating app: UNKNOWN") }
                        item { Text(mapping?.let { "CLASSIFIED · ${it.organisation}\nCategories: ${it.categories}\nSource: ${it.source}\nVersion: ${it.version}\nCategories do not prove this request's purpose." } ?: "No organisation classification for this record.") }
                    }
                },confirmButton={TextButton(onClick={selected=null}) {Text("Return to playback")}})
            }
        }
    }
}
