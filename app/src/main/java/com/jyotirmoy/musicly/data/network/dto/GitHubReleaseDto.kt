package com.jyotirmoy.musicly.data.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GitHubReleaseDto(
    @SerialName("tag_name")
    val tagName: String,

    @SerialName("body")
    val body: String? = null,

    @SerialName("assets")
    val assets: List<GitHubAssetDto> = emptyList()
)

@Serializable
data class GitHubAssetDto(
    @SerialName("name")
    val name: String,

    @SerialName("browser_download_url")
    val browserDownloadUrl: String
)
