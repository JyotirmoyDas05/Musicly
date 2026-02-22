package com.jyotirmoy.musicly.data.model

import androidx.compose.runtime.Immutable

@Immutable
data class SearchHistoryItem(
    val id: Long? = null,
    val query: String,
    val timestamp: Long,
    val itemType: String = "query",
    val itemId: String? = null,
    val subtitle: String? = null,
    val thumbnailUrl: String? = null
)
