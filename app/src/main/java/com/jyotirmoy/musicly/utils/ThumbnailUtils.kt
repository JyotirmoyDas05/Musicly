package com.jyotirmoy.musicly.utils

/**
 * Resizes a YouTube/Google thumbnail URL to the specified dimensions.
 *
 * YouTube Music thumbnail URLs from `lh3.googleusercontent.com` contain size parameters like
 * `=w226-h226-...`. This function replaces those parameters with the requested dimensions,
 * allowing us to request higher resolution thumbnails.
 *
 * Also handles `yt3.ggpht.com` URLs (artist/channel thumbnails) which use `=s{size}` format.
 *
 * @param width desired width in pixels, or null to auto-calculate from height
 * @param height desired height in pixels, or null to auto-calculate from width
 * @return the modified URL with new size parameters, or the original URL if it doesn't match
 */
fun String.resize(
    width: Int? = null,
    height: Int? = null,
): String {
    if (width == null && height == null) return this
    "https://lh3\\.googleusercontent\\.com/.*=w(\\d+)-h(\\d+).*".toRegex()
        .matchEntire(this)?.groupValues?.let { group ->
        val (W, H) = group.drop(1).map { it.toInt() }
        var w = width
        var h = height
        if (w != null && h == null) h = (w / W) * H
        if (w == null && h != null) w = (h / H) * W
        return "${split("=w")[0]}=w$w-h$h-p-l90-rj"
    }
    if (this matches "https://yt3\\.ggpht\\.com/.*=s(\\d+)".toRegex()) {
        return "$this-s${width ?: height}"
    }
    return this
}
