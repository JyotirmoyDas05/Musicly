package com.jyotirmoy.musicly.domain.repository

import com.jyotirmoy.musicly.domain.model.UpdateResult

interface UpdateRepository {
    suspend fun fetchLatestRelease(includePrerelease: Boolean): UpdateResult
}
