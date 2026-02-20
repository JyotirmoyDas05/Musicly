package com.jyotirmoy.musicly.data.model

import androidx.compose.runtime.Immutable
import java.io.File

@Immutable
data class DirectoryItem(
    val path: String,
    var isAllowed: Boolean
) {
    val displayName: String
        get() = File(path).name.ifEmpty { path } // Shows the folder name or the path if it is the root
}