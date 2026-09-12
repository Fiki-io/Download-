package com.fiki.ytdownloader.ui

import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
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
import com.fiki.ytdownloader.data.DownloadHistoryManager
import com.fiki.ytdownloader.data.DownloadRecord
import com.fiki.ytdownloader.engine.AvailableQuality
import com.fiki.ytdownloader.engine.EngineState
import com.fiki.ytdownloader.engine.Y2MateNativeEngine
import com.fiki.ytdownloader.ui.components.FloatingGlassDock
import com.fiki.ytdownloader.ui.components.GlassCard
import com.fiki.ytdownloader.ui.components.IOSSegmentedControl
import com.fiki.ytdownloader.ui.components.LiquidBackground
import com.fiki.ytdownloader.ui.theme.DangerRed
import com.fiki.ytdownloader.ui.theme.GlassInputBg
import com.fiki.ytdownloader.ui.theme.GlassSurface
import com.fiki.ytdownloader.ui.theme.NeonBlue
import com.fiki.ytdownloader.ui.theme.NeonCyan
import com.fiki.ytdownloader.ui.theme.NeonEmerald
import com.fiki.ytdownloader.ui.theme.NeonMagenta
import com.fiki.ytdownloader.ui.theme.NeonPurple
import com.fiki.ytdownloader.ui.theme.SuccessGreenLight
import com.fiki.ytdownloader.ui.theme.TextMuted
import com.fiki.ytdownloader.ui.theme.TextPrimary
import com.fiki.ytdownloader.ui.theme.TextSecondary
import com.fiki.ytdownloader.util.DownloadHelper
import com.fiki.ytdownloader.util.SearchResultItem
import com.fiki.ytdownloader.util.VideoInfo
import com.fiki.ytdownloader.util.YouTubeOEmbedHelper
import com.fiki.ytdownloader.util.YouTubeSearchHelper
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    sharedUrl: String? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current

    var selectedNavIndex by remember { mutableIntStateOf(0) }
    var inputUrl by remember { mutableStateOf("") }
    var activeVideoInfo by remember { mutableStateOf<VideoInfo?>(null) }
    var isFetchingInfo by remember { mutableStateOf(false) }

    // Keyword Search States
    var searchResults by remember { mutableStateOf<List<SearchResultItem>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }

    val historyManager = remember { DownloadHistoryManager(context) }
    var downloadHistory by remember { mutableStateOf(historyManager.getRecords()) }

    var engineState by remember { mutableStateOf<EngineState>(EngineState.Idle) }
    var availableQualities by remember { mutableStateOf<List<AvailableQuality>>(emptyList()) }
    var selectedQuality by remember { mutableStateOf<AvailableQuality?>(null) }

    var showPermissionDialog by remember { mutableStateOf(false) }

    // Check manager permission on launch
    LaunchedEffect(Unit) {
        if (!DownloadHelper.hasStoragePermission(context)) {
            showPermissionDialog = true
        }
    }

    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            icon = {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .background(
                            Brush.radialGradient(listOf(NeonCyan.copy(0.3f), Color.Transparent)),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = AppIcons.Folder,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(28.dp)
                    )
                }
            },
            title = {
                Text(
                    text = "Izin Akses Penyimpanan",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Text(
                    text = "Aplikasi membutuhkan izin untuk menyimpan video dan musik langsung ke penyimpanan perangkat Anda.",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPermissionDialog = false
                        DownloadHelper.requestStoragePermission(context)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonCyan,
                        contentColor = Color(0xFF0A0F1A)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Izinkan Akses", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionDialog = false }) {
                    Text("Nanti", color = TextMuted)
                }
            },
            containerColor = Color(0xFF121826).copy(alpha = 0.92f),
            shape = RoundedCornerShape(24.dp)
        )
    }

    val loadVideoDetails = { videoId: String ->
        keyboardController?.hide()
        isFetchingInfo = true
        availableQualities = emptyList()
        selectedQuality = null
        engineState = EngineState.Idle

        scope.launch {
            val info = YouTubeOEmbedHelper.fetchVideoInfo(videoId)
            activeVideoInfo = info
            isFetchingInfo = false
            val qualities = Y2MateNativeEngine.getAvailableQualities(videoId)
            availableQualities = qualities
            selectedQuality = qualities.firstOrNull { it.id == "mp4_720" }
                ?: qualities.firstOrNull { it.id == "mp4_1080" }
                ?: qualities.firstOrNull()
        }
    }

    val processInput = { queryOrUrl: String ->
        val trimmed = queryOrUrl.trim()
        if (trimmed.isNotBlank()) {
            keyboardController?.hide()

            val isUrl = trimmed.startsWith("http://", ignoreCase = true) ||
                    trimmed.startsWith("https://", ignoreCase = true) ||
                    trimmed.contains("youtube.com", ignoreCase = true) ||
                    trimmed.contains("youtu.be", ignoreCase = true)

            if (isUrl) {
                val videoId = DownloadHelper.extractYouTubeId(trimmed)
                if (videoId != null) {
                    searchResults = emptyList()
                    loadVideoDetails(videoId)
                } else {
                    Toast.makeText(context, "Tautan YouTube tidak valid", Toast.LENGTH_SHORT).show()
                }
            } else {
                activeVideoInfo = null
                availableQualities = emptyList()
                selectedQuality = null
                engineState = EngineState.Idle
                isSearching = true
                searchResults = emptyList()

                scope.launch {
                    val result = YouTubeSearchHelper.searchVideos(trimmed)
                    isSearching = false
                    result.onSuccess { items ->
                        searchResults = items
                        if (items.isEmpty()) {
                            Toast.makeText(context, "Tidak ditemukan video untuk kata kunci tersebut", Toast.LENGTH_LONG).show()
                        }
                    }.onFailure { err ->
                        Toast.makeText(context, "Gagal mencari: ${err.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    // Handle shared URL from Android Share sheet
    LaunchedEffect(sharedUrl) {
        if (!sharedUrl.isNullOrBlank()) {
            selectedNavIndex = 0
            inputUrl = sharedUrl
            processInput(sharedUrl)
        }
    }

    LiquidBackground {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        Brush.linearGradient(listOf(NeonCyan, NeonBlue))
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = AppIcons.Download,
                                    contentDescription = null,
                                    tint = Color(0xFF0A0F1A),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "YT Downloader",
                                    color = TextPrimary,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { DownloadHelper.openAppFolder(context) },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.08f))
                                .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                        ) {
                            Icon(
                                imageVector = AppIcons.Folder,
                                contentDescription = "Buka Folder",
                                tint = NeonCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            },
            bottomBar = {
                FloatingGlassDock(
                    selectedIndex = selectedNavIndex,
                    onTabSelected = { idx ->
                        if (idx == 1) downloadHistory = historyManager.getRecords()
                        selectedNavIndex = idx
                    },
                    items = listOf(
                        AppIcons.Download to "Cari & Unduh",
                        AppIcons.Folder to "Riwayat"
                    )
                )
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
                        isSearching = isSearching,
                        searchResults = searchResults,
                        onSelectSearchResult = { item ->
                            inputUrl = "https://youtu.be/${item.id}"
                            loadVideoDetails(item.id)
                        },
                        onClearResults = { searchResults = emptyList() },
                        engineState = engineState,
                        availableQualities = availableQualities,
                        selectedQuality = selectedQuality,
                        onQualitySelect = { selectedQuality = it },
                        onPasteClicked = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                            val text = clipboard?.primaryClip?.getItemAt(0)?.text?.toString()
                            if (!text.isNullOrBlank()) {
                                val cleanUrl = DownloadHelper.extractUrlFromText(text) ?: text.trim()
                                inputUrl = cleanUrl
                                processInput(cleanUrl)
                            } else {
                                Toast.makeText(context, "Clipboard kosong", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onSearchClicked = {
                            if (inputUrl.isNotBlank()) {
                                processInput(inputUrl)
                            } else {
                                Toast.makeText(context, "Ketik judul lagu/video atau tempel link", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onStartDownload = { quality ->
                            if (!DownloadHelper.hasStoragePermission(context)) {
                                showPermissionDialog = true
                                return@DownloaderContent
                            }
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
                                        dlResult.onSuccess { savedFilePath ->
                                            engineState = EngineState.Success(savedFilePath)
                                            downloadHistory = historyManager.getRecords()
                                            Toast.makeText(context, "Berhasil disimpan", Toast.LENGTH_SHORT).show()
                                        }.onFailure { dlErr ->
                                            val msg = dlErr.message ?: "Gagal mengunduh file"
                                            engineState = EngineState.Error(msg)
                                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                        }
                                    }.onFailure { err ->
                                        val msg = err.message ?: "Gagal memproses video"
                                        engineState = EngineState.Error(msg)
                                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                    }
                                }
                            } else {
                                Toast.makeText(context, "Pilih format terlebih dahulu", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onBackToSearch = if (searchResults.isNotEmpty()) {
                            { activeVideoInfo = null }
                        } else null
                    )

                    1 -> HistoryContent(
                        records = downloadHistory,
                        onOpenFolder = {
                            DownloadHelper.openAppFolder(context, null)
                        },
                        onDeleteRecord = { id ->
                            historyManager.removeRecord(id)
                            downloadHistory = historyManager.getRecords()
                        },
                        onItemClick = { record ->
                            DownloadHelper.openFile(context, record)
                        }
                    )
                }
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
    isSearching: Boolean,
    searchResults: List<SearchResultItem>,
    onSelectSearchResult: (SearchResultItem) -> Unit,
    onClearResults: () -> Unit,
    engineState: EngineState,
    availableQualities: List<AvailableQuality>,
    selectedQuality: AvailableQuality?,
    onQualitySelect: (AvailableQuality) -> Unit,
    onPasteClicked: () -> Unit,
    onSearchClicked: () -> Unit,
    onStartDownload: (AvailableQuality?) -> Unit,
    onBackToSearch: (() -> Unit)? = null
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { Spacer(modifier = Modifier.height(4.dp)) }

        // Liquid Glass Search Box Card
        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp)
            ) {
                OutlinedTextField(
                    value = inputUrl,
                    onValueChange = onUrlChange,
                    placeholder = {
                        Text(
                            text = "Ketik judul lagu/video atau tempel link...",
                            color = TextMuted,
                            fontSize = 13.sp
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_url"),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = GlassInputBg,
                        unfocusedContainerColor = GlassInputBg,
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.10f),
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
                                    tint = TextMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Frosted Glass Paste Button
                    Button(
                        onClick = onPasteClicked,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White.copy(alpha = 0.08f),
                            contentColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .testTag("btn_paste")
                    ) {
                        Icon(
                            imageVector = AppIcons.ContentPaste,
                            contentDescription = "Tempel",
                            modifier = Modifier.size(16.dp),
                            tint = NeonCyan
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Tempel", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }

                    // Liquid Gradient Search Button
                    Box(
                        modifier = Modifier
                            .weight(1.3f)
                            .height(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(NeonCyan, Color(0xFF0072FF))
                                )
                            )
                            .clickable(onClick = onSearchClicked)
                            .testTag("btn_search"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Cari",
                                tint = Color(0xFF0A0F1A),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Cari / Unduh",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0A0F1A)
                            )
                        }
                    }
                }
            }
        }

        // Searching by Keyword Indicator
        if (isSearching) {
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            color = NeonCyan,
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = "Mencari video di YouTube...",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Loading Video Info Indicator
        if (isFetchingInfo) {
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            color = NeonCyan,
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = "Menyiapkan opsi unduhan...",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Back to Search Results Button if coming from search
        if (activeVideoInfo != null && onBackToSearch != null) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                        .border(1.dp, NeonCyan.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                        .clickable(onClick = onBackToSearch),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "← Kembali ke Hasil Pencarian",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = NeonCyan
                    )
                }
            }
        }

        // Search Results List (When activeVideoInfo is not loaded)
        if (searchResults.isNotEmpty() && activeVideoInfo == null && !isSearching) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Hasil Pencarian (${searchResults.size})",
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = onClearResults) {
                        Text("Tutup", color = TextMuted, fontSize = 12.sp)
                    }
                }
            }

            items(searchResults, key = { it.id }) { searchItem ->
                SearchResultItemCard(
                    item = searchItem,
                    onClick = { onSelectSearchResult(searchItem) }
                )
            }
        }

        // Video Result & Download Options Card (Liquid Glass)
        if (activeVideoInfo != null) {
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    // Video Preview Row
                    Row(modifier = Modifier.fillMaxWidth()) {
                        AsyncImage(
                            model = activeVideoInfo.thumbnailUrl,
                            contentDescription = "Thumbnail",
                            modifier = Modifier
                                .size(width = 120.dp, height = 75.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.Black),
                            contentScale = ContentScale.Crop
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = activeVideoInfo.title,
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color.White.copy(alpha = 0.08f))
                                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = activeVideoInfo.author,
                                    color = TextSecondary,
                                    fontSize = 11.sp,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    var selectedFormatTab by remember { mutableStateOf("video") }
                    val isVideoTab = selectedFormatTab == "video"

                    // iOS Segmented Control (Video vs Audio)
                    IOSSegmentedControl(
                        selectedTab = selectedFormatTab,
                        onTabSelected = { tab ->
                            selectedFormatTab = tab
                            val target = availableQualities.firstOrNull { it.formatType == tab }
                            if (target != null) onQualitySelect(target)
                        },
                        options = listOf(
                            Triple("video", "Video", AppIcons.Movie),
                            Triple("audio", "Audio (MP3)", AppIcons.Headphones)
                        )
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    val allQualities = if (availableQualities.isNotEmpty()) {
                        availableQualities
                    } else {
                        listOf(
                            AvailableQuality("mp4_1080", "1080p Full HD", "MP4", "video", "1080"),
                            AvailableQuality("mp4_720", "720p HD", "MP4", "video", "720"),
                            AvailableQuality("mp4_360", "360p", "MP4", "video", "360"),
                            AvailableQuality("mp4_240", "240p", "MP4", "video", "240"),
                            AvailableQuality("mp4_144", "144p", "MP4", "video", "144"),
                            AvailableQuality("mp3_320", "320 kbps", "MP3 • Kualitas Terbaik", "audio", "320"),
                            AvailableQuality("mp3_256", "256 kbps", "MP3 • Kualitas Tinggi", "audio", "256"),
                            AvailableQuality("mp3_128", "128 kbps", "MP3 • Standar", "audio", "128")
                        )
                    }

                    val filteredList = allQualities.filter { it.formatType == selectedFormatTab }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        filteredList.forEach { quality ->
                            val isSelected = selectedQuality?.id == quality.id

                            ModernFormatSelectableRow(
                                quality = quality,
                                isSelected = isSelected,
                                onSelect = { onQualitySelect(quality) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Liquid Gradient Action Button
                    val isConverting = engineState is EngineState.Converting
                    val isDownloading = engineState is EngineState.Downloading
                    val isBusy = isConverting || isDownloading

                    val buttonGradient = if (isBusy) {
                        Brush.linearGradient(listOf(Color.Gray.copy(alpha = 0.35f), Color.Gray.copy(alpha = 0.35f)))
                    } else if (isVideoTab) {
                        Brush.horizontalGradient(listOf(NeonCyan, Color(0xFF0072FF)))
                    } else {
                        Brush.horizontalGradient(listOf(NeonPurple, NeonMagenta))
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .shadow(
                                elevation = 12.dp,
                                shape = RoundedCornerShape(16.dp),
                                spotColor = if (isVideoTab) NeonCyan.copy(0.35f) else NeonPurple.copy(0.35f)
                            )
                            .clip(RoundedCornerShape(16.dp))
                            .background(buttonGradient)
                            .clickable(enabled = !isBusy) {
                                val target = selectedQuality ?: filteredList.firstOrNull() ?: allQualities.first()
                                onStartDownload(target)
                            }
                            .testTag("btn_download_now"),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isConverting) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Menyiapkan unduhan...",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }
                        } else if (isDownloading) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Sedang mengunduh...",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = AppIcons.Download,
                                    contentDescription = "Unduh",
                                    modifier = Modifier.size(20.dp),
                                    tint = if (isVideoTab) Color(0xFF0A0F1A) else Color.White
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isVideoTab) "Unduh Video" else "Unduh Audio",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isVideoTab) Color(0xFF0A0F1A) else Color.White
                                )
                            }
                        }
                    }

                    if (isDownloading) {
                        val dlState = engineState as EngineState.Downloading
                        val progressFloat = if (dlState.percent > 0) dlState.percent / 100f else null
                        val mbDownloaded = String.format("%.1f", dlState.bytesDownloaded / (1024f * 1024f))
                        val mbTotal = if (dlState.totalBytes > 0) String.format("%.1f MB", dlState.totalBytes / (1024f * 1024f)) else "..."

                        Spacer(modifier = Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(GlassInputBg)
                                .border(1.dp, Color.White.copy(0.12f), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Mengunduh file...",
                                        color = TextPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "${if (dlState.percent > 0) "${dlState.percent}%" else ""} ($mbDownloaded MB / $mbTotal)",
                                        color = NeonCyan,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                if (progressFloat != null) {
                                    LinearProgressIndicator(
                                        progress = { progressFloat },
                                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                        color = NeonCyan,
                                        trackColor = Color.White.copy(alpha = 0.1f),
                                    )
                                } else {
                                    LinearProgressIndicator(
                                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                        color = NeonCyan,
                                        trackColor = Color.White.copy(alpha = 0.1f),
                                    )
                                }
                            }
                        }
                    }

                    if (engineState is EngineState.Error) {
                        val errMessage = (engineState as EngineState.Error).message
                        Spacer(modifier = Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(DangerRed.copy(alpha = 0.15f))
                                .border(1.dp, DangerRed.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = DangerRed,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = errMessage,
                                    color = DangerRed,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    if (engineState is EngineState.Success) {
                        val successPath = (engineState as EngineState.Success).fileName
                        Spacer(modifier = Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(NeonEmerald.copy(alpha = 0.15f))
                                .border(1.dp, NeonEmerald.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = NeonEmerald,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Selesai diunduh",
                                        color = NeonEmerald,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = successPath,
                                        color = TextSecondary,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(20.dp)) }
    }
}

@Composable
fun SearchResultItemCard(
    item: SearchResultItem,
    onClick: () -> Unit
) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        contentPadding = 10.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(width = 120.dp, height = 72.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black)
            ) {
                AsyncImage(
                    model = item.thumbnailUrl,
                    contentDescription = item.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                if (item.duration.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(4.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.Black.copy(alpha = 0.8f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = item.duration,
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.author,
                    color = TextMuted,
                    fontSize = 11.sp,
                    maxLines = 1
                )
            }

            IconButton(onClick = onClick) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(NeonCyan.copy(0.18f))
                        .border(1.dp, NeonCyan.copy(0.4f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = AppIcons.Download,
                        contentDescription = "Pilih",
                        tint = NeonCyan,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ModernFormatSelectableRow(
    quality: AvailableQuality,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val isAudio = quality.formatType == "audio" || quality.label.contains("MP3", ignoreCase = true)

    val activeBorderBrush = if (isAudio) {
        Brush.horizontalGradient(listOf(NeonPurple, NeonMagenta))
    } else {
        Brush.horizontalGradient(listOf(NeonCyan, NeonBlue))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isSelected) {
                    if (isAudio) NeonPurple.copy(alpha = 0.20f) else NeonCyan.copy(alpha = 0.16f)
                } else Color.White.copy(alpha = 0.05f)
            )
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                brush = if (isSelected) activeBorderBrush else Brush.linearGradient(
                    listOf(Color.White.copy(alpha = 0.12f), Color.White.copy(alpha = 0.03f))
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onSelect)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(
                    if (isAudio) NeonPurple.copy(alpha = 0.25f) else NeonCyan.copy(alpha = 0.20f),
                    RoundedCornerShape(10.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isAudio) AppIcons.Headphones else AppIcons.Movie,
                contentDescription = null,
                tint = if (isAudio) NeonPurple else NeonCyan,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = quality.label,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                )
                if (quality.qualityValue == "320" || quality.qualityValue == "1080") {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isAudio) NeonPurple.copy(alpha = 0.35f) else NeonCyan.copy(alpha = 0.30f))
                            .padding(horizontal = 6.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = if (isAudio) "HQ" else "FHD",
                            color = if (isAudio) Color.White else Color(0xFF0A0F1A),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = quality.size,
                color = TextMuted,
                fontSize = 11.sp
            )
        }

        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(if (isAudio) NeonPurple else NeonCyan, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Terpilih",
                    tint = if (isAudio) Color.White else Color(0xFF0A0F1A),
                    modifier = Modifier.size(16.dp)
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .border(1.5.dp, Color.White.copy(alpha = 0.2f), CircleShape)
            )
        }
    }
}

@Composable
fun HistoryContent(
    records: List<DownloadRecord>,
    onOpenFolder: () -> Unit,
    onDeleteRecord: (Long) -> Unit,
    onItemClick: (DownloadRecord) -> Unit
) {
    var selectedFilter by remember { mutableStateOf("all") } // "all", "video", "audio"

    val filteredRecords = remember(records, selectedFilter) {
        when (selectedFilter) {
            "video" -> records.filter { !it.format.equals("mp3", ignoreCase = true) }
            "audio" -> records.filter { it.format.equals("mp3", ignoreCase = true) }
            else -> records
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Riwayat Unduhan",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                if (records.isNotEmpty()) {
                    Text(
                        text = "${records.size} file tersimpan",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
            }

            // Frosted Glass Open Folder Button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(12.dp))
                    .clickable(onClick = onOpenFolder)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = AppIcons.Folder,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        tint = NeonCyan
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Buka Folder",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // iOS Capsule Filter Pills
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF0A0F1A).copy(alpha = 0.70f))
                .border(
                    1.dp,
                    Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.15f), Color.White.copy(alpha = 0.03f))
                    ),
                    RoundedCornerShape(20.dp)
                )
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf("all" to "Semua", "video" to "Video", "audio" to "Audio").forEach { (key, label) ->
                val isSelected = selectedFilter == key
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSelected) Color.White.copy(alpha = 0.14f) else Color.Transparent)
                        .clickable { selectedFilter = key }
                        .padding(vertical = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) NeonCyan else TextMuted,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (filteredRecords.isEmpty()) {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(0.06f))
                            .border(1.dp, Color.White.copy(0.12f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = AppIcons.Folder,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Belum Ada Unduhan",
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "File video dan audio yang telah diunduh akan muncul di sini.",
                        color = TextMuted,
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
                items(filteredRecords, key = { it.id }) { record ->
                    ModernHistoryItemCard(
                        record = record,
                        onClick = { onItemClick(record) },
                        onDelete = { onDeleteRecord(record.id) }
                    )
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
fun ModernHistoryItemCard(
    record: DownloadRecord,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val dateText = remember(record.timestamp) {
        val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
        sdf.format(Date(record.timestamp))
    }

    val isMp3 = record.format.equals("mp3", ignoreCase = true)

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        contentPadding = 12.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isMp3) NeonPurple.copy(alpha = 0.25f) else NeonCyan.copy(alpha = 0.20f)
                    )
                    .border(
                        1.dp,
                        if (isMp3) NeonPurple.copy(alpha = 0.4f) else NeonCyan.copy(alpha = 0.4f),
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = record.format.uppercase(),
                    color = if (isMp3) NeonPurple else NeonCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = record.fileName.ifBlank { record.title },
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = dateText,
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Hapus",
                    tint = TextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
