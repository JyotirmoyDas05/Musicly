package com.jyotirmoy.musicly.domain.usecase

import com.jyotirmoy.musicly.data.model.OnlineAlbumDetail
import com.jyotirmoy.musicly.data.model.MediaMetadata
import com.jyotirmoy.musicly.data.repository.YouTubeRepository
import javax.inject.Inject

/**
 * Use case for fetching full album details from YouTube Music.
 *
 * Retrieves the album page including track list, other versions,
 * and optionally caches song metadata for recently viewed albums.
 */
class GetAlbumDetailsUseCase @Inject constructor(
    private val youTubeRepository: YouTubeRepository,
) {
    /**
     * Get full album details including songs.
     *
     * @param browseId The YouTube Music browse ID for the album (e.g., "MPREb_...")
     * @param cacheTracksLocally If true, caches all song metadata in the local database
     *                          for faster re-access and offline metadata availability.
     */
    suspend operator fun invoke(
        browseId: String,
        cacheTracksLocally: Boolean = false,
    ): Result<OnlineAlbumDetail> {
        val result = youTubeRepository.getAlbumDetails(browseId)

        if (cacheTracksLocally && result.isSuccess) {
            result.getOrNull()?.songs?.forEach { song ->
                youTubeRepository.cacheOnlineSong(song)
            }
        }

        return result
    }
}
