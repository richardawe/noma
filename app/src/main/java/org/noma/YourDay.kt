package org.noma

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.format.DateTimeFormatter

internal fun LazyListScope.yourDayItems(summary: DailySummary, today: LocalDate,
    onDate: (LocalDate) -> Unit, onClaim: (String) -> Unit, onPlay: () -> Unit, onStart: () -> Unit) {
    val facts=summary.facts
    item {
        Text("A little more clarity.",style=MaterialTheme.typography.headlineLarge)
        Text("Your digital day, made visible.",Modifier.padding(top=8.dp),color=MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp),modifier=Modifier.padding(top=12.dp)) {
            FilterChip(selected=facts.date==today,onClick={onDate(today)},label={Text("Today")})
            FilterChip(selected=facts.date==today.minusDays(1),onClick={onDate(today.minusDays(1))},label={Text("Yesterday")})
        }
    }
    item {
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(Pine).padding(24.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Text(facts.date.format(DateTimeFormatter.ofPattern("EEEE, d MMMM")).uppercase(),style=MaterialTheme.typography.labelMedium,color=Color(0xFFD3E1D1))
            // A decorative path, not a chart or a representation of unobserved activity.
            Canvas(Modifier.fillMaxWidth().height(100.dp)) {
                val w=size.width;val h=size.height
                drawCircle(Apricot, h*.34f, Offset(w*.8f,h*.38f))
                val trail=Path().apply { moveTo(0f,h*.73f);cubicTo(w*.2f,-h*.5f,w*.25f,h*1.4f,w*.5f,h*.65f);cubicTo(w*.7f,-h*.15f,w*.73f,h*1.4f,w,h*.6f) }
                drawPath(trail,Color(0xFFADCAB2),style=Stroke(2.dp.toPx()))
                drawCircle(Cream,5.dp.toPx(),Offset(0f,h*.73f));drawCircle(Cream,5.dp.toPx(),Offset(w,h*.6f))
            }
            Text(if(facts.events.isEmpty()) "Your story starts\nwith you." else "Small moments.\nA clearer picture.",style=MaterialTheme.typography.headlineMedium,color=Cream)
            Text(if(facts.events.isEmpty()) "Choose what to record. Noma will turn the evidence into a private story you can explore."
                else "Take a moment to explore what was recorded, one chapter at a time.",color=Color(0xFFD3E1D1))
            Button(onClick=if(facts.events.isEmpty()) onStart else onPlay,modifier=Modifier.fillMaxWidth().heightIn(min=54.dp),
                colors=ButtonDefaults.buttonColors(containerColor=Apricot,contentColor=Pine)) {
                Text(if(facts.events.isEmpty()) "Choose what to record  →" else "▶  Play My Day",style=MaterialTheme.typography.titleMedium)
            }
            Text(if(facts.events.isEmpty()) "No account. No cloud. Your choice." else "Built here, from your retained records.",style=MaterialTheme.typography.bodySmall,color=Color(0xFFD3E1D1))
        }
    }
    item {
        Text(if(facts.events.isEmpty()) "Your summary is waiting for evidence." else "Here is what Noma recorded.",style=MaterialTheme.typography.titleLarge)
        if(facts.events.isEmpty()) {
            Text("Nothing recorded for this date yet. That does not mean there was no activity.",Modifier.padding(top=8.dp),color=MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Row(Modifier.fillMaxWidth().padding(top=16.dp),horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                listOf(facts.appsResumed to "Apps resumed", facts.requests to "DNS requests", facts.graph.organisations.size to "Organisations").forEach { (number,label) ->
                    Surface(Modifier.weight(1f),shape=RoundedCornerShape(18.dp),color=Color(0xFFEAEDE2)) {
                        Column(Modifier.padding(horizontal=10.dp,vertical=16.dp),verticalArrangement=Arrangement.spacedBy(5.dp)) {
                            Text("$number",style=MaterialTheme.typography.headlineMedium)
                            Text(label,style=MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
    items(summary.claims,key={"claim:${it.id}"}) { claim ->
        Card(onClick={onClaim(claim.id)},modifier=Modifier.fillMaxWidth(),colors=CardDefaults.cardColors(containerColor=Color(0xFFFEFCF7))) {
            Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                Text(when(claim.level.name) { "OBSERVED" -> "IN YOUR RECORDS";"CLASSIFIED" -> "A DATASET MATCH";else -> "STILL UNKNOWN" },style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.primary)
                Text(claim.text,style=MaterialTheme.typography.bodyLarge)
                Text("Explore the evidence  ↗",style=MaterialTheme.typography.labelMedium,color=Pine)
            }
        }
    }
    item {
        HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant)
        Spacer(Modifier.height(16.dp))
        Text("A window, not the whole picture",style=MaterialTheme.typography.titleMedium)
        Text("DNS requests do not prove a visit, tracking or data shared. Organisation matches do not identify the app behind a request.",Modifier.padding(top=8.dp),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        Text(if(facts.partialRetention) "Partial day: records older than 24 hours are not retained." else "Collection can have gaps. Records are kept for up to 24 hours.",Modifier.padding(top=8.dp),style=MaterialTheme.typography.bodySmall)
        Text("${facts.zone.id} · Factual summary · no language model",Modifier.padding(top=8.dp),style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
