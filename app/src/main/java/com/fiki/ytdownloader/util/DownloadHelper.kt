package com.fiki.ytdownloader.util

import android.app.DownloadManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import com.fiki.ytdownloader.data.DownloadHistoryManager
import com.fiki.ytdownloader.data.DownloadRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.SocketException
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

    fun extractUrlFromText(text: String): String? {
        val pattern = Pattern.compile("https?://[^\\s]+")
        val matcher = pattern.matcher(text)
        return if (matcher.find()) {
            matcher.group()
        } else {
            null
        }
    }

    /**
     * Gets or creates the application's dedicated subfolder:
     * e.g. /storage/emulated/0/YTDownloader/mp3 or /storage/emulated/0/YTDownloader/mp4
     */
    fun getTargetDirectory(isAudio: Boolean): File {
        val subFolder = if (isAudio) "mp3" else "mp4"
        val dir = File(Environment.getExternalStorageDirectory(), "YTDownloader/$subFolder")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * Downloads file directly into /storage/emulated/0/YTDownloader/mp3 or /mp4.
     * Includes auto-retry for transient socket drops.
     */
    suspend fun downloadDirectly(
        context: Context,
        downloadUrl: String,
        suggestedFileName: String,
        mimeType: String,
        videoTitle: String,
        onProgress: (percent: Int, downloadedBytes: Long, totalBytes: Long) -> Unit
    ): Result<String> = withContext(Dispatchers.IO) {
        var lastError: Exception? = null

        val isAudio = mimeType.contains("audio", ignoreCase = true) || suggestedFileName.endsWith(".mp3", ignoreCase = true)
        val ext = if (isAudio) ".mp3" else ".mp4"

        var cleanName = suggestedFileName.trim()
            .replace("[\\\\/:*?\"<>|]".toRegex(), "_")
        if (!cleanName.contains(".")) {
            cleanName += ext
        }

        val targetDir = getTargetDirectory(isAudio)

        // Ensure unique filename
        val nameWithoutExt = cleanName.substringBeforeLast(".")
        var uniqueFileName = cleanName
        var counter = 1
        var targetFile = File(targetDir, uniqueFileName)
        while (targetFile.exists()) {
            uniqueFileName = "$nameWithoutExt ($counter)$ext"
            targetFile = File(targetDir, uniqueFileName)
            counter++
        }

        val headerStrategies = listOf(
            mapOf(
                "Referer" to REFERER_URL,
                "User-Agent" to USER_AGENT,
                "Accept" to "*/*"
            ),
            mapOf(
                "Referer" to REFERER_URL,
                "Origin" to "https://iframe.y2meta-uk.com",
                "User-Agent" to USER_AGENT,
                "Accept" to "*/*"
            ),
            mapOf(
                "Referer" to "https://cnv.cx/",
                "User-Agent" to USER_AGENT,
                "Accept" to "*/*"
            ),
            mapOf(
                "Referer" to "https://cnvmp3.com/",
                "User-Agent" to USER_AGENT,
                "Accept" to "*/*"
            )
        )

        // Retry loop for transient socket closures
        for (attempt in 1..3) {
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
                    // Try next header strategy
                }
            }

            if (response == null || !response.isSuccessful) {
                lastError = Exception("HTTP error dari server: ${if (lastErrorCode != 0) lastErrorCode else 403}")
                if (attempt < 3) {
                    delay(1200)
                    continue
                }
                return@withContext Result.failure(lastError)
            }

            val body = response.body
            if (body == null) {
                lastError = Exception("File kosong dari server")
                if (attempt < 3) {
                    delay(1000)
                    continue
                }
                return@withContext Result.failure(lastError)
            }

            val totalBytes = body.contentLength()

            try {
                val outputStream: OutputStream = FileOutputStream(targetFile)
                val inputStream: InputStream = body.byteStream()
                val buffer = ByteArray(32 * 1024)
                var bytesRead: Int
                var totalRead = 0L
                var lastReportTime = 0L

                outputStream.use { out ->
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

                // Notify Android Media Indexer so it appears in Music/Gallery immediately
                try {
                    MediaScannerConnection.scanFile(
                        context,
                        arrayOf(targetFile.absolutePath),
                        arrayOf(if (isAudio) "audio/mpeg" else "video/mp4"),
                        null
                    )
                } catch (_: Exception) {}

                // Save record with exact file path
                val historyManager = DownloadHistoryManager(context)
                historyManager.addRecord(
                    DownloadRecord(
                        id = System.currentTimeMillis(),
                        title = videoTitle.ifBlank { uniqueFileName },
                        url = downloadUrl,
                        format = if (isAudio) "MP3" else "MP4",
                        fileName = uniqueFileName,
                        filePath = targetFile.absolutePath
                    )
                )

                showCompletionNotification(context, uniqueFileName, targetFile, if (isAudio) "audio/mpeg" else "video/mp4")

                return@withContext Result.success(targetFile.absolutePath)
            } catch (e: SocketException) {
                lastError = e
                targetFile.delete() // Clean partial file
                if (attempt < 3) {
                    delay(1200)
                    continue
                }
            } catch (e: Exception) {
                lastError = e
                targetFile.delete()
                if (attempt < 3) {
                    delay(1000)
                    continue
                }
            }
        }

        Result.failure(lastError ?: Exception("Gagal mengunduh file"))
    }

    private fun showCompletionNotification(context: Context, fileName: String, file: File, mimeType: String) {
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
                setDataAndType(Uri.fromFile(file), mimeType)
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
                .setContentTitle("Unduhan Berhasil Disimpan")
                .setContentText(fileName)
                .setSubText(if (fileName.endsWith(".mp3", true)) "YTDownloader/mp3" else "YTDownloader/mp4")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)

            notificationManager.notify(fileName.hashCode(), builder.build())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun openAppFolder(context: Context, subFolder: String? = null) {
        try {
            val folderPath = if (subFolder != null) {
                File(Environment.getExternalStorageDirectory(), "YTDownloader/$subFolder")
            } else {
                File(Environment.getExternalStorageDirectory(), "YTDownloader")
            }
            if (!folderPath.exists()) folderPath.mkdirs()

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
                Toast.makeText(context, "Folder: /storage/emulated/0/YTDownloader/${subFolder ?: ""}", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun openDownloadsFolder(context: Context) {
        openAppFolder(context, null)
    }

    fun openFile(context: Context, record: DownloadRecord) {
        try {
            val file = if (record.filePath.isNotBlank()) {
                File(record.filePath)
            } else {
                val isAudio = record.format.equals("mp3", ignoreCase = true)
                File(getTargetDirectory(isAudio), record.fileName)
            }

            if (!file.exists()) {
                Toast.makeText(context, "File tidak ditemukan di penyimpanan", Toast.LENGTH_SHORT).show()
                return
            }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val mimeType = if (record.format.equals("mp3", ignoreCase = true)) "audio/*" else "video/*"
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Buka file"))
        } catch (e: Exception) {
            Toast.makeText(context, "Gagal membuka file: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun hasStoragePermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }
    }

    fun requestStoragePermission(context: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val intent = Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.parse("package:${context.packageName}")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            } else {
                if (context is android.app.Activity) {
                    androidx.core.app.ActivityCompat.requestPermissions(
                        context,
                        arrayOf(
                            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            android.Manifest.permission.READ_EXTERNAL_STORAGE
                        ),
                        101
                    )
                }
            }
        } catch (e: Exception) {
            try {
                val intent = Intent(android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(intent)
            } catch (e2: Exception) {
                Toast.makeText(context, "Buka Pengaturan -> Izin Aplikasi untuk mengizinkan akses file", Toast.LENGTH_LONG).show()
            }
        }
    }
}
