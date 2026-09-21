package io.github.thisisdarkstar.imagetopdf.data

import android.content.Context
import android.content.SharedPreferences
import io.github.thisisdarkstar.imagetopdf.model.PdfRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class DocumentHistoryRepository(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("pdf_history_prefs", Context.MODE_PRIVATE)

    private val _historyFlow = MutableStateFlow<List<PdfRecord>>(emptyList())
    val historyFlow: StateFlow<List<PdfRecord>> = _historyFlow.asStateFlow()

    init {
        loadHistory()
    }

    fun loadHistory() {
        val jsonString = prefs.getString(KEY_RECORDS, null) ?: "[]"
        val list = mutableListOf<PdfRecord>()
        try {
            val array = JSONArray(jsonString)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val path = obj.optString("filePath")
                val file = File(path)
                // Only include if file still exists on disk
                if (file.exists()) {
                    list.add(
                        PdfRecord(
                            id = obj.optString("id"),
                            fileName = obj.optString("fileName", file.name),
                            filePath = path,
                            fileSizeBytes = if (obj.has("fileSizeBytes")) obj.getLong("fileSizeBytes") else file.length(),
                            pageCount = obj.optInt("pageCount", 1),
                            createdAt = obj.optLong("createdAt", file.lastModified())
                        )
                    )
                }
            }
        } catch (_: Exception) {
            // fallback
        }
        list.sortByDescending { it.createdAt }
        _historyFlow.value = list
    }

    suspend fun addRecord(record: PdfRecord) = withContext(Dispatchers.IO) {
        val current = _historyFlow.value.toMutableList()
        current.removeAll { it.filePath == record.filePath }
        current.add(0, record)
        saveList(current)
        _historyFlow.value = current
    }

    suspend fun deleteRecord(record: PdfRecord) = withContext(Dispatchers.IO) {
        try {
            val file = File(record.filePath)
            if (file.exists()) {
                file.delete()
            }
        } catch (_: Exception) { }

        val updated = _historyFlow.value.filterNot { it.id == record.id }
        saveList(updated)
        _historyFlow.value = updated
    }

    private fun saveList(list: List<PdfRecord>) {
        val array = JSONArray()
        for (item in list) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("fileName", item.fileName)
                put("filePath", item.filePath)
                put("fileSizeBytes", item.fileSizeBytes)
                put("pageCount", item.pageCount)
                put("createdAt", item.createdAt)
            }
            array.put(obj)
        }
        prefs.edit().putString(KEY_RECORDS, array.toString()).apply()
    }

    companion object {
        private const val KEY_RECORDS = "key_pdf_records"
    }
}
