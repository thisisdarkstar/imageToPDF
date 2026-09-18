package com.example.imagetopdf.data

import android.content.Context
import android.content.SharedPreferences
import com.example.imagetopdf.model.DraftRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class DraftRepository(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("pdf_drafts_prefs", Context.MODE_PRIVATE)

    private val _draftsFlow = MutableStateFlow<List<DraftRecord>>(emptyList())
    val draftsFlow: StateFlow<List<DraftRecord>> = _draftsFlow.asStateFlow()

    init {
        loadDrafts()
    }

    private fun loadDrafts() {
        val jsonString = prefs.getString(KEY_DRAFTS, null) ?: "[]"
        val list = mutableListOf<DraftRecord>()
        try {
            val array = JSONArray(jsonString)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val uriArray = obj.optJSONArray("pageUris") ?: JSONArray()
                val rotArray = obj.optJSONArray("pageRotations") ?: JSONArray()
                val filtArray = obj.optJSONArray("pageFilters") ?: JSONArray()
                val uris = (0 until uriArray.length()).map { uriArray.getString(it) }
                val rotations = (0 until rotArray.length()).map { rotArray.getInt(it) }
                val filters = (0 until filtArray.length()).map { filtArray.getString(it) }
                list.add(DraftRecord(
                    id = obj.optString("id"),
                    name = obj.optString("name", "Draft"),
                    pageUris = uris,
                    pageRotations = rotations,
                    pageFilters = filters,
                    savedAt = obj.optLong("savedAt", obj.optLong("createdAt", System.currentTimeMillis()))
                ))
            }
        } catch (_: Exception) {}
        list.sortByDescending { it.savedAt }
        _draftsFlow.value = list
    }

    suspend fun saveDraft(draft: DraftRecord) = withContext(Dispatchers.IO) {
        val current = _draftsFlow.value.toMutableList()
        current.add(0, draft)
        saveList(current)
        _draftsFlow.value = current
    }

    suspend fun deleteDraft(id: String) = withContext(Dispatchers.IO) {
        val updated = _draftsFlow.value.filterNot { it.id == id }
        saveList(updated)
        _draftsFlow.value = updated
    }

    private fun saveList(list: List<DraftRecord>) {
        val array = JSONArray()
        for (draft in list) {
            val obj = JSONObject()
            obj.put("id", draft.id)
            obj.put("name", draft.name)
            obj.put("savedAt", draft.savedAt)
            val uriArr = JSONArray(); draft.pageUris.forEach { uriArr.put(it) }; obj.put("pageUris", uriArr)
            val rotArr = JSONArray(); draft.pageRotations.forEach { rotArr.put(it) }; obj.put("pageRotations", rotArr)
            val filtArr = JSONArray(); draft.pageFilters.forEach { filtArr.put(it) }; obj.put("pageFilters", filtArr)
            array.put(obj)
        }
        prefs.edit().putString(KEY_DRAFTS, array.toString()).apply()
    }

    companion object {
        private const val KEY_DRAFTS = "key_pdf_drafts"
    }
}
