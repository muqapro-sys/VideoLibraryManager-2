package com.khaly.videolibrary.data

import android.content.Context
import com.khaly.videolibrary.domain.SortMode
import com.khaly.videolibrary.domain.VideoFolder
import com.khaly.videolibrary.domain.VideoItem

class VideoRepository(context: Context) {
    private val scanner = MediaStoreVideoScanner(context.applicationContext)

    fun loadVideos(): List<VideoItem> = scanner.scanVideos()

    fun sortVideos(videos: List<VideoItem>, sortMode: SortMode): List<VideoItem> {
        return when (sortMode) {
            SortMode.DATE_NEWEST -> videos.sortedByDescending { it.dateAddedSeconds }
            SortMode.DATE_OLDEST -> videos.sortedBy { it.dateAddedSeconds }
            SortMode.NAME_ASC -> videos.sortedBy { it.name.lowercase() }
            SortMode.NAME_DESC -> videos.sortedByDescending { it.name.lowercase() }
            SortMode.SIZE_LARGEST -> videos.sortedByDescending { it.sizeBytes }
            SortMode.SIZE_SMALLEST -> videos.sortedBy { it.sizeBytes }
            SortMode.DURATION_LONGEST -> videos.sortedByDescending { it.durationMs }
            SortMode.DURATION_SHORTEST -> videos.sortedBy { it.durationMs }
        }
    }

    fun buildFolders(videos: List<VideoItem>): List<VideoFolder> {
        return videos
            .groupBy { it.folderName }
            .map { VideoFolder(name = it.key, videos = it.value) }
            .sortedByDescending { it.latestDateAddedSeconds }
    }
}
