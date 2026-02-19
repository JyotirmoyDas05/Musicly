package com.jyotirmoy.musicly.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        AlbumArtThemeEntity::class,
        SearchHistoryEntity::class,
        SongEntity::class,
        AlbumEntity::class,
        ArtistEntity::class,
        TransitionRuleEntity::class,
        SongArtistCrossRef::class,
        SongEngagementEntity::class,
        FavoritesEntity::class,
        LyricsEntity::class,
        OnlineSongEntity::class,
        FormatCacheEntity::class,
        OnlineSearchHistoryEntity::class,
    ],
    version = 16, // Added online music entities (online_songs, online_format_cache, online_search_history)
    exportSchema = false
)
abstract class MusiclyDatabase : RoomDatabase() {
    abstract fun albumArtThemeDao(): AlbumArtThemeDao
    abstract fun searchHistoryDao(): SearchHistoryDao
    abstract fun musicDao(): MusicDao
    abstract fun transitionDao(): TransitionDao
    abstract fun engagementDao(): EngagementDao
    abstract fun favoritesDao(): FavoritesDao
    abstract fun lyricsDao(): LyricsDao
    abstract fun onlineDao(): OnlineDao

    companion object {
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE songs ADD COLUMN parent_directory_path TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE songs ADD COLUMN lyrics TEXT")
            }
        }

//        val MIGRATION_6_7 = object : Migration(6, 7) {
//            override fun migrate(db: SupportSQLiteDatabase) {
//                db.execSQL("ALTER TABLE songs ADD COLUMN date_added INTEGER NOT NULL DEFAULT 0")
//            }
//        }
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE songs ADD COLUMN mime_type TEXT")
                db.execSQL("ALTER TABLE songs ADD COLUMN bitrate INTEGER")
                db.execSQL("ALTER TABLE songs ADD COLUMN sample_rate INTEGER")
            }
        }

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add album_artist column to songs table
                db.execSQL("ALTER TABLE songs ADD COLUMN album_artist TEXT DEFAULT NULL")
                
                // Create song_artist_cross_ref junction table for many-to-many relationship
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS song_artist_cross_ref (
                        song_id INTEGER NOT NULL,
                        artist_id INTEGER NOT NULL,
                        is_primary INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY (song_id, artist_id),
                        FOREIGN KEY (song_id) REFERENCES songs(id) ON DELETE CASCADE,
                        FOREIGN KEY (artist_id) REFERENCES artists(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                
                // Create indices for efficient queries
                db.execSQL("CREATE INDEX IF NOT EXISTS index_song_artist_cross_ref_song_id ON song_artist_cross_ref(song_id)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_song_artist_cross_ref_artist_id ON song_artist_cross_ref(artist_id)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_song_artist_cross_ref_is_primary ON song_artist_cross_ref(is_primary)")
                
                // Migrate existing song-artist relationships to junction table
                // Each existing song gets its current artist as the primary artist
                db.execSQL("""
                    INSERT OR REPLACE INTO song_artist_cross_ref (song_id, artist_id, is_primary)
                    SELECT id, artist_id, 1 FROM songs WHERE artist_id IS NOT NULL
                """.trimIndent())
            }
        }

        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add image_url column to artists table for Deezer artist images
                db.execSQL("ALTER TABLE artists ADD COLUMN image_url TEXT DEFAULT NULL")
            }
        }

        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Create song_engagements table for tracking play statistics
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS song_engagements (
                        song_id TEXT NOT NULL PRIMARY KEY,
                        play_count INTEGER NOT NULL DEFAULT 0,
                        total_play_duration_ms INTEGER NOT NULL DEFAULT 0,
                        last_played_timestamp INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
            }
        }

        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS favorites (
                        songId INTEGER NOT NULL PRIMARY KEY,
                        isFavorite INTEGER NOT NULL,
                        timestamp INTEGER NOT NULL
                    )
                """.trimIndent())
                
                // Migrate existing favorites from songs table if possible
                // Note: We need to cast is_favorite (boolean/int) to ensure compatibility
                db.execSQL("""
                    INSERT OR IGNORE INTO favorites (songId, isFavorite, timestamp)
                    SELECT id, is_favorite, ? FROM songs WHERE is_favorite = 1
                """, arrayOf(System.currentTimeMillis()))
            }
        }
        val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS `lyrics` (`songId` INTEGER NOT NULL, `content` TEXT NOT NULL, `isSynced` INTEGER NOT NULL DEFAULT 0, `source` TEXT, PRIMARY KEY(`songId`))"
                )
                database.execSQL(
                    "INSERT INTO lyrics (songId, content) SELECT id, lyrics FROM songs WHERE lyrics IS NOT NULL AND lyrics != ''"
                )
            }
        }

        val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE album_art_themes ADD COLUMN paletteStyle TEXT NOT NULL DEFAULT 'tonal_spot'"
                )

                val newRoleColumns = listOf(
                    "surfaceBright",
                    "surfaceDim",
                    "surfaceContainer",
                    "surfaceContainerHigh",
                    "surfaceContainerHighest",
                    "surfaceContainerLow",
                    "surfaceContainerLowest",
                    "primaryFixed",
                    "primaryFixedDim",
                    "onPrimaryFixed",
                    "onPrimaryFixedVariant",
                    "secondaryFixed",
                    "secondaryFixedDim",
                    "onSecondaryFixed",
                    "onSecondaryFixedVariant",
                    "tertiaryFixed",
                    "tertiaryFixedDim",
                    "onTertiaryFixed",
                    "onTertiaryFixedVariant"
                )

                val prefixes = listOf("light_", "dark_")
                prefixes.forEach { prefix ->
                    newRoleColumns.forEach { role ->
                        database.execSQL(
                            "ALTER TABLE album_art_themes ADD COLUMN ${prefix}${role} TEXT NOT NULL DEFAULT '#00000000'"
                        )
                    }
                }

                // The table is a cache; wipe stale rows so we always regenerate with full token data.
                database.execSQL("DELETE FROM album_art_themes")
            }
        }

        val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Create online_songs table for caching YouTube Music tracks
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS online_songs (
                        id TEXT NOT NULL PRIMARY KEY,
                        title TEXT NOT NULL,
                        artists_text TEXT NOT NULL,
                        artists_json TEXT NOT NULL,
                        album_id TEXT DEFAULT NULL,
                        album_name TEXT DEFAULT NULL,
                        duration INTEGER DEFAULT NULL,
                        thumbnail_url TEXT DEFAULT NULL,
                        explicit INTEGER NOT NULL DEFAULT 0,
                        in_library INTEGER NOT NULL DEFAULT 0,
                        is_favorite INTEGER NOT NULL DEFAULT 0,
                        total_play_time_ms INTEGER NOT NULL DEFAULT 0,
                        play_count INTEGER NOT NULL DEFAULT 0,
                        last_played_timestamp INTEGER NOT NULL DEFAULT 0,
                        date_added INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_online_songs_title ON online_songs(title)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_online_songs_album_id ON online_songs(album_id)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_online_songs_in_library ON online_songs(in_library)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_online_songs_last_played_timestamp ON online_songs(last_played_timestamp)")

                // Create online_format_cache table for caching resolved stream URLs
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS online_format_cache (
                        id TEXT NOT NULL PRIMARY KEY,
                        itag INTEGER DEFAULT NULL,
                        mime_type TEXT DEFAULT NULL,
                        bitrate INTEGER DEFAULT NULL,
                        sample_rate INTEGER DEFAULT NULL,
                        content_length INTEGER DEFAULT NULL,
                        loudness_db REAL DEFAULT NULL,
                        playback_url TEXT DEFAULT NULL,
                        expires_at INTEGER NOT NULL DEFAULT 0,
                        cached_at INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())

                // Create online_search_history table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS online_search_history (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `query` TEXT NOT NULL,
                        timestamp INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_online_search_history_query ON online_search_history(`query`)")
            }
        }
    }
}
