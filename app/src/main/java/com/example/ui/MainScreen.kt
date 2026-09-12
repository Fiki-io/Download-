package com.example.ui

import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar

import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.DownloadHistoryManager
import com.example.data.DownloadRecord
import com.example.engine.AvailableQuality
import com.example.engine.EngineState
import com.example.engine.Y2MateNativeEngine
import com.example.ui.theme.VsCodeBackground
import com.example.ui.theme.VsCodeBlue
import com.example.ui.theme.VsCodeBlueDark
import com.example.ui.theme.VsCodeBlueLight
import com.example.ui.theme.VsCodeBorder
import com.example.ui.theme.VsCodeGreen
import com.example.ui.theme.VsCodeInputBg
import com.example.ui.theme.VsCodeOrange
import com.example.ui.theme.VsCodePanel
import com.example.ui.theme.VsCodeRed
import com.example.ui.theme.VsCodeSidebar
import com.example.ui.theme.VsCodeText
import com.example.ui.theme.VsCodeTextMuted
import com.example.util.DownloadHelper
import com.example.util.VideoInfo
import com.example.util.YouTubeOEmbedHelper
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current

    var selectedNavIndex by remember { mutableIntStateOf(0) }
    var inputUrl by remember { mutableStateOf("") }
    var activeVideoInfo by remember { mutableStateOf<VideoInfo?>(null) }
    var isFetchingInfo by remember { mutableStateOf(false) }

    val historyManager = remember { DownloadHistoryManager(context) }
    var downloadHistory by remember { mutableStateOf(historyManager.getRecords()) }

    var engineState by remember { mutableStateOf<EngineState>(EngineState.Idle) }
    var availableQualities by remember { mutableStateOf<List<AvailableQuality>>(emptyList()) }
    var selectedQuality by remember { mutableStateOf<AvailableQuality?>(null) }

    val processUrl = { urlToProcess: String ->
        val trimmed = urlToProcess.trim()
        val videoId = DownloadHelper.extractYouTubeId(trimmed)
        if (videoId != null) {
            keyboardController?.hide()
            isFetchingInfo = true
            availableQualities = emptyList()
            selectedQuality = null
            engineState = EngineState.Idle


            // Fetch video info and format qualities natively
            scope.launch {
                val info = YouTubeOEmbedHelper.fetchVideoInfo(videoId)
                activeVideoInfo = info
                isFetchingInfo = false
                val qualities = Y2MateNativeEngine.getAvailableQualities(videoId)
                availableQualities = qualities
                selectedQuality = qualities.firstOrNull { it.id == "mp4_1080" }
                    ?: qualities.firstOrNull { it.id == "mp4_720" }
                    ?: qualities.firstOrNull()
            }
        } else {
            Toast.makeText(context, "Tautan YouTube tidak valid", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = VsCodeBackground,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(VsCodeBlue, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Pengunduh Media",
                                color = Color.White,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Simpan Video & Musik Langsung ke HP",
                                color = VsCodeTextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = VsCodeSidebar
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = VsCodeSidebar,
                tonalElevation = 0.dp
            ) {
                NavigationBarItem(
                    selected = selectedNavIndex == 0,
                    onClick = { selectedNavIndex = 0 },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Unduh"
                        )
                    },
                    label = { Text("Unduh", fontSize = 12.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = Color.White,
                        indicatorColor = VsCodeBlue,
                        unselectedIconColor = VsCodeTextMuted,
                        unselectedTextColor = VsCodeTextMuted
                    )
                )

                NavigationBarItem(
                    selected = selectedNavIndex == 1,
                    onClick = {
                        downloadHistory = historyManager.getRecords()
                        selectedNavIndex = 1
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = "Riwayat"
                        )
                    },
                    label = { Text("Riwayat Unduhan", fontSize = 12.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = Color.White,
                        indicatorColor = VsCodeBlue,
                        unselectedIconColor = VsCodeTextMuted,
                        unselectedTextColor = VsCodeTextMuted
                    )
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (selectedNavIndex) {
                0 -> DownloaderContent(
                    inputUrl = inputUrl,
                    onUrlChange = { inputUrl = it },
                    activeVideoInfo = activeVideoInfo,
                    isFetchingInfo = isFetchingInfo,
                    engineState = engineState,
                    availableQualities = availableQualities,
                    selectedQuality = selectedQuality,
                    onQualitySelect = { selectedQuality = it },
                    onPasteClicked = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        val text = clipboard?.primaryClip?.getItemAt(0)?.text?.toString()
                        if (!text.isNullOrBlank()) {
                            inputUrl = text.trim()
                            processUrl(text.trim())
                        } else {
                            Toast.makeText(context, "Clipboard kosong", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onSearchClicked = {
                        if (inputUrl.isNotBlank()) {
                            processUrl(inputUrl)
                        } else {
                            Toast.makeText(context, "Masukkan tautan YouTube terlebih dahulu", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onStartDownload = { quality ->
                        val currentInfo = activeVideoInfo
                        val videoId = currentInfo?.id ?: DownloadHelper.extractYouTubeId(inputUrl.trim())
                        if (videoId != null && quality != null) {
                            engineState = EngineState.Converting(quality.label)
                            scope.launch {
                                val result = Y2MateNativeEngine.convertVideo(videoId, quality)
                                result.onSuccess { res ->
                                    engineState = EngineState.Downloading(quality.label, 0, 0L, 0L)
                                    val dlResult = DownloadHelper.downloadDirectly(
                                        context = context,
                                        downloadUrl = res.url,
                                        suggestedFileName = res.filename,
                                        mimeType = res.mimeType,
                                        videoTitle = currentInfo?.title ?: res.filename,
                                        onProgress = { percent, downloaded, total ->
                                            engineState = EngineState.Downloading(quality.label, percent, downloaded, total)
                                        }
                                    )
                                    dlResult.onSuccess { savedFileName ->
                                        engineState = EngineState.Success(savedFileName)
                                        downloadHistory = historyManager.getRecords()
                                        Toast.makeText(context, "Selesai diunduh ke folder Download!", Toast.LENGTH_SHORT).show()
                                    }.onFailure { dlErr ->
                                        val msg = dlErr.message ?: "Gagal mengunduh file"
                                        engineState = EngineState.Error(msg)
                                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                    }
                                }.onFailure { err ->
                                    val msg = err.message ?: "Gagal memproses unduhan"
                                    engineState = EngineState.Error(msg)
                                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                }
                            }
                        } else {
                            Toast.makeText(context, "Pilih format terlebih dahulu", Toast.LENGTH_SHORT).show()
                        }
                    }
                )

                1 -> HistoryContent(
                    records = downloadHistory,
                    onOpenDownloadsFolder = {
                        DownloadHelper.openDownloadsFolder(context)
                    },
                    onDeleteRecord = { id ->
                        historyManager.removeRecord(id)
                        downloadHistory = historyManager.getRecords()
                    }
                )
            }
        }
    }
}

@Composable
fun DownloaderContent(
    inputUrl: String,
    onUrlChange: (String) -> Unit,
    activeVideoInfo: VideoInfo?,
    isFetchingInfo: Boolean,
    engineState: EngineState,
    availableQualities: List<AvailableQuality>,
    selectedQuality: AvailableQuality?,
    onQualitySelect: (AvailableQuality) -> Unit,
    onPasteClicked: () -> Unit,
    onSearchClicked: () -> Unit,
    onStartDownload: (AvailableQuality?) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Input Box Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = VsCodePanel),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(VsCodeBorder))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Tautan YouTube",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = inputUrl,
                        onValueChange = onUrlChange,
                        placeholder = {
                            Text(
                                text = "Tempel tautan video di sini...",
                                color = VsCodeTextMuted,
                                fontSize = 13.sp
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_url"),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = VsCodeInputBg,
                            unfocusedContainerColor = VsCodeInputBg,
                            focusedBorderColor = VsCodeBlue,
                            unfocusedBorderColor = VsCodeBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { onSearchClicked() }),
                        trailingIcon = {
                            if (inputUrl.isNotBlank()) {
                                IconButton(onClick = { onUrlChange("") }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Hapus",
                                        tint = VsCodeTextMuted,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onPasteClicked,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = VsCodeInputBg,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).testTag("btn_paste")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentPaste,
                                contentDescription = "Tempel",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Tempel", fontSize = 13.sp)
                        }

                        Button(
                            onClick = onSearchClicked,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = VsCodeBlue,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1.3f).testTag("btn_search")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Cari",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Cari Video", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        // Loading Indicator
        if (isFetchingInfo) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = VsCodePanel)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            color = VsCodeBlue,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Mengambil data video...",
                            color = Color.White,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        // Video Result & Download Options Card
        if (activeVideoInfo != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = VsCodePanel),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(VsCodeBorder))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        // Video Preview Row
                        Row(modifier = Modifier.fillMaxWidth()) {
                            AsyncImage(
                                model = activeVideoInfo.thumbnailUrl,
                                contentDescription = "Thumbnail",
                                modifier = Modifier
                                    .size(width = 110.dp, height = 70.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.Black),
                                contentScale = ContentScale.Crop
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = activeVideoInfo.title,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = activeVideoInfo.author,
                                    color = VsCodeTextMuted,
                                    fontSize = 11.sp,
                                    maxLines = 1
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        var selectedFormatTab by remember { mutableStateOf("video") }

                        // Format / Tab Switcher (Video vs Audio)
                        Text(
                            text = "Pilih Format & Kualitas:",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(VsCodeInputBg)
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val isVideoActive = selectedFormatTab == "video"
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isVideoActive) VsCodeBlue else Color.Transparent)
                                    .clickable {
                                        selectedFormatTab = "video"
                                        val firstVideo = availableQualities.firstOrNull { it.formatType == "video" }
                                        if (firstVideo != null) onQualitySelect(firstVideo)
                                    }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Movie,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Video (MP4)",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = if (isVideoActive) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }

                            val isAudioActive = selectedFormatTab == "audio"
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isAudioActive) VsCodeOrange else Color.Transparent)
                                    .clickable {
                                        selectedFormatTab = "audio"
                                        val firstAudio = availableQualities.firstOrNull { it.formatType == "audio" }
                                        if (firstAudio != null) onQualitySelect(firstAudio)
                                    }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Headphones,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Audio (MP3)",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = if (isAudioActive) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Complete default qualities if not yet loaded
                        val allQualities = if (availableQualities.isNotEmpty()) {
                            availableQualities
                        } else {
                            listOf(
                                AvailableQuality("mp4_1080", "1080p (.mp4)", "Full HD", "video", "1080"),
                                AvailableQuality("mp4_720", "720p (.mp4)", "HD", "video", "720"),
                                AvailableQuality("mp4_360", "360p (.mp4)", "SD 360p", "video", "360"),
                                AvailableQuality("mp4_240", "240p (.mp4)", "Hemat Kuota", "video", "240"),
                                AvailableQuality("mp4_144", "144p (.mp4)", "Ukuran Kecil", "video", "144"),
                                AvailableQuality("mp3_320", "MP3 - 320kbps", "HQ Audio", "audio", "320"),
                                AvailableQuality("mp3_256", "MP3 - 256kbps", "High Quality", "audio", "256"),
                                AvailableQuality("mp3_128", "MP3 - 128kbps", "Standar", "audio", "128")
                            )
                        }

                        val filteredList = allQualities.filter { it.formatType == selectedFormatTab }

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            filteredList.forEach { quality ->
                                val isSelected = selectedQuality?.id == quality.id

                                FormatSelectableRow(
                                    quality = quality,
                                    isSelected = isSelected,
                                    onSelect = { onQualitySelect(quality) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Action Button & Status
                        val isConverting = engineState is EngineState.Converting
                        val isDownloading = engineState is EngineState.Downloading
                        val isBusy = isConverting || isDownloading

                        Button(
                            onClick = {
                                val target = selectedQuality ?: filteredList.firstOrNull() ?: allQualities.first()
                                onStartDownload(target)
                            },
                            enabled = !isBusy,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = VsCodeBlue,
                                contentColor = Color.White,
                                disabledContainerColor = VsCodeBlueDark
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("btn_download_now")
                        ) {
                            if (isConverting) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Menyiapkan Tautan...", fontSize = 13.sp)
                            } else if (isDownloading) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Sedang Mengunduh...", fontSize = 13.sp)
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Download,
                                    contentDescription = "Unduh",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Unduh File Sekarang",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        if (isDownloading) {
                            val dlState = engineState as EngineState.Downloading
                            val progressFloat = if (dlState.percent > 0) dlState.percent / 100f else null
                            val mbDownloaded = String.format("%.1f", dlState.bytesDownloaded / (1024f * 1024f))
                            val mbTotal = if (dlState.totalBytes > 0) String.format("%.1f MB", dlState.totalBytes / (1024f * 1024f)) else "..."

                            Spacer(modifier = Modifier.height(10.dp))
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Menyimpan ke HP...",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "${if (dlState.percent > 0) "${dlState.percent}%" else ""} ($mbDownloaded MB / $mbTotal)",
                                        color = VsCodeTextMuted,
                                        fontSize = 11.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                if (progressFloat != null) {
                                    LinearProgressIndicator(
                                        progress = { progressFloat },
                                        modifier = Modifier.fillMaxWidth().height(6.dp),
                                        color = VsCodeBlue,
                                        trackColor = VsCodeInputBg,
                                    )
                                } else {
                                    LinearProgressIndicator(
                                        modifier = Modifier.fillMaxWidth().height(6.dp),
                                        color = VsCodeBlue,
                                        trackColor = VsCodeInputBg,
                                    )
                                }
                            }
                        }

                        if (engineState is EngineState.Error) {
                            val errMessage = (engineState as EngineState.Error).message
                            Spacer(modifier = Modifier.height(10.dp))
                            Card(
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = VsCodeRed.copy(alpha = 0.12f)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, VsCodeRed.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = VsCodeRed,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = errMessage,
                                        color = VsCodeRed,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        if (engineState is EngineState.Success) {
                            val successFile = (engineState as EngineState.Success).fileName
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = VsCodeGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Selesai disimpan di folder Downloads: $successFile",
                                    color = VsCodeGreen,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }

        // Info Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = VsCodeSidebar),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(VsCodeBorder))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Petunjuk Penggunaan",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "1. Salin tautan video dari aplikasi YouTube.\n" +
                               "2. Tempel tautan di kotak input lalu klik 'Cari Video'.\n" +
                               "3. Pilih kualitas video (MP4) atau audio (MP3) yang diinginkan.\n" +
                               "4. Klik 'Unduh File Sekarang'. File otomatis tersimpan di folder Download perangkat Anda.",
                        color = VsCodeTextMuted,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

@Composable
fun FormatSelectableRow(
    quality: AvailableQuality,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val isAudio = quality.formatType == "audio" || quality.label.contains("MP3", ignoreCase = true)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) (if (isAudio) VsCodeOrange.copy(alpha = 0.18f) else VsCodeBlueDark.copy(alpha = 0.55f)) else VsCodeInputBg)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) (if (isAudio) VsCodeOrange else VsCodeBlue) else VsCodeBorder,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onSelect)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(
                    if (isAudio) VsCodeOrange.copy(alpha = 0.2f) else VsCodeBlue.copy(alpha = 0.2f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isAudio) Icons.Default.Headphones else Icons.Default.Movie,
                contentDescription = null,
                tint = if (isAudio) VsCodeOrange else VsCodeBlueLight,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = quality.label,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                )
                if (quality.qualityValue == "320" || quality.qualityValue == "1080") {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isAudio) VsCodeOrange.copy(alpha = 0.25f) else VsCodeBlue.copy(alpha = 0.35f))
                            .padding(horizontal = 6.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = if (isAudio) "MAX 320K" else "FULL HD",
                            color = if (isAudio) VsCodeOrange else VsCodeBlueLight,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (quality.size.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = quality.size,
                    color = VsCodeTextMuted,
                    fontSize = 11.sp
                )
            }
        }

        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(if (isAudio) VsCodeOrange else VsCodeBlue, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Terpilih",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .border(1.5.dp, VsCodeBorder, CircleShape)
            )
        }
    }
}

@Composable
fun HistoryContent(
    records: List<DownloadRecord>,
    onOpenDownloadsFolder: () -> Unit,
    onDeleteRecord: (Long) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Riwayat Unduhan",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${records.size} file tersimpan",
                    color = VsCodeTextMuted,
                    fontSize = 12.sp
                )
            }

            Button(
                onClick = onOpenDownloadsFolder,
                colors = ButtonDefaults.buttonColors(
                    containerColor = VsCodePanel,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Buka Folder HP", fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (records.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(VsCodePanel, RoundedCornerShape(12.dp))
                    .border(1.dp, VsCodeBorder, RoundedCornerShape(12.dp))
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = null,
                        tint = VsCodeTextMuted,
                        modifier = Modifier.size(44.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Belum Ada Riwayat Unduhan",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Semua video atau audio yang berhasil diunduh akan tercatat di sini dan langsung tersimpan di folder Download perangkat.",
                        color = VsCodeTextMuted,
                        fontSize = 12.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(records, key = { it.id }) { record ->
                    HistoryItemCard(
                        record = record,
                        onDelete = { onDeleteRecord(record.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun HistoryItemCard(
    record: DownloadRecord,
    onDelete: () -> Unit
) {
    val dateText = remember(record.timestamp) {
        val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
        sdf.format(Date(record.timestamp))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = VsCodePanel),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(VsCodeBorder))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        if (record.format == "MP3") VsCodeOrange.copy(alpha = 0.2f) else VsCodeBlue.copy(alpha = 0.2f),
                        RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = record.format,
                    color = if (record.format == "MP3") VsCodeOrange else VsCodeBlueLight,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = record.fileName.ifBlank { record.title },
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "$dateText • Tersimpan di /Download",
                    color = VsCodeTextMuted,
                    fontSize = 11.sp
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Hapus",
                    tint = VsCodeTextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
