package org.noma

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import org.json.JSONObject
import org.json.JSONArray
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** All access, consent state and deletion share one lock and an epoch fence. */
class LocalStore private constructor(private val context: Context) {
    private val prefs = context.getSharedPreferences("consent", Context.MODE_PRIVATE)
    private val alias = "noma.events.v1"
    private var db: SQLiteDatabase? = null
    @Synchronized fun epoch() = prefs.getLong("epoch",0)
    @Synchronized fun erasureEpoch() = prefs.getLong("erasureEpoch",0)
    @Synchronized fun fencePendingWrites() { prefs.edit().putLong("epoch",epoch()+1).commit() }
    @Synchronized fun usageEnabled() = prefs.getBoolean("usage",false)
    @Synchronized fun cursor() = prefs.getLong("cursor",System.currentTimeMillis())
    @Synchronized fun enableUsage(enabled: Boolean) {
        prefs.edit().putBoolean("usage",enabled).putLong("cursor",System.currentTimeMillis()).putLong("usageSince",System.currentTimeMillis()).commit()
    }
    @Synchronized fun setCursor(time: Long, token: Long) {
        if (token == epoch() && usageEnabled()) prefs.edit().putLong("cursor",maxOf(time,cursor())).commit()
    }
    private fun database(): SQLiteDatabase {
        if(prefs.getBoolean("erasePending",false)) finishErase()
        return db ?: context.openOrCreateDatabase("events.db",0,null).also {
        it.execSQL("CREATE TABLE IF NOT EXISTS events (id TEXT PRIMARY KEY, time INTEGER NOT NULL, payload BLOB NOT NULL)")
        it.execSQL("CREATE INDEX IF NOT EXISTS event_time ON events(time)")
        it.execSQL("CREATE TABLE IF NOT EXISTS requests (id TEXT PRIMARY KEY, created INTEGER NOT NULL, payload BLOB NOT NULL)")
        db=it
        }
    }
    private fun key(): SecretKey {
        val activeDatabase=database() // Finish a pending erasure before consulting the old key.
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getKey(alias,null) as? SecretKey)?.let { return it }
        // Never silently create a new key over an existing unreadable dataset.
        val hasData = activeDatabase.rawQuery("SELECT 1 FROM events UNION ALL SELECT 1 FROM requests LIMIT 1",null).use { it.moveToFirst() }
        check(!hasData) { "Local key unavailable. Erase data to reset storage." }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(alias,KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256).build())
        }.generateKey()
    }
    @Synchronized fun add(e: Event, token: Long): Boolean = addBatch(listOf(e),token)
    /** Bounded imports commit together; an interrupted batch is safely retried by stable event ID. */
    @Synchronized fun addBatch(events: List<Event>,token: Long): Boolean {
        require(events.size<=512)
        if(token!=epoch()) return false
        val eligible=events.filter { !it.kind.startsWith("APP_") ||
            (usageEnabled() && it.time>=prefs.getLong("usageSince",Long.MAX_VALUE)) }
        if(eligible.isEmpty()) return events.isEmpty()
        val encryptKey=key();val database=database()
        database.beginTransaction()
        try {
            eligible.forEach { e ->
                val json=JSONObject().put("kind",e.kind).put("value",e.value).put("source",e.source).put("uid",e.uid).toString()
                val cipher=Cipher.getInstance("AES/GCM/NoPadding")
                cipher.init(Cipher.ENCRYPT_MODE,encryptKey);cipher.updateAAD("${e.id}:${e.time}".toByteArray())
                val encrypted=cipher.iv+cipher.doFinal(json.toByteArray())
                database.execSQL("INSERT OR IGNORE INTO events VALUES (?,?,?)",arrayOf(e.id,e.time,encrypted))
            }
            purge();database.setTransactionSuccessful()
        } finally { database.endTransaction() }
        return true
    }
    @Synchronized fun purge() {
        database().delete("events","time < ?",arrayOf((System.currentTimeMillis()-86_400_000).toString()))
        database().execSQL("DELETE FROM events WHERE id IN (SELECT id FROM events ORDER BY time DESC LIMIT -1 OFFSET 20000)")
    }
    private data class EncryptedEvent(val id: String,val time: Long,val bytes: ByteArray)
    fun read(): List<Event> {
        // Copy a consistent encrypted snapshot quickly, then release the writer/stop lock.
        // Keystore decryption of a full day must not block Stop, consent changes or erasure.
        val snapshot=synchronized(this) {
            purge()
            val rows=mutableListOf<EncryptedEvent>()
            database().rawQuery("SELECT id,time,payload FROM events ORDER BY time DESC LIMIT 20000",null).use { c ->
                while(c.moveToNext()) rows+=EncryptedEvent(c.getString(0),c.getLong(1),c.getBlob(2))
            }
            Triple(rows,if(rows.isEmpty()) null else key(),erasureEpoch())
        }
        val out=snapshot.first.map { row ->
            val data=row.bytes
            val cipher=Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE,snapshot.second,GCMParameterSpec(128,data.copyOfRange(0,12)))
            cipher.updateAAD("${row.id}:${row.time}".toByteArray())
            val j=JSONObject(String(cipher.doFinal(data.copyOfRange(12,data.size))))
            Event(row.id,row.time,j.getString("kind"),j.getString("value"),j.getString("source"),if(j.has("uid")) j.getInt("uid") else null)
        }
        return synchronized(this) { if(snapshot.third==erasureEpoch()) out else emptyList() }
    }
    @Synchronized fun requests(): List<PrivacyRequest> {
        val out=mutableListOf<PrivacyRequest>()
        database().rawQuery("SELECT id,created,payload FROM requests ORDER BY created DESC",null).use { c ->
            val decryptKey=if(c.count>0) key() else null
            while(c.moveToNext()) {
                val id=c.getString(0);val created=c.getLong(1);val data=c.getBlob(2)
                val cipher=Cipher.getInstance("AES/GCM/NoPadding")
                cipher.init(Cipher.DECRYPT_MODE,decryptKey,GCMParameterSpec(128,data.copyOfRange(0,12)))
                cipher.updateAAD("request:$id:$created".toByteArray())
                val j=JSONObject(String(cipher.doFinal(data.copyOfRange(12,data.size))))
                val history=j.getJSONArray("history")
                out+=PrivacyRequest(id,j.getString("organisation"),RequestKind.valueOf(j.getString("kind")),j.getString("body"),created,
                    (0 until history.length()).map { i -> history.getJSONObject(i).let {
                        RequestUpdate(RequestStatus.valueOf(it.getString("status")),it.getLong("time"),it.getString("reference"))
                    } })
            }
        }
        return out
    }
    @Synchronized fun saveRequest(request: PrivacyRequest,token: Long,expected: PrivacyRequest?=null): Boolean {
        if(token!=erasureEpoch()) return false
        require(request.body.isNotBlank() && request.body.length<=6000 && request.history.size in 1..100)
        require(request.history.all { it.reference.length<=1000 })
        require(PrivacyDirectory.find(request.organisation)!=null)
        val records=requests();val old=records.firstOrNull { it.id==request.id }
        require(old==expected) { "Request changed or was removed. Reopen it before editing." }
        if(old==null) {
            require(records.size<100) { "Remove an old request before adding another (limit 100)." }
            require(request.history.size==1 && request.status==RequestStatus.PREPARED && request.history.single().time==request.createdAt)
        } else {
            require(request.organisation==old.organisation && request.kind==old.kind && request.createdAt==old.createdAt)
            if(request.history==old.history) require(request==old.revised(request.body,old.status,"",old.history.last().time))
            else {
                require(request.history.dropLast(1)==old.history) { "Request changed; reopen it before editing." }
                val update=request.history.last()
                require(request==old.revised(request.body,update.status,update.reference,update.time))
            }
        }
        val history=JSONArray()
        request.history.forEach { history.put(JSONObject().put("status",it.status.name).put("time",it.time).put("reference",it.reference)) }
        val json=JSONObject().put("organisation",request.organisation).put("kind",request.kind.name).put("body",request.body).put("history",history)
        val cipher=Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE,key());cipher.updateAAD("request:${request.id}:${request.createdAt}".toByteArray())
        val encrypted=cipher.iv+cipher.doFinal(json.toString().toByteArray())
        database().execSQL("INSERT OR REPLACE INTO requests VALUES (?,?,?)",arrayOf(request.id,request.createdAt,encrypted))
        return true
    }
    @Synchronized fun deleteRequest(id: String,token: Long): Boolean {
        if(token!=erasureEpoch()) return false
        database().delete("requests","id = ?",arrayOf(id));return true
    }
    @Synchronized fun erase() {
        // Persist intent first: a killed process must finish erasing before any later read/write.
        check(prefs.edit().clear().putLong("epoch",epoch()+1).putLong("erasureEpoch",erasureEpoch()+1)
            .putBoolean("erasePending",true).putLong("cursor",System.currentTimeMillis()).commit())
        finishErase()
    }
    private fun finishErase() {
        db?.close();db=null
        check(context.deleteDatabase("events.db") || !context.getDatabasePath("events.db").exists())
        KeyStore.getInstance("AndroidKeyStore").apply { load(null); deleteEntry(alias) }
        check(prefs.edit().putBoolean("erasePending",false).commit())
    }
    companion object {
        @Volatile private var instance: LocalStore?=null
        fun get(context: Context): LocalStore = instance ?: synchronized(this) {
            instance ?: LocalStore(context.applicationContext).also { instance=it }
        }
    }
}
