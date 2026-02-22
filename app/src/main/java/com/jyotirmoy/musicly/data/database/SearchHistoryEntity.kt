package com.jyotirmoy.musicly.data.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.jyotirmoy.musicly.data.model.SearchHistoryItem

@Entity(tableName = "search_history")
data class SearchHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "query") val query: String, // Also used as Title for items
    @ColumnInfo(name = "timestamp") val timestamp: Long,
    @ColumnInfo(name = "item_type", defaultValue = "query") val itemType: String = "query",
    @ColumnInfo(name = "item_id") val itemId: String? = null,
    @ColumnInfo(name = "subtitle") val subtitle: String? = null,
    @ColumnInfo(name = "thumbnail_url") val thumbnailUrl: String? = null
)

fun SearchHistoryEntity.toSearchHistoryItem(): SearchHistoryItem {
    return SearchHistoryItem(
        id = this.id,
        query = this.query,
        timestamp = this.timestamp,
        itemType = this.itemType,
        itemId = this.itemId,
        subtitle = this.subtitle,
        thumbnailUrl = this.thumbnailUrl
    )
}

fun SearchHistoryItem.toEntity(): SearchHistoryEntity {
    return SearchHistoryEntity(
        id = this.id ?: 0,
        query = this.query,
        timestamp = this.timestamp,
        itemType = this.itemType,
        itemId = this.itemId,
        subtitle = this.subtitle,
        thumbnailUrl = this.thumbnailUrl
    )
}
