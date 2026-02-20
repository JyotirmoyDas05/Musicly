package com.jyotirmoy.musicly.data.network.mapper

import com.jyotirmoy.musicly.data.network.dto.GitHubAssetDto
import com.jyotirmoy.musicly.data.network.dto.GitHubReleaseDto
import com.jyotirmoy.musicly.domain.model.ApkAsset
import com.jyotirmoy.musicly.domain.model.GitHubRelease

fun GitHubReleaseDto.toDomain(): GitHubRelease {
    return GitHubRelease(
        tagName = tagName,
        releaseNotes = body ?: "",
        assets = assets.mapNotNull { it.toDomain() }
    )
}

fun GitHubAssetDto.toDomain(): ApkAsset? {
    // Only include APK files
    if (!name.endsWith(".apk", ignoreCase = true)) {
        return null
    }

    // Detect architecture from APK name
    val architecture = when {
        name.contains("arm64-v8a", ignoreCase = true) -> "arm64-v8a"
        name.contains("armeabi-v7a", ignoreCase = true) -> "armeabi-v7a"
        name.contains("x86_64", ignoreCase = true) -> "x86_64"
        name.contains("x86", ignoreCase = true) -> "x86"
        else -> "universal"
    }

    return ApkAsset(
        name = name,
        downloadUrl = browserDownloadUrl,
        architecture = architecture
    )
}
