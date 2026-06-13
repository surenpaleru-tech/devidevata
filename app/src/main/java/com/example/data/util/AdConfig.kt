package com.example.data.util

/**
 * Centrally manages AdMob configuration. Matches the properties declared in AdConfig.js.
 * Provides unified global controls for enabling ads, custom IDs, and gating parameters.
 */
object AdConfig {
    // Global toggle to enable or disable ads in the application entirely
    const val ADS_ENABLED = true

    // Gated safety parameter ensuring NO ads show on app startup
    const val NO_STARTUP_ADS = true

    // Default rate-limiting threshold: interstitial ads will only display after X critical sections
    const val DEFAULT_CRITICAL_ACTIONS_INTERVAL = 3

    // Google AdMob Standard Test Credentials
    const val DEFAULT_APP_ID = "ca-app-pub-3940256099942544~3347511713"
    const val DEFAULT_BANNER_ID = "ca-app-pub-3940256099942544/6300978111"
    const val DEFAULT_INTERSTITIAL_ID = "ca-app-pub-3940256099942544/1033173712"
}
