package com.jyotirmoy.musicly.domain.usecase

import com.jyotirmoy.musicly.data.repository.YouTubeRepository
import javax.inject.Inject

/**
 * Use case for resolving stream URLs for online playback.
 *
 * Handles checking the format cache, falling back to API resolution,
 * and caching the result for subsequent plays.
 *
 * Note: Full integration with [YTPlayerUtils] (including client fallback,
 * PoToken generation, cipher deobfuscation) will be completed in Phase 3
 * when integrating with MusicService.
 */
class GetStreamUrlUseCase @Inject constructor(
    private val youTubeRepository: YouTubeRepository,
) {
    /**
     * Resolve the audio stream URL for the given video ID.
     *
     * @param videoId YouTube video ID
     * @return The direct audio stream URL, or failure if resolution fails.
     */
    suspend operator fun invoke(videoId: String): Result<String> {
        return youTubeRepository.getStreamUrl(videoId)
    }
}
