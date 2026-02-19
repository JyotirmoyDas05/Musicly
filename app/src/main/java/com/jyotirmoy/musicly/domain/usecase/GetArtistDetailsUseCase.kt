package com.jyotirmoy.musicly.domain.usecase

import com.jyotirmoy.musicly.data.model.OnlineArtistDetail
import com.jyotirmoy.musicly.data.repository.YouTubeRepository
import javax.inject.Inject

/**
 * Use case for fetching artist details from YouTube Music.
 *
 * Retrieves the artist page including sections for songs,
 * albums, singles, videos, related artists, etc.
 */
class GetArtistDetailsUseCase @Inject constructor(
    private val youTubeRepository: YouTubeRepository,
) {
    /**
     * Get full artist details.
     *
     * @param browseId The YouTube Music browse ID for the artist (e.g., "UC...")
     */
    suspend operator fun invoke(browseId: String): Result<OnlineArtistDetail> {
        return youTubeRepository.getArtistDetails(browseId)
    }
}
