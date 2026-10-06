package org.noma

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Process
import androidx.work.*
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

object UsageCollector {
    fun allowed(context: Context): Boolean = context.getSystemService(AppOpsManager::class.java)
        .unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,Process.myUid(),context.packageName) == AppOpsManager.MODE_ALLOWED
    @Synchronized fun collect(context: Context) {
        val store=LocalStore.get(context)
        if (!store.usageEnabled()) return
        if (!allowed(context)) { store.enableUsage(false);return }
        val token=store.epoch();val end=System.currentTimeMillis()
        val start=maxOf(store.cursor(),end-86_400_000)
        val events=context.getSystemService(UsageStatsManager::class.java).queryEvents(start,end) ?: return
        val e=UsageEvents.Event()
        val batch=ArrayList<Event>(256)
        while(events.hasNextEvent()) {
            events.getNextEvent(e)
            val kind=when(e.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED -> "APP_RESUMED"
                UsageEvents.Event.ACTIVITY_PAUSED -> "APP_PAUSED"
                else -> continue
            }
            val pkg=e.packageName ?: continue
            val id=MessageDigest.getInstance("SHA-256").digest("${e.timeStamp}:$kind:$pkg:${e.className}".toByteArray()).joinToString("") { "%02x".format(it) }
            batch+=Event(id,e.timeStamp,kind,pkg,"Android UsageStats ACTIVITY lifecycle")
            if(batch.size==256) { if(!store.addBatch(batch,token)) return;batch.clear() }
        }
        if(batch.isNotEmpty() && !store.addBatch(batch,token)) return
        store.setCursor(end,token)
    }
    fun schedule(context: Context) {
        WorkManager.getInstance(context).enqueueUniquePeriodicWork("noma-retention",ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<CollectorWorker>(15,TimeUnit.MINUTES).build())
    }
}
class CollectorWorker(context: Context,parameters: WorkerParameters): Worker(context,parameters) {
    override fun doWork(): Result = try {
        UsageCollector.collect(applicationContext); LocalStore.get(applicationContext).purge();Result.success()
    } catch (_: Exception) { Result.retry() }
}
