package com.loyalstring.rfid.ui.utils

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.loyalstring.rfid.data.model.deliveryChallan.DeliveryChallanListRow
import com.loyalstring.rfid.data.model.deliveryChallan.DeliveryChallanResponseList
import com.loyalstring.rfid.data.model.deliveryChallan.toListRow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import java.io.File

/**
 * Fast local cache for Delivery Challan list (~thousands of rows).
 *
 * - Rows: one lightweight JSON file (list UI only)
 * - Full challan: one JSON file per Id (print/edit) — never one giant prefs blob
 */
object DeliveryChallanListCache {
    private const val PREFS_NAME = "delivery_challan_list_cache"
    private const val DIR_NAME = "delivery_challan_cache"

    private val gson = Gson()
    private val memoryRows = mutableMapOf<String, List<DeliveryChallanListRow>>()

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun baseKey(clientCode: String, branchId: Int) =
        "${clientCode.trim().lowercase()}_$branchId"

    private fun rowsKey(clientCode: String, branchId: Int) = "rows_${baseKey(clientCode, branchId)}"
    private fun fullKey(clientCode: String, branchId: Int) = "full_${baseKey(clientCode, branchId)}"
    private fun legacyKey(clientCode: String, branchId: Int) = baseKey(clientCode, branchId)

    private fun cacheDir(context: Context, clientCode: String, branchId: Int): File {
        val dir = File(context.filesDir, "$DIR_NAME/${baseKey(clientCode, branchId)}")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun rowsFile(context: Context, clientCode: String, branchId: Int) =
        File(cacheDir(context, clientCode, branchId), "rows.json")

    private fun fullDir(context: Context, clientCode: String, branchId: Int): File {
        val dir = File(cacheDir(context, clientCode, branchId), "full")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun fullFile(context: Context, clientCode: String, branchId: Int, id: Int) =
        File(fullDir(context, clientCode, branchId), "$id.json")

    fun readMemoryRows(clientCode: String, branchId: Int): List<DeliveryChallanListRow>? =
        memoryRows[baseKey(clientCode, branchId)]

    private fun writeMemoryRows(clientCode: String, branchId: Int, rows: List<DeliveryChallanListRow>) {
        memoryRows[baseKey(clientCode, branchId)] = rows
    }

    /** Publish progressive stream rows so list UI can paint while API is still reading. */
    fun putMemoryRows(clientCode: String, branchId: Int, rows: List<DeliveryChallanListRow>) {
        if (rows.isEmpty()) return
        val existing = readMemoryRows(clientCode, branchId).orEmpty()
        // Keep locally-saved rows (e.g. just-added challan) that API has not returned yet.
        val merged = (existing + rows).distinctBy { it.Id }.sortedByDescending { it.Id }
        writeMemoryRows(clientCode, branchId, merged)
    }

    /**
     * If the old SharedPreferences XML is huge (full challan dump), delete the file
     * BEFORE opening it. Opening a multi‑MB prefs file loads it all into RAM and ANRs.
     * Lightweight rows live in [rowsFile] going forward.
     */
    fun discardOversizedPrefsFile(context: Context) {
        val dir = File(context.applicationInfo.dataDir, "shared_prefs")
        val prefsXml = File(dir, "$PREFS_NAME.xml")
        val bak = File(dir, "$PREFS_NAME.xml.bak")
        val maxBytes = 400_000L // ~400KB — rows for 2000 challans fit; full dump does not
        if (prefsXml.exists() && prefsXml.length() > maxBytes) {
            prefsXml.delete()
            bak.delete()
        }
    }

    /**
     * Drop legacy giant full/list blobs from SharedPreferences without parsing.
     * Prefer [discardOversizedPrefsFile] before first prefs access.
     */
    fun purgeLegacyFullPrefs(context: Context, clientCode: String, branchId: Int) {
        discardOversizedPrefsFile(context)
        val p = prefs(context)
        val fk = fullKey(clientCode, branchId)
        val lk = legacyKey(clientCode, branchId)
        if (!p.contains(fk) && !p.contains(lk)) return
        p.edit().remove(fk).remove(lk).commit()
    }

    /** Fast path: memory → rows file → legacy prefs rows (never parses full payload). */
    fun readDiskRowsOnly(
        context: Context,
        clientCode: String,
        branchId: Int,
    ): List<DeliveryChallanListRow> {
        readMemoryRows(clientCode, branchId)?.let { return it }

        val file = rowsFile(context, clientCode, branchId)
        if (file.exists()) {
            val rows = parseRows(file.readText())
            if (rows.isNotEmpty()) {
                writeMemoryRows(clientCode, branchId, rows)
                // Prefer file cache; strip any leftover giant prefs keys.
                purgeLegacyFullPrefs(context, clientCode, branchId)
                return rows
            }
        }

        // Prefer reading only rows_ key; purge full blobs first so prefs XML is small.
        purgeLegacyFullPrefs(context, clientCode, branchId)

        val prefsJson = prefs(context).getString(rowsKey(clientCode, branchId), null)
        if (!prefsJson.isNullOrBlank()) {
            val rows = parseRows(prefsJson)
            if (rows.isNotEmpty()) {
                writeMemoryRows(clientCode, branchId, rows)
                runCatching {
                    file.writeText(prefsJson)
                    prefs(context).edit()
                        .remove(rowsKey(clientCode, branchId))
                        .apply()
                }
                return rows
            }
        }
        return emptyList()
    }

    /**
     * Slow: only when no rows file exists. Extracts lightweight rows from legacy full prefs,
     * then clears the giant prefs blob so future opens stay fast.
     */
    suspend fun migrateLegacyIfNeeded(
        context: Context,
        clientCode: String,
        branchId: Int,
    ) = withContext(Dispatchers.IO) {
        if (rowsFile(context, clientCode, branchId).exists()) return@withContext
        if (readMemoryRows(clientCode, branchId)?.isNotEmpty() == true) return@withContext

        val p = prefs(context)
        val legacyJson = p.getString(fullKey(clientCode, branchId), null)
            ?: p.getString(legacyKey(clientCode, branchId), null)
            ?: return@withContext

        val fullList = parseFull(legacyJson)
        if (fullList.isEmpty()) {
            p.edit()
                .remove(fullKey(clientCode, branchId))
                .remove(legacyKey(clientCode, branchId))
                .apply()
            return@withContext
        }

        val rows = fullList.map { it.toListRow() }.distinctBy { it.Id }.sortedByDescending { it.Id }
        saveRows(context, clientCode, branchId, rows)
        // Do NOT rewrite 2000 full prefs objects here — next API sync writes per-id files.
        // Keep process memory free; print/edit use in-memory map after sync.
        p.edit()
            .remove(rowsKey(clientCode, branchId))
            .remove(fullKey(clientCode, branchId))
            .remove(legacyKey(clientCode, branchId))
            .apply()
    }

    suspend fun loadRows(
        context: Context,
        clientCode: String,
        branchId: Int,
    ): List<DeliveryChallanListRow> = withContext(Dispatchers.IO) {
        val fast = readDiskRowsOnly(context, clientCode, branchId)
        if (fast.isNotEmpty()) return@withContext fast
        migrateLegacyIfNeeded(context, clientCode, branchId)
        readDiskRowsOnly(context, clientCode, branchId)
    }

    suspend fun saveRows(
        context: Context,
        clientCode: String,
        branchId: Int,
        rows: List<DeliveryChallanListRow>,
    ) = withContext(Dispatchers.IO) {
        writeMemoryRows(clientCode, branchId, rows)
        val type = object : TypeToken<List<DeliveryChallanListRow>>() {}.type
        val json = gson.toJson(rows, type)
        rowsFile(context, clientCode, branchId).writeText(json)
        // Clear old prefs blobs if present (they cause ANR with ~2000 full challans).
        prefs(context).edit()
            .remove(rowsKey(clientCode, branchId))
            .remove(fullKey(clientCode, branchId))
            .remove(legacyKey(clientCode, branchId))
            .apply()
    }

    /** Load a single challan for print/edit — never parse the whole list. */
    suspend fun loadFullById(
        context: Context,
        clientCode: String,
        branchId: Int,
        id: Int,
    ): DeliveryChallanResponseList? = withContext(Dispatchers.IO) {
        val file = fullFile(context, clientCode, branchId, id)
        if (!file.exists()) return@withContext null
        runCatching {
            gson.fromJson(file.readText(), DeliveryChallanResponseList::class.java)
        }.getOrNull()
    }

    /** Write raw API object JSON (from streamer) — no DeliveryChallanResponseList allocation. */
    fun saveRawJsonById(
        context: Context,
        clientCode: String,
        branchId: Int,
        id: Int,
        rawJson: String,
    ) {
        if (id == 0 || rawJson.isBlank()) return
        runCatching {
            fullFile(context, clientCode, branchId, id).writeText(rawJson)
        }
    }

    suspend fun saveRawJsonBatch(
        context: Context,
        clientCode: String,
        branchId: Int,
        rawById: Map<Int, String>,
        batchSize: Int = 80,
    ) = withContext(Dispatchers.IO) {
        if (rawById.isEmpty()) return@withContext
        val dir = fullDir(context, clientCode, branchId)
        val writtenIds = HashSet<Int>(rawById.size)
        rawById.entries.chunked(batchSize).forEach { batch ->
            batch.forEach { (id, raw) ->
                writtenIds.add(id)
                saveRawJsonById(context, clientCode, branchId, id, raw)
            }
            yield()
        }
        dir.listFiles()?.forEach { file ->
            val id = file.nameWithoutExtension.toIntOrNull() ?: return@forEach
            if (id !in writtenIds) file.delete()
        }
        prefs(context).edit()
            .remove(fullKey(clientCode, branchId))
            .remove(legacyKey(clientCode, branchId))
            .apply()
    }

    @Deprecated("Use loadFullById — loading entire full list ANRs with thousands of records")
    suspend fun loadFull(
        context: Context,
        clientCode: String,
        branchId: Int,
    ): List<DeliveryChallanResponseList> = withContext(Dispatchers.IO) {
        val dir = fullDir(context, clientCode, branchId)
        val files = dir.listFiles()?.filter { it.extension == "json" }.orEmpty()
        if (files.isEmpty()) return@withContext emptyList()
        files.mapNotNull { file ->
            runCatching {
                gson.fromJson(file.readText(), DeliveryChallanResponseList::class.java)
            }.getOrNull()
        }
    }

    /** Persist full challans as one file per Id (batched). Safe for ~2000 records. */
    suspend fun saveFullPerIdBatch(
        context: Context,
        clientCode: String,
        branchId: Int,
        list: List<DeliveryChallanResponseList>,
        batchSize: Int = 100,
    ) = withContext(Dispatchers.IO) {
        val dir = fullDir(context, clientCode, branchId)
        // Replace set for this branch: remove stale ids after writing new ones.
        val writtenIds = HashSet<Int>(list.size)
        list.chunked(batchSize).forEach { batch ->
            batch.forEach { full ->
                if (full == null) return@forEach
                writtenIds.add(full.Id)
                runCatching {
                    fullFile(context, clientCode, branchId, full.Id)
                        .writeText(gson.toJson(full))
                }
            }
            yield()
        }
        dir.listFiles()?.forEach { file ->
            val id = file.nameWithoutExtension.toIntOrNull() ?: return@forEach
            if (id !in writtenIds) file.delete()
        }
        prefs(context).edit()
            .remove(fullKey(clientCode, branchId))
            .remove(legacyKey(clientCode, branchId))
            .apply()
    }

    @Deprecated("Use saveFullPerIdBatch")
    suspend fun saveFull(
        context: Context,
        clientCode: String,
        branchId: Int,
        list: List<DeliveryChallanResponseList>,
    ) = saveFullPerIdBatch(context, clientCode, branchId, list)

    fun mergeRowIntoMemory(
        clientCode: String,
        branchId: Int,
        row: DeliveryChallanListRow,
    ): List<DeliveryChallanListRow> {
        val existing = readMemoryRows(clientCode, branchId).orEmpty()
        val mergedRows = (listOf(row) + existing.filterNot { it.Id == row.Id })
            .sortedByDescending { it.Id }
        writeMemoryRows(clientCode, branchId, mergedRows)
        return mergedRows
    }

    suspend fun upsertChallan(
        context: Context,
        clientCode: String,
        branchId: Int,
        full: DeliveryChallanResponseList,
        row: DeliveryChallanListRow,
    ) = withContext(Dispatchers.IO) {
        val existingRows = loadRows(context, clientCode, branchId)
            .filterNot { it.Id == row.Id }
        val mergedRows = (listOf(row) + existingRows).sortedByDescending { it.Id }
        saveRows(context, clientCode, branchId, mergedRows)
        runCatching {
            fullFile(context, clientCode, branchId, full.Id)
                .writeText(gson.toJson(full))
        }
    }

    suspend fun removeChallan(
        context: Context,
        clientCode: String,
        branchId: Int,
        id: Int,
        remainingRows: List<DeliveryChallanListRow>,
    ) = withContext(Dispatchers.IO) {
        saveRows(context, clientCode, branchId, remainingRows)
        fullFile(context, clientCode, branchId, id).delete()
    }

    private fun parseRows(json: String): List<DeliveryChallanListRow> = runCatching {
        val type = object : TypeToken<List<DeliveryChallanListRow>>() {}.type
        gson.fromJson<List<DeliveryChallanListRow>>(json, type).orEmpty()
    }.getOrDefault(emptyList())

    private fun parseFull(json: String): List<DeliveryChallanResponseList> = runCatching {
        val type = object : TypeToken<List<DeliveryChallanResponseList>>() {}.type
        gson.fromJson<List<DeliveryChallanResponseList>>(json, type).orEmpty()
    }.getOrDefault(emptyList())
}
