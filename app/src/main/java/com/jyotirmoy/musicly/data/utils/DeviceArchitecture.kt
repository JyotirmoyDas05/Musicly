package com.jyotirmoy.musicly.data.utils

import android.os.Build
import com.jyotirmoy.musicly.domain.model.ApkAsset

object DeviceArchitecture {
    
    private fun getDeviceArchitecture(): String {
        return when {
            Build.SUPPORTED_ABIS.contains("arm64-v8a") -> "arm64-v8a"
            Build.SUPPORTED_ABIS.contains("armeabi-v7a") -> "armeabi-v7a"
            Build.SUPPORTED_ABIS.contains("x86_64") -> "x86_64"
            Build.SUPPORTED_ABIS.contains("x86") -> "x86"
            else -> "universal"
        }
    }

    fun selectBestApk(assets: List<ApkAsset>): ApkAsset? {
        if (assets.isEmpty()) return null

        val deviceArch = getDeviceArchitecture()

        // First, try to find exact architecture match
        assets.find { it.architecture == deviceArch }?.let { return it }

        // If no exact match, prefer arm64-v8a (most common)
        assets.find { it.architecture == "arm64-v8a" }?.let { return it }

        // Fall back to universal APK
        assets.find { it.architecture == "universal" }?.let { return it }

        // If nothing else, return first available
        return assets.firstOrNull()
    }
}
