package com.example.util

import android.app.DownloadManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.app.NotificationCompat
import com.example.data.DownloadHistoryManager
import com.example.data.DownloadRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

object DownloadHelper {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .retryOnConnectionFailure(true)
        .build()

    private const val CHANNEL_ID = "yt_downloader_channel"
    private const val CHANNEL_NAME = "Status Unduhan"
    private const val REFERER_URL = "https://iframe.y2meta-uk.com/"
    private const val USER_AGENT = "Mozilla/5.0 (Linux; Android 13; Mobile) AppleWebKit/537.36"

    fun extractYouTubeId(url: String): String? {

        val trimmed = url.trim()
        if (trimmed.isEmpty()) return null

        val patterns = listOf(
            "(?<=watch\\?v=|/videos/|embed/|youtu.be/|/v/|/e/|shorts/)[^#&?\\n]+",
            "youtu\\.be/([^#&?\\n]+)",
            "shorts/([^#&?\\n]+)"
        )
        for (pat in patterns) {
            val matcher = Pattern.compile(pat).matcher(trimmed)
            if (matcher.find()) {
                val groupCount = matcher.groupCount()
                return if (groupCount >= 1) matcher.group(1) else matcher.group()
            }
        }
        return try {
            Uri.parse(trimmed).getQueryParameter("v")
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Downloads file directly into public Download folder using OkHttp streaming.
     * Passes the required Referer and User-Agent headers to prevent Cloudflare 403 blocks.
     */
    suspend fun downloadDirectly(
        context: Context,
        downloadUrl: String,
        suggestedFileName: String,
        mimeType: String,
        videoTitle: String,
        onProgress: (percent: Int, downloadedBytes: Long, totalBytes: Long) -> Unit
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            var fileName = suggestedFileName.trim()
                .replace("[\\\\/:*?\"<>|]".toRegex(), "_")
            if (!fileName.contains(".")) {
                fileName += if (mimeType.contains("audio") || mimeType.contains("mpeg")) ".mp3" else ".mp4"
            }

            val headerStrategies = listOf(
                // Strategy 1: Clean Referer with standard mobile User-Agent (No Origin to prevent CORS block on GET)
                mapOf(
                    "Referer" to REFERER_URL,
                    "User-Agent" to USER_AGENT,
                    "Accept" to "*/*",
                    "Connection" to "close"
                ),
                // Strategy 2: With Origin included
                mapOf(
                    "Referer" to REFERER_URL,
                    "Origin" to "https://iframe.y2meta-uk.com",
                    "User-Agent" to USER_AGENT,
                    "Accept" to "*/*",
                    "Connection" to "close"
                ),
                // Strategy 3: Referer to cnv.cx
                mapOf(
                    "Referer" to "https://cnv.cx/",
                    "User-Agent" to USER_AGENT,
                    "Accept" to "*/*",
                    "Connection" to "close"
                ),
                // Strategy 4: Fallback standard browser headers
                mapOf(
                    "Referer" to "https://cnvmp3.com/",
                    "User-Agent" to USER_AGENT,
                    "Accept" to "*/*",
                    "Connection" to "close"
                )
            )

            var response: okhttp3.Response? = null
            var lastErrorCode = 0

            for (headers in headerStrategies) {
                try {
                    val reqBuilder = Request.Builder().url(downloadUrl)
                    headers.forEach { (k, v) -> reqBuilder.header(k, v) }
                    val resp = client.newCall(reqBuilder.build()).execute()
                    if (resp.isSuccessful) {
                        response = resp
                        break
                    } else {
                        lastErrorCode = resp.code
                        resp.close()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            if (response == null || !response.isSuccessful) {
                val code = if (lastErrorCode != 0) lastErrorCode else 403
                return@withContext Result.failure(Exception("HTTP error dari server: $code"))
            }

            val body = response.body ?: return@withContext Result.failure(Exception("File kosong"))
            val totalBytes = body.contentLength()

            val resolver = context.contentResolver
            var outputStream: OutputStream? = null
            var finalUri: Uri? = null

            // Avoid file collisions on repeated downloads
            var uniqueFileName = fileName
            val nameWithoutExt = fileName.substringBeforeLast(".")
            val ext = if (fileName.contains(".")) "." + fileName.substringAfterLast(".") else ""

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Ensure unique name in MediaStore
                var counter = 1
                while (true) {
                    val projection = arrayOf(MediaStore.MediaColumns.DISPLAY_NAME)
                    val selection = "${MediaStore.MediaColumns.DISPLAY_NAME} = ?"
                    val selectionArgs = arrayOf(uniqueFileName)
                    val cursor = resolver.query(
                        MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                        projection,
                        selection,
                        selectionArgs,
                        null
                    )
                    val exists = (cursor?.use { it.count } ?: 0) > 0
                    if (exists) {
                        uniqueFileName = "$nameWithoutExt ($counter)$ext"
                        counter++
                    } else {
                        break
                    }
                }

                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, uniqueFileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
                val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI
                finalUri = resolver.insert(collection, values)
                    ?: return@withContext Result.failure(Exception("Gagal mengalokasikan file di folder Unduhan"))
                outputStream = resolver.openOutputStream(finalUri)
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) downloadsDir.mkdirs()
                var counter = 1
                var targetFile = File(downloadsDir, uniqueFileName)
                while (targetFile.exists()) {
                    uniqueFileName = "$nameWithoutExt ($counter)$ext"
                    targetFile = File(downloadsDir, uniqueFileName)
                    counter++
                }
                outputStream = FileOutputStream(targetFile)
                finalUri = Uri.fromFile(targetFile)
            }

            val inputStream: InputStream = body.byteStream()
            val buffer = ByteArray(32 * 1024)
            var bytesRead: Int
            var totalRead = 0L
            var lastReportTime = 0L

            outputStream?.use { out ->
                inputStream.use { inStream ->
                    while (inStream.read(buffer).also { bytesRead = it } != -1) {
                        out.write(buffer, 0, bytesRead)
                        totalRead += bytesRead
                        val now = System.currentTimeMillis()
                        if (now - lastReportTime > 250 || totalRead == totalBytes) {
                            lastReportTime = now
                            val percent = if (totalBytes > 0) ((totalRead * 100) / totalBytes).toInt() else -1
                            onProgress(percent, totalRead, totalBytes)
                        }
                    }
                    out.flush()
                }
            }

            // Release IS_PENDING on Android 10+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && finalUri != null) {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.IS_PENDING, 0)
                }
                resolver.update(finalUri, values, null, null)
            }

            // Media scan for older androids
            try {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                    val path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath + "/" + uniqueFileName
                    MediaScannerConnection.scanFile(context, arrayOf(path), arrayOf(mimeType), null)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // Save record to history
            val format = if (uniqueFileName.endsWith(".mp3", ignoreCase = true) || mimeType.contains("audio")) "MP3" else "MP4"
            val historyManager = DownloadHistoryManager(context)
            historyManager.addRecord(
                DownloadRecord(
                    id = System.currentTimeMillis(),
                    title = videoTitle.ifBlank { uniqueFileName },
                    url = downloadUrl,
                    format = format,
                    fileName = uniqueFileName
                )
            )

            // Trigger completion notification
            showCompletionNotification(context, uniqueFileName, finalUri, mimeType)

            Result.success(uniqueFileName)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        } finally {
            try {
                client.connectionPool.evictAll()
            } catch (_: Exception) {}
        }
    }

    private fun showCompletionNotification(context: Context, fileName: String, fileUri: Uri?, mimeType: String) {
        try {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Pemberitahuan ketika unduhan video atau musik selesai"
                }
                notificationManager.createNotificationChannel(channel)
            }

            val openIntent = Intent(Intent.ACTION_VIEW).apply {
                if (fileUri != null) {
                    setDataAndType(fileUri, mimeType)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } else {
                    action = DownloadManager.ACTION_VIEW_DOWNLOADS
                }
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                fileName.hashCode(),
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setContentTitle("Unduhan Berhasil")
                .setContentText(fileName)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)

            notificationManager.notify(fileName.hashCode(), builder.build())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun openDownloadsFolder(context: Context) {
        try {
            val intent = Intent(DownloadManager.ACTION_VIEW_DOWNLOADS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                    type = "*/*"
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(Intent.createChooser(intent, "Buka Folder Unduhan"))
            } catch (e2: Exception) {
                // Ignore
            }
        }
    }
}
