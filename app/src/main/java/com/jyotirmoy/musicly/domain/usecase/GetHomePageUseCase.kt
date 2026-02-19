package com.jyotirmoy.musicly.domain.usecase

import com.jyotirmoy.musicly.data.model.OnlineHomePage
import com.jyotirmoy.musicly.data.repository.YouTubeRepository
import javax.inject.Inject

/**
 * Use case for fetching the YouTube Music home feed.
 *
 * Retrieves personalized content sections like "Listen Again",
 * "Mixed for you", "New releases", etc.
 */
class GetHomePageUseCase @Inject constructor(
    private val youTubeRepository: YouTubeRepository,
) {
    /**
     * Get the home feed.
     *
     * @param continuation Optional continuation token for loading more sections.
     */
    suspend operator fun invoke(continuation: String? = null): Result<OnlineHomePage> {
        return youTubeRepository.getHome(continuation)
    }
}
