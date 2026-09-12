package com.fiki.ytdownloader.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

data class DownloadRecord(
    val id: Long,
    val title: String,
    val url: String,
    val format: String, // "MP4" or "MP3"
    val timestamp: Long = System.currentTimeMillis(),
    val fileName: String = "",
    val filePath: String = ""
)

class DownloadHistoryManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("download_history_prefs", Context.MODE_PRIVATE)

    fun getRecords(): List<DownloadRecord> {
        val jsonString = prefs.getString("records", "[]") ?: "[]"
        val list = mutableListOf<DownloadRecord>()
        try {
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    DownloadRecord(
                        id = obj.optLong("id", 0L),
                        title = obj.optString("title", "Unknown"),
                        url = obj.optString("url", ""),
                        format = obj.optString("format", "MP4"),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                        fileName = obj.optString("fileName", ""),
                        filePath = obj.optString("filePath", "")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list.sortedByDescending { it.timestamp }
    }

    fun addRecord(record: DownloadRecord) {
        val current = getRecords().toMutableList()
        current.add(0, record)
        val jsonArray = JSONArray()
        current.take(50).forEach { item ->
            val obj = JSONObject().apply {
                put("id", item.id)
                put("title", item.title)
                put("url", item.url)
                put("format", item.format)
                put("timestamp", item.timestamp)
                put("fileName", item.fileName)
                put("filePath", item.filePath)
            }
            jsonArray.put(obj)
        }
        prefs.edit().putString("records", jsonArray.toString()).apply()
    }

    fun removeRecord(id: Long) {
        val current = getRecords().filter { it.id != id }
        val jsonArray = JSONArray()
        current.forEach { item ->
            val obj = JSONObject().apply {
                put("id", item.id)
                put("title", item.title)
                put("url", item.url)
                put("format", item.format)
                put("timestamp", item.timestamp)
                put("fileName", item.fileName)
                put("filePath", item.filePath)
            }
            jsonArray.put(obj)
        }
        prefs.edit().putString("records", jsonArray.toString()).apply()
    }
}
