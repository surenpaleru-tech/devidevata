// AdConfig.js
// Centralized configuration for Google AdMob in the DeviDevata App.
// Exposes easy toggles and configuration options to control advertisement loading and gating.

const AdConfig = {
  // 1. Global toggle to enable or disable all advertisements in the application
  adsEnabled: true,

  // 2. Strict protection against intrusive/annoying startup ads
  // If true, ad loads are buffered silently in the background, but never shown directly on app opening.
  noStartupAds: true,

  // 3. Ad Display Gating & Frequency Limiting
  // Threshold to determine how often Interstitial Ads are shown at critical application lifecycles.
  // Gating ensures ads only display after 'X' critical events (e.g., completing 1 Mala or starting 3 stotrams).
  // A higher value means fewer ads (and more user-friendly experience). Example: 3 or 4.
  criticalActionsInterval: 3,

  // 4. Configurable AdMob Application ID and Unit Ad IDs
  // Test IDs provided by Google are used by default. Replace these with your live AdMobile App/Unit IDs.
  android: {
    // AdMob App ID
    appId: "ca-app-pub-3940256099942544~3347511713",

    // Standard Medium Rectangle Ad unit ID (300x250) - used for sacred bottom display banners
    bannerAdUnitId: "ca-app-pub-3940256099942544/6300978111",

    // Full screen overlay Interstitial Ad unit ID - triggered at critical checkpoints only
    interstitialAdUnitId: "ca-app-pub-3940256099942544/1033173712"
  },

  // Metadata/documentation for multi-framework integration or notes
  metadata: {
    packageName: "com.example",
    description: "Configurable AdMob banner and gated interstitial integration with local persistent override capabilities",
    lastUpdated: "2026-06-12"
  }
};

module.exports = AdConfig;
