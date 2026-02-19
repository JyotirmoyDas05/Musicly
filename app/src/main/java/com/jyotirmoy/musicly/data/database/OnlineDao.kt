package com.jyotirmoy.musicly.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/**
 * DAO for online music content (YouTube Music songs, format cache, online search history).
 */
@Dao
interface OnlineDao {

    // ---- Online Songs ----

    @Upsert
    suspend fun upsertOnlineSong(song: OnlineSongEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOnlineSongIfAbsent(song: OnlineSongEntity)

    @Query("SELECT * FROM online_songs WHERE id = :videoId")
    suspend fun getOnlineSong(videoId: String): OnlineSongEntity?

    @Query("SELECT * FROM online_songs WHERE id = :videoId")
    fun observeOnlineSong(videoId: String): Flow<OnlineSongEntity?>

    @Query("SELECT * FROM online_songs WHERE in_library = 1 ORDER BY date_added DESC")
    fun observeLibrarySongs(): Flow<List<OnlineSongEntity>>

    @Query("SELECT * FROM online_songs WHERE is_favorite = 1 ORDER BY date_added DESC")
    fun observeFavoriteSongs(): Flow<List<OnlineSongEntity>>

    @Query("SELECT * FROM online_songs ORDER BY last_played_timestamp DESC LIMIT :limit")
    fun observeRecentlyPlayed(limit: Int = 50): Flow<List<OnlineSongEntity>>

    @Query("SELECT * FROM online_songs WHERE play_count > 0 ORDER BY play_count DESC LIMIT :limit")
    fun observeMostPlayed(limit: Int = 50): Flow<List<OnlineSongEntity>>

    @Query("""
        UPDATE online_songs 
        SET play_count = play_count + 1, 
            total_play_time_ms = total_play_time_ms + :durationMs,
            last_played_timestamp = :timestamp
        WHERE id = :videoId
    """)
    suspend fun recordOnlinePlay(videoId: String, durationMs: Long, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE online_songs SET is_favorite = :isFavorite WHERE id = :videoId")
    suspend fun setOnlineFavorite(videoId: String, isFavorite: Boolean)

    @Query("UPDATE online_songs SET in_library = :inLibrary WHERE id = :videoId")
    suspend fun setInLibrary(videoId: String, inLibrary: Boolean)

    @Query("DELETE FROM online_songs WHERE id = :videoId")
    suspend fun deleteOnlineSong(videoId: String)

    @Query("SELECT COUNT(*) FROM online_songs WHERE in_library = 1")
    fun observeLibrarySongCount(): Flow<Int>

    // ---- Format Cache ----

    @Upsert
    suspend fun upsertFormatCache(format: FormatCacheEntity)

    @Query("SELECT * FROM online_format_cache WHERE id = :videoId AND expires_at > :now")
    suspend fun getValidFormatCache(videoId: String, now: Long = System.currentTimeMillis()): FormatCacheEntity?

    @Query("DELETE FROM online_format_cache WHERE expires_at <= :now")
    suspend fun clearExpiredFormats(now: Long = System.currentTimeMillis())

    @Query("DELETE FROM online_format_cache WHERE id = :videoId")
    suspend fun deleteFormatCache(videoId: String)

    @Query("DELETE FROM online_format_cache")
    suspend fun clearAllFormatCache()

    // ---- Online Search History ----

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOnlineSearchHistory(entry: OnlineSearchHistoryEntity)

    @Query("SELECT * FROM online_search_history ORDER BY timestamp DESC LIMIT :limit")
    fun observeOnlineSearchHistory(limit: Int = 20): Flow<List<OnlineSearchHistoryEntity>>

    @Query("SELECT * FROM online_search_history ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getOnlineSearchHistory(limit: Int = 20): List<OnlineSearchHistoryEntity>

    @Query("DELETE FROM online_search_history WHERE `query` = :query")
    suspend fun deleteOnlineSearchHistoryByQuery(query: String)

    @Query("DELETE FROM online_search_history")
    suspend fun clearOnlineSearchHistory()
}
