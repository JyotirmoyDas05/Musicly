package com.jyotirmoy.musicly.domain.usecase

import com.jyotirmoy.musicly.data.model.OnlineHomePage
import com.jyotirmoy.musicly.data.repository.YouTubeRepository
import javax.inject.Inject

/**
 * Use case for fetching the YouTube Music explore/discover page.
 *
 * Retrieves new release albums and mood & genres categories.
 */
class GetExplorePageUseCase @Inject constructor(
    private val youTubeRepository: YouTubeRepository,
) {
    /**
     * Get the explore page content.
     */
    suspend operator fun invoke(): Result<OnlineHomePage> {
        return youTubeRepository.getExplorePage()
    }
}
