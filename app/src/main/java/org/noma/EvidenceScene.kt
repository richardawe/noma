package org.noma

import android.annotation.SuppressLint
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream

/** A read-only projection of the selected day's retained evidence. No drafts or private keys cross into JS. */
internal fun sceneSnapshot(summary: DailySummary, usage: Boolean, dns: Boolean, status: String, error: String): String {
    val facts=summary.facts
    val records=JSONArray()
    if(error.isEmpty()) facts.events.forEach { e -> records.put(JSONObject().put("id",e.id).put("time",e.time)
        .put("kind",e.kind).put("value",e.value).put("source",e.source)) }
    val matches=JSONArray()
    if(error.isEmpty()) facts.graph.organisations.flatMap { it.domains }.mapNotNull { it.classification }.forEach { c ->
        matches.put(JSONObject().put("domain",c.domain).put("organisation",c.organisation)
            .put("categories",c.categories).put("source",c.source).put("version",c.version))
    }
    return JSONObject().put("date",facts.date.toString()).put("zone",facts.zone.id)
        .put("generatedAt",facts.generatedAt).put("retainedFrom",facts.retainedFrom)
        .put("partialRetention",facts.partialRetention).put("usage",usage).put("dns",dns)
        .put("status",status).put("error",error).put("events",records).put("matches",matches).toString()
}

/** No JS interface, file access, remote resources, popups or arbitrary navigation. */
@SuppressLint("SetJavaScriptEnabled")
private class ArchiveWebView(context: android.content.Context): WebView(context) {
    var snapshot=""
    var openEvidence: (String)->Unit={}
    private var ready=false
    init {
        settings.javaScriptEnabled=true
        settings.blockNetworkLoads=true
        settings.allowFileAccess=false
        settings.allowContentAccess=false
        settings.domStorageEnabled=false
        settings.databaseEnabled=false
        settings.cacheMode=android.webkit.WebSettings.LOAD_NO_CACHE
        settings.mixedContentMode=android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
        settings.setSupportMultipleWindows(false)
        isSaveEnabled=false
        webViewClient=object: WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val uri=request.url
                if(request.isForMainFrame && uri.scheme=="https" && uri.host=="noma.local" && uri.port == -1 && uri.path=="/evidence") {
                    uri.getQueryParameter("id")?.takeIf { it.length<=256 }?.let(openEvidence)
                }
                return true
            }
            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse {
                val uri=request.url
                val path=uri.path.orEmpty()
                if(request.method!="GET" || uri.scheme!="https" || uri.host!="noma.local" || uri.port != -1 || !path.startsWith("/archive/") || path.contains("..") || path.contains('\\')) return denied()
                val asset=path.removePrefix("/archive/")
                val mime=when(asset.substringAfterLast('.')) { "html"->"text/html";"js"->"text/javascript";"css"->"text/css";"json"->"application/json";else->return denied() }
                return try { WebResourceResponse(mime,"UTF-8",200,"OK",mapOf("Cache-Control" to "no-store","X-Content-Type-Options" to "nosniff"),context.assets.open("evidence-scene/$asset")) }
                catch (_: Exception) { denied() }
            }
            override fun onPageFinished(view: WebView, url: String) {
                if(url=="https://noma.local/archive/index.html") { ready=true;publish() }
            }
        }
        loadUrl("https://noma.local/archive/index.html")
    }
    private fun denied()=WebResourceResponse("text/plain","UTF-8",403,"Blocked",emptyMap(),ByteArrayInputStream("Local assets only".toByteArray()))
    fun publish() {
        if(ready && snapshot.isNotEmpty()) {
            // A quoted JSON string prevents evidence text from becoming executable code.
            evaluateJavascript("window.nomaUpdate(JSON.parse(${JSONObject.quote(snapshot)}))",null)
        }
    }
}

@Composable
internal fun EvidenceSceneScreen(summary: DailySummary, usage: Boolean, dns: Boolean, status: String, error: String,
                                onClose: ()->Unit, onEvidence: (String)->Unit) {
    BackHandler(onBack=onClose)
    val latestEvidence by rememberUpdatedState(onEvidence)
    val snapshot=remember(summary,usage,dns,status,error) { sceneSnapshot(summary,usage,dns,status,error) }
    var browser by remember { mutableStateOf<ArchiveWebView?>(null) }
    val owner=androidx.compose.ui.platform.LocalContext.current as? androidx.lifecycle.LifecycleOwner
    DisposableEffect(owner,browser) {
        val observer=androidx.lifecycle.LifecycleEventObserver { _,event ->
            if(event==androidx.lifecycle.Lifecycle.Event.ON_STOP) browser?.onPause()
            if(event==androidx.lifecycle.Lifecycle.Event.ON_START) browser?.onResume()
        }
        owner?.lifecycle?.addObserver(observer)
        onDispose { owner?.lifecycle?.removeObserver(observer) }
    }
    DisposableEffect(Unit) { onDispose { browser?.apply { stopLoading();loadUrl("about:blank");clearHistory();removeAllViews();destroy() };browser=null } }
    Column(Modifier.fillMaxSize().safeDrawingPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal=12.dp),horizontalArrangement=Arrangement.SpaceBetween) {
            TextButton(onClick=onClose) { Text("← Your Day") }
            Text("Your Day in 3D",Modifier.padding(12.dp),style=MaterialTheme.typography.titleSmall)
        }
        AndroidView(factory={ context -> ArchiveWebView(context).also { browser=it } },
            modifier=Modifier.fillMaxWidth().weight(1f),update={ view ->
                view.openEvidence={ id -> latestEvidence(id) }
                view.snapshot=snapshot;view.publish()
            })
    }
}
