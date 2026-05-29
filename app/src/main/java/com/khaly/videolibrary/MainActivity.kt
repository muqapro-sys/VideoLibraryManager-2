package com.khaly.videolibrary

import android.Manifest
import android.content.Context
import android.content.ContentValues
import android.content.ClipData
import android.content.Intent
import android.graphics.Bitmap
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Size
import android.provider.Settings
import android.widget.Toast
import android.view.ViewGroup
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.IntentSenderRequest
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.produceState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import eightbitlab.com.blurview.BlurView
import eightbitlab.com.blurview.RenderEffectBlur

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
fun RealBlurBackground(
    modifier: Modifier = Modifier,
    blurRadius: Float = 22f
) {
    val composeView = LocalView.current
    val overlayColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.20f).toArgb()

    AndroidView(
        modifier = modifier,
        factory = { context ->
            BlurView(context).apply {
                val rootView = composeView.rootView as ViewGroup

                setupWith(rootView, RenderEffectBlur())
                    .setFrameClearDrawable(rootView.background)
                    .setBlurRadius(blurRadius)

                setOverlayColor(overlayColor)
            }
        },
        update = { blurView ->
            blurView.setOverlayColor(overlayColor)
            blurView.setBlurRadius(blurRadius)
        }
    )
}

@Composable
fun GlassIconButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(52.dp)
            .clip(RoundedCornerShape(22.dp))
    ) {
        RealBlurBackground(
            modifier = Modifier.matchParentSize(),
            blurRadius = 24f
        )

        Surface(
            onClick = onClick,
            modifier = Modifier.matchParentSize(),
            shape = RoundedCornerShape(22.dp),
            color = Color.Transparent,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            border = BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)
            )
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Text(
                    text = text,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.94f)
                )
            }
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
            .padding(start = 18.dp, end = 18.dp, top = 10.dp, bottom = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = when (state.selectedTab) {
                        1 -> "Folders"
                        2 -> "Favorites"
                        else -> "Video"
                    },
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Light,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Text(
                    text = when (state.selectedTab) {
                        1 -> "${state.folders.size} Folders"
                        2 -> "${state.favorites.size} Favorites"
                        else -> "${state.videos.size} Videos"
                    },
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlassIconButton(
                    text = if (state.viewMode == ViewMode.GRID) "☷" else "▦",
                    onClick = onToggleView
                )

                GlassIconButton(
                    text = "⌕",
                    onClick = { }
                )

                GlassSortButton(
                    sortMode = state.sortMode,
                    onSortChanged = onSortChanged
                )
            }
        }

        if (state.selectedTab == 0) {
            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilterChip(
                    selected = false,
                    onClick = { },
                    label = { Text("Large files") },
                    leadingIcon = { Text("◇") }
                )

                FilterChip(
                    selected = false,
                    onClick = { },
                    label = { Text("This week") },
                    leadingIcon = { Text("◷") }
                )
            }
        }

        if (state.query.isNotBlank()) {
            Spacer(Modifier.height(10.dp))

            OutlinedTextField(
                value = state.query,
                onValueChange = onQueryChanged,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(22.dp),
                label = { Text("Search videos") }
            )
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
fun GlassSortButton(
    sortMode: SortMode,
    onSortChanged: (SortMode) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Box(
            modifier = Modifier
                .height(52.dp)
                .clip(RoundedCornerShape(22.dp))
        ) {
            RealBlurBackground(
                modifier = Modifier.matchParentSize(),
                blurRadius = 24f
            )

            Surface(
                onClick = { expanded = true },
                modifier = Modifier.matchParentSize(),
                shape = RoundedCornerShape(22.dp),
                color = Color.Transparent,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
                border = BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)
                )
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(horizontal = 18.dp)
                ) {
                    Text(
                        "Sort",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.94f)
                    )
                }
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            SortMode.entries.forEach { mode ->
                DropdownMenuItem(
                    text = {
                        Text(
                            mode.name
                                .replace("_", " ")
                                .lowercase()
                                .replaceFirstChar { it.uppercase() }
                        )
                    },
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
fun OneUiBottomNav(selected: Int, onSelect: (Int) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 22.dp, end = 22.dp, bottom = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(82.dp)
                .clip(RoundedCornerShape(34.dp))
        ) {
            RealBlurBackground(
                modifier = Modifier.matchParentSize(),
                blurRadius = 28f
            )

            Surface(
                modifier = Modifier.matchParentSize(),
                shape = RoundedCornerShape(34.dp),
                color = Color.Transparent,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
                border = BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.20f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GlassBottomTab(
                        selected = selected == 0,
                        icon = "▦",
                        label = "Videos",
                        onClick = { onSelect(0) }
                    )

                    GlassBottomTab(
                        selected = selected == 1,
                        icon = "▣",
                        label = "Folders",
                        onClick = { onSelect(1) }
                    )
                }
            }
        }
    }
}













@Composable
fun GlassBottomTab(
    selected: Boolean,
    icon: String,
    label: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(132.dp)
            .height(62.dp)
            .clip(RoundedCornerShape(28.dp))
    ) {
        if (selected) {
            RealBlurBackground(
                modifier = Modifier.matchParentSize(),
                blurRadius = 18f
            )
        }

        Surface(
            onClick = onClick,
            modifier = Modifier.matchParentSize(),
            shape = RoundedCornerShape(28.dp),
            color = if (selected)
                MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            else
                Color.Transparent,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = icon,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (selected)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = label,
                    fontSize = 14.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    color = if (selected)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}





@Composable
fun SelectionActionBar(
    count: Int,
    canRename: Boolean,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onRename: () -> Unit,
    onCancel: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(26.dp))
    ) {
        RealBlurBackground(
            modifier = Modifier.matchParentSize(),
            blurRadius = 24f
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(26.dp),
            color = Color.Transparent,
            border = BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.20f)
            )
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "$count selected",
                    modifier = Modifier.weight(1f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )

                TextButton(onClick = onShare) {
                    Text("Share")
                }

                if (canRename) {
                    TextButton(onClick = onRename) {
                        Text("Rename")
                    }
                }

                TextButton(onClick = onDelete) {
                    Text("Delete")
                }

                TextButton(onClick = onCancel) {
                    Text("Cancel")
                }
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

    val context = LocalContext.current
    var selectedIds by remember { mutableStateOf(setOf<Long>()) }
    var renameVideo by remember { mutableStateOf<VideoItem?>(null) }

    val selectedVideos = videos.filter { it.id in selectedIds }

    val deleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) {
        selectedIds = emptySet()
    }

    Column {
        if (selectedIds.isNotEmpty()) {
            SelectionActionBar(
                count = selectedIds.size,
                canRename = selectedIds.size == 1,
                onShare = { shareVideos(context, selectedVideos) },
                onDelete = { requestDeleteVideos(context, selectedVideos, deleteLauncher) },
                onRename = {
                    if (selectedVideos.size == 1) renameVideo = selectedVideos.first()
                },
                onCancel = { selectedIds = emptySet() }
            )
        }

        if (viewMode == ViewMode.GRID) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 0.dp, bottom = 110.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                items(videos, key = { it.id }) { video ->
                    VideoGridCard(
                        video = video,
                        favorite = video.id in favorites,
                        selected = video.id in selectedIds,
                        selectionMode = selectedIds.isNotEmpty(),
                        onOpen = { onOpen(video) },
                        onToggleSelected = {
                            selectedIds = if (video.id in selectedIds) {
                                selectedIds - video.id
                            } else {
                                selectedIds + video.id
                            }
                        },
                        onFavorite = { onFavorite(video) }
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 0.dp, bottom = 110.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(videos, key = { it.id }) { video ->
                    VideoListCard(
                        video = video,
                        favorite = video.id in favorites,
                        onOpen = {
                            if (selectedIds.isNotEmpty()) {
                                selectedIds = if (video.id in selectedIds) selectedIds - video.id else selectedIds + video.id
                            } else {
                                onOpen(video)
                            }
                        },
                        onFavorite = { onFavorite(video) }
                    )
                }
            }
        }
    }

    renameVideo?.let { video ->
        RenameVideoDialog(
            video = video,
            onDismiss = { renameVideo = null },
            onRename = { newName ->
                val ok = renameVideo(context, video, newName)
                Toast.makeText(
                    context,
                    if (ok) "Renamed" else "Rename failed",
                    Toast.LENGTH_SHORT
                ).show()
                renameVideo = null
                selectedIds = emptySet()
            }
        )
    }
}











@Composable
fun VideoThumbnail(
    video: VideoItem,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val bitmapState = produceState<Bitmap?>(initialValue = null, video.uri) {
        value = withContext(Dispatchers.IO) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    context.contentResolver.loadThumbnail(
                        video.uri,
                        Size(512, 512),
                        null
                    )
                } else {
                    null
                }
            } catch (_: Exception) {
                null
            }
        }
    }

    val bitmap = bitmapState.value

    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = video.name,
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    } else {
        Box(
            modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "▶",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VideoGridCard(
    video: VideoItem,
    favorite: Boolean,
    selected: Boolean,
    selectionMode: Boolean,
    onOpen: () -> Unit,
    onToggleSelected: () -> Unit,
    onFavorite: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {
                    if (selectionMode) onToggleSelected() else onOpen()
                },
                onLongClick = {
                    onToggleSelected()
                }
            )
    ) {
        Box {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(3.dp),
                border = if (selected) BorderStroke(3.dp, MaterialTheme.colorScheme.primary) else null,
                color = Color.Transparent
            ) {
                Box {
                    VideoThumbnail(
                        video = video,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(126.dp)
                            .clip(RoundedCornerShape(2.dp))
                    )

                    if (selected) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(6.dp),
                            shape = RoundedCornerShape(100.dp),
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Text(
                                "✓",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(5.dp),
                        color = Color.Black.copy(alpha = 0.62f),
                        shape = RoundedCornerShape(2.dp)
                    ) {
                        Text(
                            text = formatDuration(video.durationMs),
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(7.dp))

        Text(
            text = video.name,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(Modifier.height(3.dp))

        Text(
            text = "${formatDuration(video.durationMs)} • ${formatSize(video.sizeBytes)} • ${video.width}×${video.height}",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}











@Composable
fun VideoListCard(
    video: VideoItem,
    favorite: Boolean,
    onOpen: () -> Unit,
    onFavorite: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpen() }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        VideoThumbnail(
            video = video,
            modifier = Modifier
                .size(width = 126.dp, height = 78.dp)
                .clip(RoundedCornerShape(4.dp))
        )

        Spacer(Modifier.width(12.dp))

        Column(Modifier.weight(1f)) {
            Text(
                video.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 17.sp
            )

            Spacer(Modifier.height(4.dp))

            Text(
                "${formatDuration(video.durationMs)} • ${formatSize(video.sizeBytes)} • ${video.width}×${video.height}",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                video.folderName,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Text(
            text = "⋮",
            fontSize = 24.sp,
            modifier = Modifier.padding(start = 8.dp)
        )
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

@Composable
fun RenameVideoDialog(
    video: VideoItem,
    onDismiss: () -> Unit,
    onRename: (String) -> Unit
) {
    var name by remember { mutableStateOf(video.name.substringBeforeLast(".")) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename video") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                label = { Text("New name") },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = { onRename(name) }) {
                Text("Rename")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}


fun shareVideos(context: Context, videos: List<VideoItem>) {
    if (videos.isEmpty()) return

    val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
        type = "video/*"
        putParcelableArrayListExtra(
            Intent.EXTRA_STREAM,
            ArrayList(videos.map { it.uri })
        )
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    context.startActivity(Intent.createChooser(intent, "Share videos"))
}

fun requestDeleteVideos(
    context: Context,
    videos: List<VideoItem>,
    launcher: androidx.activity.result.ActivityResultLauncher<IntentSenderRequest>
) {
    if (videos.isEmpty()) return

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val request = MediaStore.createDeleteRequest(
            context.contentResolver,
            videos.map { it.uri }
        )
        launcher.launch(IntentSenderRequest.Builder(request.intentSender).build())
    } else {
        videos.forEach { video ->
            try {
                context.contentResolver.delete(video.uri, null, null)
            } catch (_: Exception) {
            }
        }
        Toast.makeText(context, "Deleted", Toast.LENGTH_SHORT).show()
    }
}

fun renameVideo(
    context: Context,
    video: VideoItem,
    newName: String
): Boolean {
    if (newName.isBlank()) return false

    return try {
        val cleanName = if (newName.contains(".")) {
            newName.trim()
        } else {
            val extension = video.name.substringAfterLast(".", "")
            if (extension.isNotBlank()) "${newName.trim()}.$extension" else newName.trim()
        }

        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, cleanName)
        }

        val rows = context.contentResolver.update(
            video.uri,
            values,
            null,
            null
        )

        rows > 0
    } catch (_: Exception) {
        false
    }
}


fun openVideoDirectly(context: Context, video: VideoItem) {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(video.uri, video.mimeType)

        putExtra(Intent.EXTRA_TITLE, video.name)
        putExtra(Intent.EXTRA_SUBJECT, video.name)
        putExtra(Intent.EXTRA_TEXT, video.name)
        putExtra("title", video.name)
        putExtra("filename", video.name)

        clipData = ClipData.newUri(
            context.contentResolver,
            video.name,
            video.uri
        )

        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    try {
        context.startActivity(intent)
    } catch (_: Exception) {
        context.startActivity(Intent.createChooser(intent, video.name))
    }
}



fun openVideoExternally(context: Context, video: VideoItem) {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(video.uri, video.mimeType)

        putExtra(Intent.EXTRA_TITLE, video.name)
        putExtra(Intent.EXTRA_SUBJECT, video.name)
        putExtra(Intent.EXTRA_TEXT, video.name)

        clipData = ClipData.newUri(
            context.contentResolver,
            video.name,
            video.uri
        )

        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    context.startActivity(Intent.createChooser(intent, video.name))
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
