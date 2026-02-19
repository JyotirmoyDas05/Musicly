package com.jyotirmoy.musicly.domain.usecase

import com.jyotirmoy.musicly.data.model.MediaMetadata
import com.jyotirmoy.musicly.data.repository.YouTubeRepository
import javax.inject.Inject

/**
 * Use case for fetching lyrics for online songs.
 */
class GetOnlineLyricsUseCase @Inject constructor(
    private val youTubeRepository: YouTubeRepository,
) {
    /**
     * Get lyrics for the given video ID.
     *
     * @param videoId YouTube video ID
     * @return The lyrics text, or null if not available.
     */
    suspend operator fun invoke(videoId: String): Result<String?> {
        return youTubeRepository.getLyrics(videoId)
    }
}

/**
 * Use case for fetching related/recommended songs for autoplay queue.
 */
class GetRelatedSongsUseCase @Inject constructor(
    private val youTubeRepository: YouTubeRepository,
) {
    /**
     * Get related songs for the given video ID.
     *
     * @param videoId YouTube video ID
     * @return List of related song metadata for queue continuation.
     */
    suspend operator fun invoke(videoId: String): Result<List<MediaMetadata>> {
        return youTubeRepository.getRelated(videoId)
    }
}
