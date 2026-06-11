package com.khaly.videolibrary

import android.Manifest
import android.app.RecoverableSecurityException
import android.content.Context
import android.content.res.Configuration
import android.content.ContentValues
import android.content.ClipData
import android.content.Intent
import android.content.IntentSender
import android.graphics.Bitmap
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Size
import android.provider.Settings
import android.view.View
import android.widget.Toast
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.ActivityResultLauncher
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.border
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties

private const val PREFS_NAME = "video_library_prefs"
private const val KEY_DEFAULT_PLAYER_PACKAGE = "default_video_player_package"

class MainActivity : ComponentActivity() {
    private val viewModel: VideoLibraryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        applyDynamicSystemBars()

        setContent {
            OneUi85Theme {
                VideoLibraryApp(viewModel = viewModel)
            }
        }
    }

    private fun applyDynamicSystemBars() {
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT

        window.decorView.post {
            val isDarkMode =
                (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
                    android.content.res.Configuration.UI_MODE_NIGHT_YES

            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = if (isDarkMode) {
                0
            } else {
                android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or
                    android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            }
        }
    }

}

@Composable
fun StatusBarGradientHaze() {
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current

    val isDark =
        (configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
            android.content.res.Configuration.UI_MODE_NIGHT_YES

    val topColor = if (isDark) {
        androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.68f)
    } else {
        androidx.compose.ui.graphics.Color.White.copy(alpha = 0.78f)
    }

    val midColor = if (isDark) {
        androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.34f)
    } else {
        androidx.compose.ui.graphics.Color.White.copy(alpha = 0.40f)
    }

    val bottomColor = if (isDark) {
        androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.00f)
    } else {
        androidx.compose.ui.graphics.Color.White.copy(alpha = 0.00f)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(
                brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                    colors = listOf(
                        topColor,
                        midColor,
                        bottomColor
                    )
                )
            )
    )
}










@Composable
fun ExistingFolderTitleGlassSurface(
    title: String
) {
    Surface(
        modifier = Modifier
            .height(32.dp)
            .widthIn(min = 108.dp, max = 220.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.58f),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.42f)
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
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
        Box(Modifier.fillMaxSize()) {

            Box(Modifier.fillMaxSize()) {
                when {
                    state.isLoading -> Box(
                        Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }

                    state.permissionDenied -> PermissionDeniedView(context)

                    state.selectedTab == 1 -> FoldersScreen(
                        folders = state.folders,
                        onOpenFolder = { viewModel.openFolder(it.name) }
                    )

                    else -> Column(
            modifier = Modifier.fillMaxSize()
        ) {
            SmartCollectionStrip()
            VideosScreen(
                        videos = state.filteredVideos,
                        favorites = state.favorites,
                        viewMode = state.viewMode,
                        onOpen = { video ->
                            if (isVideoUriValid(context, video)) {
                                openVideoDirectly(context, video)
                            } else {
                                Toast.makeText(
                                    context,
                                    "Video no longer exists",
                                    Toast.LENGTH_SHORT
                                ).show()
                                viewModel.scanVideos()
                            }
                        },
                        onFavorite = { viewModel.toggleFavorite(it.id) },
                        onRefresh = viewModel::scanVideos
                    )
        }
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxSize()
                    .navigationBarsPadding()
                    .padding(bottom = 12.dp)
            ) {
                OneUiLargeHeader(
                    state = state,
                    onQueryChanged = viewModel::setQuery,
                    onToggleView = {
                        viewModel.setViewMode(
                            if (state.viewMode == ViewMode.GRID) ViewMode.LIST else ViewMode.GRID
                        )
                    },
                    onSortChanged = viewModel::setSortMode,
                    onRefresh = viewModel::scanVideos,
                    onTabSelect = { tab ->
                viewModel.setTab(tab)
            }
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
            ) {
                StatusBarGradientHaze()
            }

            if (state.selectedFolder != null && state.selectedTab != 0) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(top = 0.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        ExistingFolderTitleGlassSurface(
            title = state.selectedFolder ?: "Folder"
        )
    }
}
        }
    }
}










@Composable
fun GlassTopTabButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val iconName = when (text) {
        "▦" -> "videos"
        "▣" -> "folders"
        else -> "grid"
    }

    val label = when (text) {
        "▦" -> "Videos"
        "▣" -> "Folders"
        else -> "Tab"
    }

    OneUiLabeledBarButton(
        iconName = iconName,
        label = label,
        selected = selected,
        onClick = onClick
    )
}




@Composable
fun GlassIconButton(
    text: String,
    onClick: () -> Unit
) {
    val iconName = when (text) {
        "⌕" -> "search"
        "☷" -> "list"
        "▤" -> "grid"
        else -> "more"
    }

    val label = when (text) {
        "⌕" -> "Search"
        "☷" -> "List"
        "▤" -> "Grid"
        else -> "More"
    }

    OneUiLabeledBarButton(
        iconName = iconName,
        label = label,
        selected = false,
        onClick = onClick
    )
}















@Composable
fun BottomBarGradientHaze() {
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()

    val bottomColor = if (isDark) {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)
    }

    val midColor = if (isDark) {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.58f)
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.46f)
    }

    val transparentColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.00f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(132.dp)
            .background(
                brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                    colors = listOf(
                        transparentColor,
                        midColor,
                        bottomColor
                    )
                )
            )
    )
}


@Composable
fun OneUiLargeHeader(
    state: VideoLibraryUiState,
    onQueryChanged: (String) -> Unit,
    onToggleView: () -> Unit,
    onSortChanged: (SortMode) -> Unit,
    onRefresh: () -> Unit,
    onTabSelect: (Int) -> Unit
) {
    var searchActive by remember { mutableStateOf(state.query.isNotBlank()) }

    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    val searchFocusRequester = remember { FocusRequester() }

    fun hideSearchBarOnly() {
        searchActive = false
        focusManager.clearFocus()
        keyboardController?.hide()
    }

    fun clearSearchAndClose() {
        searchActive = false
        onQueryChanged("")
        focusManager.clearFocus()
        keyboardController?.hide()
    }

    BackHandler(enabled = searchActive || state.query.isNotBlank()) {
        clearSearchAndClose()
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        if (searchActive) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        indication = null,
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                    ) {
                        // مهم: نخفي الشريط فقط ولا نمسح نتائج البحث
                        hideSearchBarOnly()
                    }
            )

            LaunchedEffect(searchActive) {
                searchFocusRequester.requestFocus()
                keyboardController?.show()
            }

            val searchShape = RoundedCornerShape(24.dp)

            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(
                        start = 14.dp,
                        end = 14.dp,
                        top = 42.dp
                    )
                    .height(56.dp),
                shape = searchShape,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
                border = BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.28f)
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(searchShape)
                        .padding(horizontal = 22.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    androidx.compose.foundation.text.BasicTextField(
                        value = state.query,
                        onValueChange = onQueryChanged,
                        singleLine = true,
                        textStyle = androidx.compose.material3.LocalTextStyle.current.copy(
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(
                            MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(searchFocusRequester),
                        decorationBox = { innerTextField ->
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (state.query.isBlank()) {
                                    Text(
                                        text = "Type video name...",
                                        fontSize = 20.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
                                        maxLines = 1
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )
                }
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .offset(y = 24.dp)
        ) {
            BottomBarGradientHaze()
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .offset(y = 24.dp)
                .padding(start = 14.dp, end = 14.dp, bottom = 4.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlassTopTabButton(
                    text = "▦",
                    selected = state.selectedTab == 0,
                    onClick = {
                        onTabSelect(0)
                    }
                )

                GlassTopTabButton(
                    text = "▣",
                    selected = state.selectedTab == 1,
                    onClick = { onTabSelect(1) }
                )

                GlassIconButton(
                    text = if (state.viewMode == ViewMode.GRID) "☷" else "▤",
                    onClick = onToggleView
                )

                GlassIconButton(
                    text = "⌕",
                    onClick = {
                        searchActive = true
                    }
                )

                GlassSortButton(
                    sortMode = state.sortMode,
                    onSortChanged = onSortChanged
                )
            }
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
fun SuperSortMenuItem(
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp),
        shape = RoundedCornerShape(18.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
        } else {
            androidx.compose.ui.graphics.Color.Transparent
        },
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                maxLines = 1
            )

            if (selected) {
                Text(
                    text = "✓",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
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
        OneUiLabeledBarButton(
            iconName = "sort",
            label = "Sort",
            selected = false,
            onClick = { expanded = true }
        )

        if (expanded) {
            Popup(
                alignment = Alignment.BottomEnd,
                onDismissRequest = { expanded = false },
                properties = PopupProperties(
                    focusable = true,
                    dismissOnBackPress = true,
                    dismissOnClickOutside = true
                )
            ) {
                val shape = RoundedCornerShape(30.dp)

                Surface(
                    modifier = Modifier
                        .padding(end = 10.dp, bottom = 82.dp)
                        .widthIn(min = 232.dp, max = 292.dp)
                        .clip(shape)
                        .background(
                            brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.78f),
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)
                                )
                            ),
                            shape = shape
                        ),
                    shape = shape,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp,
                    border = BorderStroke(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.42f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .clip(shape)
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Sort videos",
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        SortMode.values().forEach { mode ->
                            val label = mode.name
                                .lowercase()
                                .replace("_", " ")
                                .replaceFirstChar { it.uppercase() }

                            SuperSortMenuItem(
                                title = label,
                                selected = mode == sortMode,
                                onClick = {
                                    expanded = false
                                    onSortChanged(mode)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}















@Composable
fun OneUiBottomNav(selected: Int, onSelect: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 28.dp, end = 28.dp, bottom = 12.dp),
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















@Composable
fun GlassBottomTab(
    selected: Boolean,
    icon: String,
    label: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .width(128.dp)
            .height(64.dp),
        shape = RoundedCornerShape(28.dp),
        color = if (selected)
            MaterialTheme.colorScheme.primary.copy(alpha = 0.20f)
        else
            MaterialTheme.colorScheme.surface.copy(alpha = 0.14f),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.16f)
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = icon,
                fontSize = 27.sp,
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






@Composable
fun PermissionDeniedView(context: Context) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.18f),
            border = BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
            )
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Permission needed",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = "Allow video access so the app can organize your library.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(18.dp))

                Button(
                    onClick = {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.parse("package:${context.packageName}")
                        }
                        context.startActivity(intent)
                    }
                ) {
                    Text("Open Settings")
                }
            }
        }
    }
}

@Composable
fun OneUiVideoFolderIcon(
    modifier: Modifier = Modifier,
    videoCount: Int = 0
) {
    Box(
        modifier = modifier
            .size(width = 88.dp, height = 68.dp)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 5.dp, top = 2.dp)
                .size(width = 64.dp, height = 48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.34f))
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .size(width = 82.dp, height = 52.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.82f))
                .padding(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(3) { index ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .background(
                                when (index) {
                                    0 -> MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)
                                    1 -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.60f)
                                    else -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.55f)
                                }
                            )
                    ) {
                        Text(
                            text = "▶",
                            modifier = Modifier.align(Alignment.Center),
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }

        if (videoCount > 0) {
            Surface(
                modifier = Modifier.align(Alignment.BottomEnd),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                border = BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.20f)
                )
            ) {
                Text(
                    text = videoCount.toString(),
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
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
        EmptyView("No folders found")
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(
            start = 14.dp,
            end = 14.dp,
            top = 0.dp,
            bottom = 96.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(folders, key = { folder -> folder.name }) { folder ->
            Surface(
                onClick = { onOpenFolder(folder) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.36f),
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
                border = BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OneUiVideoFolderIcon(
                        videoCount = folder.count
                    )

                    Spacer(Modifier.width(14.dp))

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = folder.name,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )

                        Spacer(Modifier.height(4.dp))

                        Text(
                            text = "${folder.count} videos • ${formatSize(folder.totalSizeBytes)}",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}




private object VideoDeleteScrollMemory {
    var active: Boolean = false
    var anchorVideoId: Long? = null
    var fallbackIndex: Int = 0
    var scrollOffset: Int = 0
    var wasGrid: Boolean = true

    fun clear() {
        active = false
        anchorVideoId = null
        fallbackIndex = 0
        scrollOffset = 0
        wasGrid = true
    }
}


fun videoQualityLabel(width: Int, height: Int): String {
    val maxSide = kotlin.math.max(width, height)
    return when {
        maxSide >= 3840 -> "4K"
        maxSide >= 1920 -> "FHD"
        maxSide >= 1280 -> "HD"
        maxSide > 0 -> "SD"
        else -> "VIDEO"
    }
}

@Composable
fun SuperVideoMetaChip(
    text: String
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.70f),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.34f)
        ),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

@Composable
fun SuperQualityBadge(
    label: String
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.36f)
        ),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1
        )
    }
}

@Composable
fun SmartCollectionStrip(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SuperVideoMetaChip("Recent")
        SuperVideoMetaChip("Large")
        SuperVideoMetaChip("4K")
        SuperVideoMetaChip("Camera")
        SuperVideoMetaChip("Downloads")
        SuperVideoMetaChip("WhatsApp")
    }
}


@Composable
fun VideosScreen(
    videos: List<VideoItem>,
    favorites: Set<Long>,
    viewMode: ViewMode,
    onOpen: (VideoItem) -> Unit,
    onFavorite: (VideoItem) -> Unit,
    onRefresh: () -> Unit
) {
    if (videos.isEmpty()) {
        EmptyView("No videos found")
        return
    }

    val context = LocalContext.current

    val gridState = androidx.compose.foundation.lazy.grid.rememberLazyGridState()
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()

    var selectedIds by remember { mutableStateOf(setOf<Long>()) }
    var renameVideo by remember { mutableStateOf<VideoItem?>(null) }
    var pendingRename by remember { mutableStateOf<Pair<VideoItem, String>?>(null) }

    val selectedVideos = videos.filter { it.id in selectedIds }

    fun saveScrollPointBeforeDelete() {
        val firstIndex = if (viewMode == ViewMode.GRID) {
            gridState.firstVisibleItemIndex
        } else {
            listState.firstVisibleItemIndex
        }

        val firstOffset = if (viewMode == ViewMode.GRID) {
            gridState.firstVisibleItemScrollOffset
        } else {
            listState.firstVisibleItemScrollOffset
        }

        // نختار أول فيديو ظاهر غير محذوف. إذا كان أول الظاهر من المحددات، نبحث عن الذي بعده.
        val anchor = videos
            .drop(firstIndex)
            .firstOrNull { it.id !in selectedIds }
            ?: videos.take(firstIndex).lastOrNull { it.id !in selectedIds }

        VideoDeleteScrollMemory.active = true
        VideoDeleteScrollMemory.anchorVideoId = anchor?.id
        VideoDeleteScrollMemory.fallbackIndex = firstIndex.coerceAtLeast(0)
        VideoDeleteScrollMemory.scrollOffset = firstOffset.coerceAtLeast(0)
        VideoDeleteScrollMemory.wasGrid = viewMode == ViewMode.GRID
    }

    LaunchedEffect(
        videos.size,
        videos.firstOrNull()?.id,
        videos.lastOrNull()?.id,
        viewMode
    ) {
        if (VideoDeleteScrollMemory.active) {
            val anchorId = VideoDeleteScrollMemory.anchorVideoId

            val indexByAnchor = if (anchorId != null) {
                videos.indexOfFirst { it.id == anchorId }
            } else {
                -1
            }

            val targetIndex = if (indexByAnchor >= 0) {
                indexByAnchor
            } else {
                VideoDeleteScrollMemory.fallbackIndex.coerceIn(
                    0,
                    (videos.size - 1).coerceAtLeast(0)
                )
            }

            val offset = VideoDeleteScrollMemory.scrollOffset

            if (videos.isNotEmpty()) {
                if (viewMode == ViewMode.GRID) {
                    gridState.scrollToItem(targetIndex, offset)
                } else {
                    listState.scrollToItem(targetIndex, offset)
                }
            }

            VideoDeleteScrollMemory.clear()
        }
    }

    BackHandler(enabled = selectedIds.isNotEmpty()) {
        selectedIds = emptySet()
        renameVideo = null
    }

    val deleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) {
        selectedIds = emptySet()
        onRefresh()
    }

    val renamePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) {
        val pending = pendingRename
        pendingRename = null

        if (pending != null) {
            val ok = renameVideoFileDirect(context, pending.first, pending.second)

            Toast.makeText(
                context,
                if (ok) "Renamed" else "Rename failed",
                Toast.LENGTH_SHORT
            ).show()

            selectedIds = emptySet()
            renameVideo = null
            onRefresh()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (viewMode == ViewMode.GRID) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                state = gridState,
                contentPadding = PaddingValues(
                    start = 12.dp,
                    end = 12.dp,
                    top = 0.dp,
                    bottom = if (selectedIds.isNotEmpty()) 152.dp else 88.dp
                ),
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
                state = listState,
                contentPadding = PaddingValues(
                    start = 14.dp,
                    end = 14.dp,
                    top = 0.dp,
                    bottom = if (selectedIds.isNotEmpty()) 152.dp else 88.dp
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(videos, key = { it.id }) { video ->
                    VideoListCard(
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
        }

        if (selectedIds.isNotEmpty()) {
            SelectionActionBar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(start = 14.dp, end = 14.dp, bottom = 76.dp),
                count = selectedIds.size,
                canRename = selectedIds.size == 1,
                onShare = { shareVideos(context, selectedVideos) },
                onDelete = {
                    saveScrollPointBeforeDelete()

                    requestDeleteVideos(
                        context = context,
                        videos = selectedVideos,
                        launcher = deleteLauncher,
                        onDeletedWithoutSystemDialog = {
                            selectedIds = emptySet()
                            onRefresh()
                        }
                    )
                },
                onRename = {
                    if (selectedVideos.size == 1) {
                        renameVideo = selectedVideos.first()
                    }
                }
            )
        }
    }

    renameVideo?.let { video ->
        RenameVideoDialog(
            video = video,
            onDismiss = { renameVideo = null },
            onRename = { newName ->
                val result = renameVideoFile(
                    context = context,
                    video = video,
                    newName = newName,
                    launcher = renamePermissionLauncher,
                    rememberPending = {
                        pendingRename = video to newName
                    }
                )

                when (result) {
                    RenameResult.SUCCESS -> {
                        Toast.makeText(context, "Renamed", Toast.LENGTH_SHORT).show()
                        renameVideo = null
                        selectedIds = emptySet()
                        onRefresh()
                    }

                    RenameResult.PERMISSION_REQUESTED -> {
                        // ننتظر موافقة النظام
                    }

                    RenameResult.FAILED -> {
                        Toast.makeText(context, "Rename failed", Toast.LENGTH_SHORT).show()
                        renameVideo = null
                    }
                }
            }
        )
    }
}














@Composable
fun OneUiLineIcon(
    name: String,
    modifier: Modifier = Modifier,
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    androidx.compose.foundation.Canvas(
        modifier = modifier.size(34.dp)
    ) {
        val w = size.width
        val h = size.height
        val stroke = w * 0.078f

        fun p(x: Float, y: Float) = androidx.compose.ui.geometry.Offset(w * x, h * y)

        val outline = androidx.compose.ui.graphics.drawscope.Stroke(
            width = stroke,
            cap = androidx.compose.ui.graphics.StrokeCap.Round,
            join = androidx.compose.ui.graphics.StrokeJoin.Round
        )

        when (name) {
            "move" -> {
                val folder = androidx.compose.ui.graphics.Path().apply {
                    moveTo(w * 0.16f, h * 0.34f)
                    quadraticBezierTo(w * 0.16f, h * 0.23f, w * 0.27f, h * 0.23f)
                    lineTo(w * 0.43f, h * 0.23f)
                    quadraticBezierTo(w * 0.50f, h * 0.23f, w * 0.55f, h * 0.31f)
                    lineTo(w * 0.62f, h * 0.39f)
                    lineTo(w * 0.76f, h * 0.39f)
                    quadraticBezierTo(w * 0.86f, h * 0.39f, w * 0.86f, h * 0.49f)
                    lineTo(w * 0.86f, h * 0.76f)
                    quadraticBezierTo(w * 0.86f, h * 0.86f, w * 0.76f, h * 0.86f)
                    lineTo(w * 0.27f, h * 0.86f)
                    quadraticBezierTo(w * 0.16f, h * 0.86f, w * 0.16f, h * 0.76f)
                    close()
                }
                drawPath(folder, color, style = outline)

                drawLine(color, p(0.39f, 0.58f), p(0.67f, 0.58f), strokeWidth = stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                drawLine(color, p(0.57f, 0.47f), p(0.68f, 0.58f), strokeWidth = stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                drawLine(color, p(0.57f, 0.69f), p(0.68f, 0.58f), strokeWidth = stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round)
            }

            "copy" -> {
                val back = androidx.compose.ui.graphics.Path().apply {
                    moveTo(w * 0.36f, h * 0.15f)
                    lineTo(w * 0.72f, h * 0.15f)
                    quadraticBezierTo(w * 0.84f, h * 0.15f, w * 0.84f, h * 0.27f)
                    lineTo(w * 0.84f, h * 0.64f)
                }
                drawPath(back, color.copy(alpha = 0.70f), style = outline)

                val front = androidx.compose.ui.graphics.Path().apply {
                    moveTo(w * 0.18f, h * 0.32f)
                    quadraticBezierTo(w * 0.18f, h * 0.20f, w * 0.30f, h * 0.20f)
                    lineTo(w * 0.58f, h * 0.20f)
                    quadraticBezierTo(w * 0.70f, h * 0.20f, w * 0.70f, h * 0.32f)
                    lineTo(w * 0.70f, h * 0.74f)
                    quadraticBezierTo(w * 0.70f, h * 0.86f, w * 0.58f, h * 0.86f)
                    lineTo(w * 0.30f, h * 0.86f)
                    quadraticBezierTo(w * 0.18f, h * 0.86f, w * 0.18f, h * 0.74f)
                    close()
                }
                drawPath(front, color, style = outline)
            }

            "share" -> {
                val a = p(0.28f, 0.52f)
                val b = p(0.70f, 0.28f)
                val c = p(0.70f, 0.76f)

                drawLine(color, a, b, strokeWidth = stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                drawLine(color, a, c, strokeWidth = stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round)

                drawCircle(color, radius = w * 0.105f, center = a, style = outline)
                drawCircle(color, radius = w * 0.105f, center = b, style = outline)
                drawCircle(color, radius = w * 0.105f, center = c, style = outline)
            }

            "delete" -> {
                drawLine(color, p(0.27f, 0.31f), p(0.73f, 0.31f), strokeWidth = stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                drawLine(color, p(0.42f, 0.19f), p(0.58f, 0.19f), strokeWidth = stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round)

                val bin = androidx.compose.ui.graphics.Path().apply {
                    moveTo(w * 0.34f, h * 0.38f)
                    lineTo(w * 0.38f, h * 0.80f)
                    quadraticBezierTo(w * 0.39f, h * 0.88f, w * 0.47f, h * 0.88f)
                    lineTo(w * 0.63f, h * 0.88f)
                    quadraticBezierTo(w * 0.71f, h * 0.88f, w * 0.72f, h * 0.80f)
                    lineTo(w * 0.76f, h * 0.38f)
                }
                drawPath(bin, color, style = outline)

                drawLine(color.copy(alpha = 0.78f), p(0.47f, 0.50f), p(0.47f, 0.73f), strokeWidth = stroke * 0.72f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                drawLine(color.copy(alpha = 0.78f), p(0.59f, 0.50f), p(0.59f, 0.73f), strokeWidth = stroke * 0.72f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
            }

            "more" -> {
                drawCircle(color, radius = w * 0.055f, center = p(0.50f, 0.25f))
                drawCircle(color, radius = w * 0.055f, center = p(0.50f, 0.50f))
                drawCircle(color, radius = w * 0.055f, center = p(0.50f, 0.75f))
            }

            "rename" -> {
                drawLine(color, p(0.29f, 0.72f), p(0.70f, 0.31f), strokeWidth = stroke * 1.20f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                drawLine(color, p(0.62f, 0.23f), p(0.78f, 0.39f), strokeWidth = stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                drawLine(color.copy(alpha = 0.76f), p(0.23f, 0.84f), p(0.78f, 0.84f), strokeWidth = stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round)
            }

            "search" -> {
                drawCircle(color, radius = w * 0.22f, center = p(0.43f, 0.42f), style = outline)
                drawLine(color, p(0.59f, 0.59f), p(0.78f, 0.78f), strokeWidth = stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round)
            }

            "videos" -> {
                val frame = androidx.compose.ui.graphics.Path().apply {
                    moveTo(w * 0.20f, h * 0.26f)
                    quadraticBezierTo(w * 0.20f, h * 0.18f, w * 0.28f, h * 0.18f)
                    lineTo(w * 0.72f, h * 0.18f)
                    quadraticBezierTo(w * 0.80f, h * 0.18f, w * 0.80f, h * 0.26f)
                    lineTo(w * 0.80f, h * 0.74f)
                    quadraticBezierTo(w * 0.80f, h * 0.82f, w * 0.72f, h * 0.82f)
                    lineTo(w * 0.28f, h * 0.82f)
                    quadraticBezierTo(w * 0.20f, h * 0.82f, w * 0.20f, h * 0.74f)
                    close()
                }
                drawPath(frame, color, style = outline)

                val play = androidx.compose.ui.graphics.Path().apply {
                    moveTo(w * 0.44f, h * 0.38f)
                    lineTo(w * 0.44f, h * 0.64f)
                    lineTo(w * 0.64f, h * 0.51f)
                    close()
                }
                drawPath(play, color)
            }

            "folders" -> {
                val folder = androidx.compose.ui.graphics.Path().apply {
                    moveTo(w * 0.16f, h * 0.34f)
                    quadraticBezierTo(w * 0.16f, h * 0.23f, w * 0.27f, h * 0.23f)
                    lineTo(w * 0.43f, h * 0.23f)
                    quadraticBezierTo(w * 0.50f, h * 0.23f, w * 0.55f, h * 0.31f)
                    lineTo(w * 0.62f, h * 0.39f)
                    lineTo(w * 0.76f, h * 0.39f)
                    quadraticBezierTo(w * 0.86f, h * 0.39f, w * 0.86f, h * 0.49f)
                    lineTo(w * 0.86f, h * 0.76f)
                    quadraticBezierTo(w * 0.86f, h * 0.86f, w * 0.76f, h * 0.86f)
                    lineTo(w * 0.27f, h * 0.86f)
                    quadraticBezierTo(w * 0.16f, h * 0.86f, w * 0.16f, h * 0.76f)
                    close()
                }
                drawPath(folder, color, style = outline)
            }

            "grid" -> {
                repeat(2) { row ->
                    repeat(2) { col ->
                        val left = w * (0.25f + col * 0.29f)
                        val top = h * (0.25f + row * 0.29f)
                        drawRoundRect(
                            color = color,
                            topLeft = androidx.compose.ui.geometry.Offset(left, top),
                            size = androidx.compose.ui.geometry.Size(w * 0.17f, h * 0.17f),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.045f, w * 0.045f),
                            style = outline
                        )
                    }
                }
            }

            "list" -> {
                repeat(3) { i ->
                    val y = 0.30f + i * 0.20f
                    drawLine(color, p(0.24f, y), p(0.76f, y), strokeWidth = stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                }
            }

            "sort" -> {
                drawLine(color, p(0.26f, 0.28f), p(0.74f, 0.28f), strokeWidth = stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                drawLine(color, p(0.26f, 0.50f), p(0.62f, 0.50f), strokeWidth = stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                drawLine(color, p(0.26f, 0.72f), p(0.50f, 0.72f), strokeWidth = stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round)
            }

            "category" -> {
                val tag = androidx.compose.ui.graphics.Path().apply {
                    moveTo(w * 0.22f, h * 0.30f)
                    quadraticBezierTo(w * 0.22f, h * 0.22f, w * 0.30f, h * 0.22f)
                    lineTo(w * 0.58f, h * 0.22f)
                    quadraticBezierTo(w * 0.66f, h * 0.22f, w * 0.72f, h * 0.28f)
                    lineTo(w * 0.82f, h * 0.38f)
                    quadraticBezierTo(w * 0.88f, h * 0.44f, w * 0.82f, h * 0.50f)
                    lineTo(w * 0.50f, h * 0.82f)
                    quadraticBezierTo(w * 0.44f, h * 0.88f, w * 0.38f, h * 0.82f)
                    lineTo(w * 0.22f, h * 0.66f)
                    quadraticBezierTo(w * 0.16f, h * 0.60f, w * 0.22f, h * 0.54f)
                    close()
                }
                drawPath(tag, color, style = outline)
                drawCircle(
                    color = color,
                    radius = w * 0.055f,
                    center = p(0.39f, 0.39f),
                    style = outline
                )
            }

            else -> {
                drawCircle(color, radius = w * 0.07f, center = p(0.50f, 0.50f))
            }
        }
    }
}

@Composable
fun OneUiLabeledBarButton(
    iconName: String,
    label: String,
    selected: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    var flashTick by remember { mutableStateOf(0) }
    var flashing by remember { mutableStateOf(false) }

    val activeColor = MaterialTheme.colorScheme.primary
    val inactiveColor = MaterialTheme.colorScheme.onSurfaceVariant
    val disabledColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.34f)

    val baseIconColor = when {
        !enabled -> disabledColor
        selected -> activeColor
        else -> inactiveColor
    }

    val baseTextColor = when {
        !enabled -> disabledColor
        selected -> activeColor
        else -> inactiveColor
    }

    val flashColor = MaterialTheme.colorScheme.primary

    val animatedIconColor = androidx.compose.animation.animateColorAsState(
        targetValue = if (flashing && enabled) flashColor else baseIconColor,
        animationSpec = androidx.compose.animation.core.tween(
            durationMillis = 130
        ),
        label = "iconFlashColor"
    ).value

    val animatedTextColor = androidx.compose.animation.animateColorAsState(
        targetValue = if (flashing && enabled) flashColor else baseTextColor,
        animationSpec = androidx.compose.animation.core.tween(
            durationMillis = 130
        ),
        label = "textFlashColor"
    ).value

    LaunchedEffect(flashTick) {
        if (flashTick > 0) {
            flashing = true
            kotlinx.coroutines.delay(170)
            flashing = false
        }
    }

    Box(
        modifier = Modifier
            .width(58.dp)
            .height(58.dp)
            .clickable(
                enabled = enabled,
                indication = null,
                interactionSource = remember {
                    androidx.compose.foundation.interaction.MutableInteractionSource()
                }
            ) {
                flashTick += 1
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 5.dp, bottom = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            OneUiLineIcon(
                name = iconName,
                color = animatedIconColor,
                modifier = Modifier.size(25.dp)
            )

            Spacer(Modifier.height(3.dp))

            Text(
                text = label,
                fontSize = 10.sp,
                color = animatedTextColor,
                maxLines = 1
            )
        }
    }
}








@Composable
fun SelectionIconActionButton(
    iconText: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val iconName = when (iconText) {
        "↗" -> "share"
        "✎" -> "rename"
        "⌫" -> "delete"
        else -> "more"
    }

    val label = when (iconText) {
        "↗" -> "Share"
        "✎" -> "Rename"
        "⌫" -> "Delete"
        else -> "More"
    }

    OneUiLabeledBarButton(
        iconName = iconName,
        label = label,
        selected = false,
        enabled = enabled,
        onClick = onClick
    )
}








@Composable
fun SelectionActionBar(
    modifier: Modifier = Modifier,
    count: Int,
    canRename: Boolean,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onRename: () -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(86.dp),
        shape = RoundedCornerShape(34.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.10f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            OneUiLabeledBarButton(
                iconName = "move",
                label = "Move",
                selected = false,
                enabled = false,
                onClick = { }
            )

            OneUiLabeledBarButton(
                iconName = "copy",
                label = "Copy",
                selected = false,
                enabled = false,
                onClick = { }
            )

            OneUiLabeledBarButton(
                iconName = "share",
                label = "Share",
                selected = false,
                enabled = true,
                onClick = onShare
            )

            OneUiLabeledBarButton(
                iconName = "rename",
                label = "Rename",
                selected = false,
                enabled = canRename,
                onClick = onRename
            )

            OneUiLabeledBarButton(
                iconName = "delete",
                label = "Delete",
                selected = false,
                enabled = true,
                onClick = onDelete
            )

            OneUiLabeledBarButton(
                iconName = "more",
                label = "More",
                selected = false,
                enabled = false,
                onClick = { }
            )
        }
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
    launcher: ActivityResultLauncher<IntentSenderRequest>,
    onDeletedWithoutSystemDialog: () -> Unit
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
        onDeletedWithoutSystemDialog()
    }
}









enum class RenameResult {
    SUCCESS,
    PERMISSION_REQUESTED,
    FAILED
}

fun isVideoUriValid(context: Context, video: VideoItem): Boolean {
    return try {
        context.contentResolver.openAssetFileDescriptor(video.uri, "r")?.use {
            true
        } ?: false
    } catch (_: Exception) {
        false
    }
}

fun buildCleanVideoName(
    oldName: String,
    newName: String
): String {
    val trimmed = newName.trim()
    if (trimmed.isBlank()) return oldName

    return if (trimmed.contains(".")) {
        trimmed
    } else {
        val extension = oldName.substringAfterLast(".", "")
        if (extension.isNotBlank()) "$trimmed.$extension" else trimmed
    }
}

fun renameVideoFileDirect(
    context: Context,
    video: VideoItem,
    newName: String
): Boolean {
    val cleanName = buildCleanVideoName(video.name, newName)

    return try {
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

fun renameVideoFile(
    context: Context,
    video: VideoItem,
    newName: String,
    launcher: ActivityResultLauncher<IntentSenderRequest>,
    rememberPending: () -> Unit
): RenameResult {
    val cleanName = buildCleanVideoName(video.name, newName)

    return try {
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, cleanName)
        }

        val rows = context.contentResolver.update(
            video.uri,
            values,
            null,
            null
        )

        if (rows > 0) RenameResult.SUCCESS else RenameResult.FAILED
    } catch (e: RecoverableSecurityException) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            rememberPending()
            launcher.launch(
                IntentSenderRequest.Builder(e.userAction.actionIntent.intentSender).build()
            )
            RenameResult.PERMISSION_REQUESTED
        } else {
            RenameResult.FAILED
        }
    } catch (_: SecurityException) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                rememberPending()

                val request = MediaStore.createWriteRequest(
                    context.contentResolver,
                    listOf(video.uri)
                )

                launcher.launch(
                    IntentSenderRequest.Builder(request.intentSender).build()
                )

                RenameResult.PERMISSION_REQUESTED
            } catch (_: Exception) {
                RenameResult.FAILED
            }
        } else {
            RenameResult.FAILED
        }
    } catch (_: Exception) {
        RenameResult.FAILED
    }
}


fun openVideoDirectly(context: Context, video: VideoItem) {
    if (!isVideoUriValid(context, video)) {
        Toast.makeText(context, "Video no longer exists", Toast.LENGTH_SHORT).show()
        return
    }

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



@Composable
fun EmptyView(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.18f),
            border = BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
            )
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "No content",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(6.dp))

                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
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
                    if (selectionMode) {
                        onToggleSelected()
                    } else {
                        onOpen()
                    }
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
                color = Color.Transparent,
                border = if (selected) {
                    BorderStroke(3.dp, MaterialTheme.colorScheme.primary)
                } else {
                    null
                }
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
                                text = "✓",
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VideoListCard(
    video: VideoItem,
    favorite: Boolean,
    selected: Boolean,
    selectionMode: Boolean,
    onOpen: () -> Unit,
    onToggleSelected: () -> Unit,
    onFavorite: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {
                    if (selectionMode) {
                        onToggleSelected()
                    } else {
                        onOpen()
                    }
                },
                onLongClick = {
                    onToggleSelected()
                }
            )
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(4.dp),
            border = if (selected) {
                BorderStroke(3.dp, MaterialTheme.colorScheme.primary)
            } else {
                null
            },
            color = Color.Transparent
        ) {
            VideoThumbnail(
                video = video,
                modifier = Modifier
                    .size(width = 126.dp, height = 78.dp)
                    .clip(RoundedCornerShape(4.dp))
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(Modifier.weight(1f)) {
            Text(
                text = video.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 17.sp,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = "${formatDuration(video.durationMs)} • ${formatSize(video.sizeBytes)} • ${video.width}×${video.height}",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = video.folderName,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Text(
            text = if (selected) "✓" else "⋮",
            fontSize = 24.sp,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}
