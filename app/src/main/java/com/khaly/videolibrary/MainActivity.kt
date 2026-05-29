package com.khaly.videolibrary

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SearchBar
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.rememberAsyncImagePainter
import com.khaly.videolibrary.domain.SortMode
import com.khaly.videolibrary.domain.VideoFolder
import com.khaly.videolibrary.domain.VideoItem
import com.khaly.videolibrary.domain.ViewMode
import com.khaly.videolibrary.ui.theme.OneUi85Theme
import java.text.DecimalFormat
import java.util.concurrent.TimeUnit

private const val PREFS_NAME = "video_library_prefs"
private const val KEY_DEFAULT_PLAYER_PACKAGE = "default_video_player_package"

class MainActivity : ComponentActivity() {
    private val viewModel: VideoLibraryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            OneUi85Theme {
                VideoLibraryApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun VideoLibraryApp(viewModel: VideoLibraryViewModel) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()

    val permission = if (Build.VERSION.SDK_INT >= 33) {
        Manifest.permission.READ_MEDIA_VIDEO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.scanVideos() else viewModel.showPermissionDenied()
    }

    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(
            context,
            permission
        ) == PermissionChecker.PERMISSION_GRANTED

        if (granted) viewModel.scanVideos() else launcher.launch(permission)
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(Modifier.fillMaxSize()) {
            OneUiLargeHeader(
                state = state,
                onQueryChanged = viewModel::setQuery,
                onToggleView = {
                    viewModel.setViewMode(
                        if (state.viewMode == ViewMode.GRID) ViewMode.LIST else ViewMode.GRID
                    )
                },
                onSortChanged = viewModel::setSortMode,
                onRefresh = viewModel::scanVideos
            )

            if (state.selectedFolder != null) {
                AssistChip(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp),
                    onClick = viewModel::clearFolder,
                    label = { Text("Folder: ${state.selectedFolder}  ×") }
                )
            }

            Box(Modifier.weight(1f)) {
                when {
                    state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }

                    state.permissionDenied -> PermissionDeniedView(context)

                    state.selectedTab == 0 -> VideosScreen(
                        videos = state.filteredVideos,
                        favorites = state.favorites,
                        viewMode = state.viewMode,
                        onOpen = { openVideoDirectly(context, it) },
                        onFavorite = { viewModel.toggleFavorite(it.id) }
                    )

                    state.selectedTab == 1 -> FoldersScreen(
                        folders = state.folders,
                        onOpenFolder = { viewModel.openFolder(it.name) }
                    )

                    state.selectedTab == 2 -> VideosScreen(
                        videos = state.videos.filter { it.id in state.favorites },
                        favorites = state.favorites,
                        viewMode = state.viewMode,
                        onOpen = { openVideoDirectly(context, it) },
                        onFavorite = { viewModel.toggleFavorite(it.id) }
                    )
                }
            }

            OneUiBottomNav(
                selected = state.selectedTab,
                onSelect = viewModel::setTab
            )
        }
    }
}

@Composable
fun OneUiLargeHeader(
    state: VideoLibraryUiState,
    onQueryChanged: (String) -> Unit,
    onToggleView: () -> Unit,
    onSortChanged: (SortMode) -> Unit,
    onRefresh: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 24.dp, end = 24.dp, top = 18.dp, bottom = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = when (state.selectedTab) {
                        1 -> "Folders"
                        2 -> "Favorites"
                        else -> "Videos"
                    },
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "${state.videos.size} videos • ${state.folders.size} folders",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            OneUiPillButton(text = if (state.viewMode == ViewMode.GRID) "List" else "Grid", onClick = onToggleView)
            Spacer(Modifier.width(8.dp))
            SortPill(sortMode = state.sortMode, onSortChanged = onSortChanged)
        }

        Spacer(Modifier.height(18.dp))

        OutlinedTextField(
            value = state.query,
            onValueChange = onQueryChanged,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(28.dp),
            label = { Text("Search videos") },
            placeholder = { Text("File name, folder, format") },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedIndicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
                unfocusedIndicatorColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
            )
        )

        Spacer(Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            QuickStatChip("Recent", state.videos.take(20).size.toString())
            QuickStatChip("Favorites", state.favorites.size.toString())
            QuickStatChip("Storage", formatSize(state.videos.sumOf { it.sizeBytes }))
        }
    }
}

@Composable
fun OneUiPillButton(text: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(100.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shadowElevation = 1.dp
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun SortPill(sortMode: SortMode, onSortChanged: (SortMode) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OneUiPillButton(text = "Sort", onClick = { expanded = true })
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            SortMode.entries.forEach { mode ->
                DropdownMenuItem(
                    text = { Text(mode.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }) },
                    onClick = {
                        expanded = false
                        onSortChanged(mode)
                    }
                )
            }
        }
    }
}

@Composable
fun QuickStatChip(label: String, value: String) {
    Surface(
        shape = RoundedCornerShape(100.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(6.dp))
            Text(value, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun OneUiBottomNav(selected: Int, onSelect: (Int) -> Unit) {
    Surface(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .navigationBarsPadding()
            .fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
        shadowElevation = 4.dp
    ) {
        NavigationBar(
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp
        ) {
            val items = listOf(
                Triple("▦", "Videos", 0),
                Triple("▣", "Folders", 1),
                Triple("★", "Favorites", 2)
            )

            items.forEach { item ->
                NavigationBarItem(
                    selected = selected == item.third,
                    onClick = { onSelect(item.third) },
                    icon = { Text(item.first, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold) },
                    label = { Text(item.second) }
                )
            }
        }
    }
}

@Composable
fun VideosScreen(
    videos: List<VideoItem>,
    favorites: Set<Long>,
    viewMode: ViewMode,
    onOpen: (VideoItem) -> Unit,
    onFavorite: (VideoItem) -> Unit
) {
    if (videos.isEmpty()) {
        EmptyView("No videos found")
        return
    }

    if (viewMode == ViewMode.GRID) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(156.dp),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(videos, key = { it.id }) { video ->
                VideoGridCard(
                    video = video,
                    favorite = video.id in favorites,
                    onOpen = { onOpen(video) },
                    onFavorite = { onFavorite(video) }
                )
            }
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(videos, key = { it.id }) { video ->
                VideoListCard(
                    video = video,
                    favorite = video.id in favorites,
                    onOpen = { onOpen(video) },
                    onFavorite = { onFavorite(video) }
                )
            }
        }
    }
}

@Composable
fun VideoGridCard(
    video: VideoItem,
    favorite: Boolean,
    onOpen: () -> Unit,
    onFavorite: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpen() },
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.5.dp)
    ) {
        Column {
            Box {
                Image(
                    painter = rememberAsyncImagePainter(video.uri),
                    contentDescription = video.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(128.dp)
                        .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)),
                    contentScale = ContentScale.Crop
                )
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(9.dp),
                    color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.68f),
                    shape = RoundedCornerShape(100.dp)
                ) {
                    Text(
                        text = formatDuration(video.durationMs),
                        color = MaterialTheme.colorScheme.inverseOnSurface,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(9.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.86f),
                    shape = RoundedCornerShape(100.dp),
                    onClick = onFavorite
                ) {
                    Text(
                        text = if (favorite) "★" else "☆",
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                        color = if (favorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Column(Modifier.padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 12.dp)) {
                Text(video.name, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(3.dp))
                Text(
                    "${formatSize(video.sizeBytes)} • ${video.folderName}",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
fun VideoListCard(
    video: VideoItem,
    favorite: Boolean,
    onOpen: () -> Unit,
    onFavorite: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpen() },
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = rememberAsyncImagePainter(video.uri),
                contentDescription = video.name,
                modifier = Modifier
                    .size(width = 112.dp, height = 74.dp)
                    .clip(RoundedCornerShape(20.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text(video.name, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(3.dp))
                Text("${formatDuration(video.durationMs)} • ${formatSize(video.sizeBytes)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(video.folderName, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Surface(
                onClick = onFavorite,
                shape = RoundedCornerShape(100.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    text = if (favorite) "★" else "☆",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    color = if (favorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun FoldersScreen(
    folders: List<VideoFolder>,
    onOpenFolder: (VideoFolder) -> Unit
) {
    if (folders.isEmpty()) {
        EmptyView("No video folders found")
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(folders, key = { it.name }) { folder ->
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenFolder(folder) },
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            "▣",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 13.dp),
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(folder.name, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${folder.count} videos • ${formatSize(folder.totalSizeBytes)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text("›", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun EmptyView(message: String) {
    Box(
        Modifier
            .fillMaxSize()
            .padding(28.dp),
        contentAlignment = Alignment.Center
    ) {
        ElevatedCard(
            shape = RoundedCornerShape(32.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("No content", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun PermissionDeniedView(context: Context) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ElevatedCard(shape = RoundedCornerShape(32.dp)) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Permission needed", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Allow video access so the app can organize your library.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(18.dp))
                Button(onClick = {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.parse("package:${context.packageName}")
                    }
                    context.startActivity(intent)
                }) {
                    Text("Open Settings")
                }
            }
        }
    }
}

@Composable
fun VideoPreviewDialog(
    context: Context,
    video: VideoItem,
    onDismiss: () -> Unit
) {
    var showPlayerPicker by remember { mutableStateOf(false) }
    val defaultPackage = remember { getDefaultVideoPlayerPackage(context) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = video.name,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.ExtraBold
            )
        },
        text = {
            Column {
                Image(
                    painter = rememberAsyncImagePainter(video.uri),
                    contentDescription = video.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(RoundedCornerShape(26.dp)),
                    contentScale = ContentScale.Crop
                )

                Spacer(Modifier.height(12.dp))

                Text(
                    text = "${formatDuration(video.durationMs)} • ${formatSize(video.sizeBytes)} • ${video.width}×${video.height}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = video.folderName,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(14.dp))

                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { showPlayerPicker = true }
                ) {
                    Text(
                        if (defaultPackage.isNullOrBlank())
                            "Choose default player for this library"
                        else
                            "Change default player"
                    )
                }

                TextButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        openVideoExternally(context, video)
                        onDismiss()
                    }
                ) {
                    Text("Open with system chooser")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    openVideoWithPreferredPlayer(context, video)
                    onDismiss()
                }
            ) {
                Text("Open")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )

    if (showPlayerPicker) {
        PlayerPickerDialog(
            context = context,
            video = video,
            onDismiss = { showPlayerPicker = false },
            onPlayerSelected = { packageName ->
                saveDefaultVideoPlayerPackage(context, packageName)
                showPlayerPicker = false
            }
        )
    }
}

@Composable
fun PlayerPickerDialog(
    context: Context,
    video: VideoItem,
    onDismiss: () -> Unit,
    onPlayerSelected: (String) -> Unit
) {
    val players = remember(video.uri) { listVideoPlayers(context, video) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Default video player") },
        text = {
            if (players.isEmpty()) {
                Text("No external video players were found.")
            } else {
                LazyColumn(
                    modifier = Modifier.height(320.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(players) { player ->
                        val packageName = player.activityInfo.packageName
                        val appName = player.loadLabel(context.packageManager).toString()

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onPlayerSelected(packageName) },
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                        ) {
                            Text(
                                text = appName,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

fun listVideoPlayers(context: Context, video: VideoItem): List<ResolveInfo> {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(video.uri, video.mimeType)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    return context.packageManager.queryIntentActivities(intent, 0)
}

fun getDefaultVideoPlayerPackage(context: Context): String? {
    return context
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getString(KEY_DEFAULT_PLAYER_PACKAGE, null)
}

fun saveDefaultVideoPlayerPackage(context: Context, packageName: String) {
    context
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putString(KEY_DEFAULT_PLAYER_PACKAGE, packageName)
        .apply()
}

fun openVideoWithPreferredPlayer(context: Context, video: VideoItem) {
    val packageName = getDefaultVideoPlayerPackage(context)

    if (packageName.isNullOrBlank()) {
        openVideoExternally(context, video)
        return
    }

    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(video.uri, video.mimeType)
        setPackage(packageName)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    try {
        context.startActivity(intent)
    } catch (_: Exception) {
        openVideoExternally(context, video)
    }
}

fun openVideoDirectly(context: Context, video: VideoItem) {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(video.uri, video.mimeType)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    try {
        context.startActivity(intent)
    } catch (_: Exception) {
        context.startActivity(Intent.createChooser(intent, "Open video with"))
    }
}

fun openVideoExternally(context: Context, video: VideoItem) {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(video.uri, video.mimeType)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Open video with"))
}

fun formatDuration(ms: Long): String {
    val hours = TimeUnit.MILLISECONDS.toHours(ms)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(ms) % 60
    val seconds = TimeUnit.MILLISECONDS.toSeconds(ms) % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds)
    else "%02d:%02d".format(minutes, seconds)
}

fun formatSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    var size = bytes.toDouble()
    var index = 0
    while (size >= 1024 && index < units.lastIndex) {
        size /= 1024
        index++
    }
    return DecimalFormat("#,##0.#").format(size) + " " + units[index]
}
