package com.jyotirmoy.musicly.data.network.lyrics

import com.google.gson.annotations.SerializedName

/**
* Represents the LRCLIB API response.
 * Contains song lyrics in both plain and synced formats.
 */
data class LrcLibResponse(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("artistName") val artistName: String,
    @SerializedName("albumName") val albumName: String,
    @SerializedName("duration") val duration: Double,
    @SerializedName("plainLyrics") val plainLyrics: String?,
    @SerializedName("syncedLyrics") val syncedLyrics: String?
)