package com.example.data.util

import android.app.Activity
import android.content.Context
import android.util.Log
import com.example.R
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

object AdMobManager {
    private const val TAG = "AdMobManager"
    private const val PREFS_NAME = "admob_settings_prefs"
    
    // SharedPreferences Keys
    private const val KEY_APP_ID = "admob_app_id"
    private const val KEY_BANNER_ID = "admob_banner_id"
    private const val KEY_INTERSTITIAL_ID = "admob_interstitial_id"
    private const val KEY_LIMIT_COUNT = "admob_limit_count"
    private const val KEY_USER_CONSENT = "admob_user_consent"

    // Consent constants
    const val CONSENT_PENDING = "PENDING"
    const val CONSENT_ACCEPTED = "ACCEPTED"
    const val CONSENT_DECLINED = "DECLINED"

    private var isInitialized = false
    private var mInterstitialAd: InterstitialAd? = null
    private var isAdLoading = false
    private var criticalEventCount = 0

    // Initialize the Google Mobile Ads SDK on main thread/coroutine context safely
    fun initialize(context: Context) {
        if (!AdConfig.ADS_ENABLED) {
            Log.d(TAG, "AdMob initialization bypassed because ADS_ENABLED is false in AdConfig")
            return
        }
        val consent = getUserConsentStatus(context)
        if (consent == CONSENT_DECLINED) {
            Log.d(TAG, "AdMob initialization bypassed because user declined consent")
            return
        }
        if (isInitialized) return
        try {
            MobileAds.initialize(context) {
                Log.d(TAG, "Google Mobile Ads SDK Initialized")
                isInitialized = true
                // Preload first interstitial ad on initialization (but do not show right away, ensuring no startup ads)
                if (!AdConfig.NO_STARTUP_ADS || isInitialized) {
                    loadInterstitial(context.applicationContext)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize AdMob SDK", e)
        }
    }

    // Load/preload an interstitial ad dynamically with the configured AdUnit ID
    fun loadInterstitial(context: Context) {
        if (!AdConfig.ADS_ENABLED) return
        if (isAdLoading) return
        isAdLoading = true

        val adUnitId = getInterstitialAdUnitId(context)
        Log.d(TAG, "Preloading Interstitial Ad with unit ID: $adUnitId")

        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            context,
            adUnitId,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.e(TAG, "Interstitial Ad failed to load: ${adError.message}")
                    mInterstitialAd = null
                    isAdLoading = false
                }

                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    Log.d(TAG, "Interstitial Ad loaded and ready")
                    mInterstitialAd = interstitialAd
                    isAdLoading = false
                }
            }
        )
    }

    // Increment count & try showing the Interstitial Ad at a critical section (obeying our frequency bounds)
    fun showInterstitialAtCriticalSection(activity: Activity, onAdDismissed: () -> Unit = {}) {
        if (!AdConfig.ADS_ENABLED) {
            Log.d(TAG, "AdMob show request bypassed because ADS_ENABLED is false.")
            onAdDismissed()
            return
        }

        val limitThreshold = getLimitCount(activity)

        criticalEventCount++
        Log.d(TAG, "Critical action logged. Counter = $criticalEventCount / Limit = $limitThreshold")

        if (criticalEventCount >= limitThreshold) {
            // Reset counter so we don't bombard the user even if load fails or succeeds
            criticalEventCount = 0

            val ad = mInterstitialAd
            if (ad != null) {
                ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        Log.d(TAG, "Ad dismissed by user. Fetching new placeholder.")
                        mInterstitialAd = null
                        onAdDismissed()
                        loadInterstitial(activity.applicationContext)
                    }

                    override fun onAdFailedToShowFullScreenContent(adError: com.google.android.gms.ads.AdError) {
                        Log.e(TAG, "Ad failed to show full screen: ${adError.message}")
                        mInterstitialAd = null
                        onAdDismissed()
                        loadInterstitial(activity.applicationContext)
                    }

                    override fun onAdShowedFullScreenContent() {
                        Log.d(TAG, "Ad shown to user fully.")
                    }
                }
                ad.show(activity)
            } else {
                Log.d(TAG, "Interstitial ad was requested but was not fully buffered. Continuing action directly.")
                onAdDismissed()
                loadInterstitial(activity.applicationContext) // Retrigger buffer attempt
            }
        } else {
            // Not reached rate limiting interval, execute immediately and silently
            onAdDismissed()
        }
    }

    // Dynamic preferences getter with fallbacks to compiled XML values
    fun getAppId(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_APP_ID, AdConfig.DEFAULT_APP_ID) ?: AdConfig.DEFAULT_APP_ID
    }

    fun getBannerAdUnitId(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_BANNER_ID, AdConfig.DEFAULT_BANNER_ID) ?: AdConfig.DEFAULT_BANNER_ID
    }

    fun getInterstitialAdUnitId(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_INTERSTITIAL_ID, AdConfig.DEFAULT_INTERSTITIAL_ID) ?: AdConfig.DEFAULT_INTERSTITIAL_ID
    }

    fun getLimitCount(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_LIMIT_COUNT, AdConfig.DEFAULT_CRITICAL_ACTIONS_INTERVAL)
    }

    // Save customized developer override IDs securely
    fun saveAdMobConfig(
        context: Context,
        appId: String,
        bannerId: String,
        interstitialId: String,
        limitCount: Int
    ) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString(KEY_APP_ID, appId.trim())
            putString(KEY_BANNER_ID, bannerId.trim())
            putString(KEY_INTERSTITIAL_ID, interstitialId.trim())
            putInt(KEY_LIMIT_COUNT, limitCount)
            // Configuration is finalized, mark consent as PENDING to show the consent banner on first session
            putString(KEY_USER_CONSENT, CONSENT_PENDING)
            apply()
        }
        Log.d(TAG, "Config updated & finalized. Consent status set to PENDING.")
        
        // Reinitialize safely or prepare to load
        isInitialized = false // Reset initialization to allow re-checking configuration with consent
        if (getUserConsentStatus(context) != CONSENT_DECLINED) {
            initialize(context.applicationContext)
        }
    }

    fun getUserConsentStatus(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_USER_CONSENT, CONSENT_PENDING) ?: CONSENT_PENDING
    }

    fun setUserConsentStatus(context: Context, status: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_USER_CONSENT, status).apply()
        Log.d(TAG, "User consent updated to: $status")
        if (status == CONSENT_ACCEPTED) {
            initialize(context.applicationContext)
        }
    }
}
