package com.khaly.videolibrary

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.khaly.videolibrary.data.VideoRepository
import com.khaly.videolibrary.domain.SortMode
import com.khaly.videolibrary.domain.VideoFolder
import com.khaly.videolibrary.domain.VideoItem
import com.khaly.videolibrary.domain.ViewMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class VideoLibraryUiState(
    val videos: List<VideoItem> = emptyList(),
    val filteredVideos: List<VideoItem> = emptyList(),
    val folders: List<VideoFolder> = emptyList(),
    val favorites: Set<Long> = emptySet(),
    val query: String = "",
    val sortMode: SortMode = SortMode.DATE_NEWEST,
    val viewMode: ViewMode = ViewMode.GRID,
    val selectedTab: Int = 0,
    val isLoading: Boolean = false,
    val permissionDenied: Boolean = false,
    val selectedFolder: String? = null
)

class VideoLibraryViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository = VideoRepository(application)
    private val _state = MutableStateFlow(VideoLibraryUiState())
    val state: StateFlow<VideoLibraryUiState> = _state

    fun scanVideos() {
        viewModelScope.launch(Dispatchers.IO) {
            _state.value = _state.value.copy(isLoading = true, permissionDenied = false)
            val videos = repository.loadVideos()
            val sorted = repository.sortVideos(videos, _state.value.sortMode)
            val folders = repository.buildFolders(sorted)
            _state.value = _state.value.copy(
                videos = sorted,
                filteredVideos = applyFilters(sorted, _state.value.query, _state.value.selectedFolder),
                folders = folders,
                isLoading = false
            )
        }
    }

    fun showPermissionDenied() {
        _state.value = _state.value.copy(permissionDenied = true, isLoading = false)
    }

    fun setQuery(query: String) {
        val s = _state.value
        _state.value = s.copy(
            query = query,
            filteredVideos = applyFilters(s.videos, query, s.selectedFolder)
        )
    }

    fun setSortMode(sortMode: SortMode) {
        val s = _state.value
        val sorted = repository.sortVideos(s.videos, sortMode)
        _state.value = s.copy(
            sortMode = sortMode,
            videos = sorted,
            filteredVideos = applyFilters(sorted, s.query, s.selectedFolder),
            folders = repository.buildFolders(sorted)
        )
    }

    fun setViewMode(viewMode: ViewMode) {
        _state.value = _state.value.copy(viewMode = viewMode)
    }

    fun setTab(tab: Int) {
    _state.value = if (tab == 0) {
        _state.value.copy(
            selectedTab = 0,
            selectedFolder = null,
            query = ""
        )
    } else {
        _state.value.copy(
            selectedTab = tab
        )
    }
}





    fun openFolder(folderName: String) {
        val s = _state.value
        _state.value = s.copy(
            selectedTab = 0,
            selectedFolder = folderName,
            filteredVideos = applyFilters(s.videos, s.query, folderName)
        )
    }

    fun clearFolder() {
        val s = _state.value
        _state.value = s.copy(
            selectedFolder = null,
            filteredVideos = applyFilters(s.videos, s.query, null)
        )
    }

    fun toggleFavorite(videoId: Long) {
        val current = _state.value.favorites
        _state.value = _state.value.copy(
            favorites = if (videoId in current) current - videoId else current + videoId
        )
    }

    private fun applyFilters(
        videos: List<VideoItem>,
        query: String,
        folderName: String?
    ): List<VideoItem> {
        return videos.filter { video ->
            val matchQuery = query.isBlank() || video.name.contains(query, ignoreCase = true)
            val matchFolder = folderName == null || video.folderName == folderName
            matchQuery && matchFolder
        }
    }
}
