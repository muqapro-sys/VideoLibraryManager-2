package com.khaly.videolibrary.domain

import android.net.Uri

data class VideoItem(
    val id: Long,
    val uri: Uri,
    val name: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val dateAddedSeconds: Long,
    val dateModifiedSeconds: Long,
    val folderName: String,
    val width: Int,
    val height: Int,
    val mimeType: String,
    val filePath: String? = null
)

data class VideoFolder(
    val name: String,
    val videos: List<VideoItem>
) {
    val count: Int get() = videos.size
    val totalSizeBytes: Long get() = videos.sumOf { it.sizeBytes }
    val latestDateAddedSeconds: Long get() = videos.maxOfOrNull { it.dateAddedSeconds } ?: 0L
}

enum class SortMode {
    DATE_NEWEST,
    DATE_OLDEST,
    NAME_ASC,
    NAME_DESC,
    SIZE_LARGEST,
    SIZE_SMALLEST,
    DURATION_LONGEST,
    DURATION_SHORTEST
}

enum class ViewMode {
    GRID,
    LIST
}
