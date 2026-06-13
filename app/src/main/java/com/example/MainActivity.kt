package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.data.api.RetrofitClient
import com.example.data.database.AppDatabase
import com.example.data.repository.DivineRepository
import com.example.data.database.StotramPuja
import com.example.ui.screens.CalendarScreen
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.GalleryScreen
import com.example.ui.screens.DailySpiritualInsightDialog
import com.example.ui.screens.SadhanaScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.AdMobConsentBanner
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.DivineViewModel
import com.example.data.util.AdMobManager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize Google AdMob
        AdMobManager.initialize(this)

        // Initialize SQLite Room database
        val database = AppDatabase.getDatabase(this)
        val dao = database.divineDao()

        // Initialize Repository & ViewModel via manual injection (clean architecture)
        val repository = DivineRepository(dao, RetrofitClient.service, this)

        setContent {
            // Initialize ViewModel tied to this activity lifecycle
            val viewModel = remember {
                DivineViewModel(application, repository)
            }

            val isDarkTheme by viewModel.isDarkTheme.collectAsState()
            var showSettingsDialog by remember { mutableStateOf(false) }
            var showDailyInsightDialog by remember { mutableStateOf(false) }

            // Configurable AdMob override state variables
            val context = LocalContext.current
            var customAppId by remember { mutableStateOf("") }
            var customBannerId by remember { mutableStateOf("") }
            var customInterstitialId by remember { mutableStateOf("") }
            var customLimitCount by remember { mutableStateOf("") }
            var adMobExpanded by remember { mutableStateOf(false) }

            MyApplicationTheme(darkTheme = isDarkTheme) {
                val activeTab by viewModel.activeTab.collectAsState()

                Scaffold(
                    modifier = Modifier.fillMaxSize().navigationBarsPadding(),
                    topBar = {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            Text(
                                text = "DeviDevata",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                            Row(
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                // Daily Spiritual Insight Button before settings, in the top right
                                IconButton(
                                    onClick = { showDailyInsightDialog = true },
                                    modifier = Modifier.testTag("appbar_daily_insight_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = "Daily Spiritual Insight",
                                        tint = Color(0xFFFF9100), // Vibrant Saffron-orange brand indicator
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { showSettingsDialog = true },
                                    modifier = Modifier.testTag("settings_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = "Settings",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    },
                    bottomBar = {
                        Column {
                            PersistentAudioController(viewModel = viewModel)

                            NavigationBar(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(64.dp)
                                    .testTag("bottom_nav_bar"),
                                containerColor = MaterialTheme.colorScheme.surface,
                                tonalElevation = 8.dp
                            ) {
                                NavigationBarItem(
                                    selected = activeTab == "gallery",
                                    onClick = { viewModel.selectTab("gallery") },
                                    icon = {
                                        Icon(
                                            Icons.Default.Home,
                                            contentDescription = "Gallery screen"
                                        )
                                    },
                                    label = { Text("Gallery", style = MaterialTheme.typography.labelSmall) },
                                    modifier = Modifier.testTag("tab_gallery")
                                )

                                NavigationBarItem(
                                    selected = activeTab == "library",
                                    onClick = { viewModel.selectTab("library") },
                                    icon = {
                                        Icon(
                                            Icons.Default.List,
                                            contentDescription = "Sacred hymns and puja library"
                                        )
                                    },
                                    label = { Text("Library", style = MaterialTheme.typography.labelSmall) },
                                    modifier = Modifier.testTag("tab_library")
                                )

                                NavigationBarItem(
                                    selected = activeTab == "calendar",
                                    onClick = { viewModel.selectTab("calendar") },
                                    icon = {
                                        Icon(
                                            Icons.Default.Star,
                                            contentDescription = "Festivals and astrological calendar"
                                        )
                                    },
                                    label = { Text("Festivals", style = MaterialTheme.typography.labelSmall) },
                                    modifier = Modifier.testTag("tab_calendar")
                                )

                                NavigationBarItem(
                                    selected = activeTab == "chat",
                                    onClick = { viewModel.selectTab("chat") },
                                    icon = {
                                        Icon(
                                            Icons.Default.Info,
                                            contentDescription = "Mythology AI Chat assistant"
                                        )
                                    },
                                    label = { Text("AI Guide", style = MaterialTheme.typography.labelSmall) },
                                    modifier = Modifier.testTag("tab_chat")
                                )

                                NavigationBarItem(
                                    selected = activeTab == "sadhana",
                                    onClick = { viewModel.selectTab("sadhana") },
                                    icon = {
                                        Icon(
                                            Icons.Default.Favorite,
                                            contentDescription = "My Sadhana and Devotions"
                                        )
                                    },
                                    label = { Text("Sadhana", style = MaterialTheme.typography.labelSmall) },
                                    modifier = Modifier.testTag("tab_sadhana")
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(
                                top = innerPadding.calculateTopPadding(),
                                bottom = innerPadding.calculateBottomPadding()
                            )
                    ) {
                        when (activeTab) {
                                "gallery" -> GalleryScreen(
                                    viewModel = viewModel,
                                    modifier = Modifier.fillMaxSize()
                                )
                                "library" -> LibraryScreen(
                                    viewModel = viewModel,
                                    modifier = Modifier.fillMaxSize()
                                )
                                "calendar" -> CalendarScreen(
                                    viewModel = viewModel,
                                    modifier = Modifier.fillMaxSize()
                                )
                            "chat" -> ChatScreen(
                                viewModel = viewModel,
                                modifier = Modifier.fillMaxSize()
                            )
                            "sadhana" -> SadhanaScreen(
                                viewModel = viewModel,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        // Prominent but non-intrusive bottom-positioned consent banner
                        AdMobConsentBanner(
                            modifier = Modifier.align(Alignment.BottomCenter)
                        )
                    }
                }

                if (showSettingsDialog) {
                    LaunchedEffect(Unit) {
                        customAppId = AdMobManager.getAppId(context)
                        customBannerId = AdMobManager.getBannerAdUnitId(context)
                        customInterstitialId = AdMobManager.getInterstitialAdUnitId(context)
                        customLimitCount = AdMobManager.getLimitCount(context).toString()
                    }

                    AlertDialog(
                        onDismissRequest = { showSettingsDialog = false },
                        confirmButton = {
                            TextButton(
                                onClick = { showSettingsDialog = false },
                                modifier = Modifier.testTag("settings_close_btn")
                            ) {
                                Text("Close", style = MaterialTheme.typography.labelLarge)
                            }
                        },
                        title = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Settings",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "App Settings",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Serif
                                    )
                                )
                            }
                        },
                        text = {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text(
                                    text = "Aesthetics",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold
                                )

                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = if (isDarkTheme) "🌙" else "☀️",
                                                fontSize = 18.sp
                                            )
                                            Text(
                                                text = "Dark Interface Theme",
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        Switch(
                                            checked = isDarkTheme,
                                            onCheckedChange = { viewModel.toggleTheme() },
                                            modifier = Modifier.testTag("settings_theme_switch")
                                        )
                                    }
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                                Text(
                                    text = "Sacred Language Preference",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold
                                )

                                val selectedLanguage by viewModel.selectedLanguage.collectAsState()
                                var showLanguageSubSelector by remember { mutableStateOf(false) }

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { showLanguageSubSelector = true }
                                        .testTag("settings_language_card_trigger"),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    border = androidx.compose.foundation.BorderStroke(
                                        width = 1.dp,
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = "🕉️",
                                                fontSize = 18.sp
                                            )
                                            Column {
                                                Text(
                                                    text = "Current Language",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    text = selectedLanguage,
                                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                        Text(
                                            text = "Change ➔",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                if (showLanguageSubSelector) {
                                    AlertDialog(
                                        onDismissRequest = { showLanguageSubSelector = false },
                                        confirmButton = {
                                            TextButton(onClick = { showLanguageSubSelector = false }) {
                                                Text("Back")
                                            }
                                        },
                                        title = {
                                            Text(
                                                "Select Sacred Language",
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                            )
                                        },
                                        text = {
                                            val indianLanguages = listOf(
                                                "Assamese", "Bengali", "Bodo", "Dogri", "English", "Gujarati", "Hindi",
                                                "Kannada", "Kashmiri", "Konkani", "Maithili", "Malayalam", "Manipuri",
                                                "Marathi", "Nepali", "Odia", "Punjabi", "Sanskrit", "Santali", "Sindhi",
                                                "Tamil", "Telugu", "Urdu"
                                            )

                                            Box(modifier = Modifier.sizeIn(maxHeight = 350.dp)) {
                                                LazyVerticalGrid(
                                                    columns = GridCells.Fixed(2),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                                    modifier = Modifier.fillMaxWidth().testTag("indian_languages_grid")
                                                ) {
                                                    items(indianLanguages.size) { index ->
                                                        val lang = indianLanguages[index]
                                                        val isCurrent = lang.equals(selectedLanguage, ignoreCase = true)
                                                        Box(
                                                            modifier = Modifier
                                                                .clip(RoundedCornerShape(8.dp))
                                                                .background(
                                                                    if (isCurrent) MaterialTheme.colorScheme.primaryContainer
                                                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                                                )
                                                                .border(
                                                                    width = 1.dp,
                                                                    color = if (isCurrent) MaterialTheme.colorScheme.primary
                                                                            else MaterialTheme.colorScheme.outlineVariant,
                                                                    shape = RoundedCornerShape(8.dp)
                                                                )
                                                                .clickable {
                                                                    viewModel.changeLanguage(lang)
                                                                    showLanguageSubSelector = false
                                                                }
                                                                .padding(horizontal = 10.dp, vertical = 12.dp)
                                                                .testTag("settings_lang_select_$lang"),
                                                            contentAlignment = Alignment.CenterStart
                                                        ) {
                                                            Row(
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                            ) {
                                                                if (isCurrent) {
                                                                    Text("✓", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                                }
                                                                Text(
                                                                    text = lang,
                                                                    style = MaterialTheme.typography.bodyMedium.copy(
                                                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                                                                    ),
                                                                    color = if (isCurrent) MaterialTheme.colorScheme.onPrimaryContainer
                                                                            else MaterialTheme.colorScheme.onSurface
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    )
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                                Text(
                                    text = "Offline Local Cache Status",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold
                                )

                                Card(
                                    modifier = Modifier.fillMaxWidth().testTag("offline_cache_status_card"),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.15f)
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    border = androidx.compose.foundation.BorderStroke(
                                        width = 1.dp,
                                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f)
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = "Offline Vault Active",
                                                tint = MaterialTheme.colorScheme.secondary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Text(
                                                text = "Offline Storage Vault",
                                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Spacer(modifier = Modifier.weight(1f))
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "SECURED (SQLite Room)",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.secondary
                                                )
                                            }
                                        }

                                        Text(
                                            text = "All temple locations, rich puranic histories, deity profiles, and prayer chants are fully stored in the app's local SQLite database (via Room DB). No internet connection is required for browsing.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { adMobExpanded = !adMobExpanded }
                                        .testTag("admob_settings_toggle")
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "AdMob Advertising Setup",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (adMobExpanded) "▲ Hide" else "▼ Show Details",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                if (adMobExpanded) {
                                    Card(
                                        modifier = Modifier.fillMaxWidth().testTag("admob_override_card"),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        border = androidx.compose.foundation.BorderStroke(
                                            width = 1.dp,
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                                        )
                                    ) {
                                        Column(
                                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                                            verticalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Text(
                                                text = "Configure AdMob Credentials",
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = "You can customize AdMob Application and Unit IDs below. Safe Google advertiser test values are loaded by default.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )

                                            OutlinedTextField(
                                                value = customAppId,
                                                onValueChange = { customAppId = it },
                                                label = { Text("AdMob Application ID", fontSize = 10.sp) },
                                                singleLine = true,
                                                modifier = Modifier.fillMaxWidth().testTag("admob_app_id_input"),
                                                textStyle = MaterialTheme.typography.bodySmall
                                            )

                                            OutlinedTextField(
                                                value = customInterstitialId,
                                                onValueChange = { customInterstitialId = it },
                                                label = { Text("Interstitial Ad Unit ID", fontSize = 10.sp) },
                                                singleLine = true,
                                                modifier = Modifier.fillMaxWidth().testTag("admob_interstitial_id_input"),
                                                textStyle = MaterialTheme.typography.bodySmall
                                            )

                                            OutlinedTextField(
                                                value = customBannerId,
                                                onValueChange = { customBannerId = it },
                                                label = { Text("Inline Banner Ad Unit ID", fontSize = 10.sp) },
                                                singleLine = true,
                                                modifier = Modifier.fillMaxWidth().testTag("admob_banner_id_input"),
                                                textStyle = MaterialTheme.typography.bodySmall
                                            )

                                            OutlinedTextField(
                                                value = customLimitCount,
                                                onValueChange = { customLimitCount = it },
                                                label = { Text("Display Frequency Action Interval", fontSize = 10.sp) },
                                                singleLine = true,
                                                modifier = Modifier.fillMaxWidth().testTag("admob_limit_count_input"),
                                                textStyle = MaterialTheme.typography.bodySmall
                                            )

                                            Button(
                                                onClick = {
                                                    val parsedLimit = customLimitCount.toIntOrNull() ?: 3
                                                    AdMobManager.saveAdMobConfig(
                                                        context,
                                                        customAppId,
                                                        customBannerId,
                                                        customInterstitialId,
                                                        parsedLimit
                                                    )
                                                    android.widget.Toast.makeText(context, "Configurations stored successfully!", android.widget.Toast.LENGTH_SHORT).show()
                                                },
                                                modifier = Modifier.fillMaxWidth().testTag("admob_save_btn"),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = MaterialTheme.colorScheme.primary
                                                )
                                            ) {
                                                Text("Save AdMob Parameters", color = MaterialTheme.colorScheme.onPrimary)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    )
                }

                if (showDailyInsightDialog) {
                    DailySpiritualInsightDialog(
                        viewModel = viewModel,
                        onDismiss = { showDailyInsightDialog = false }
                    )
                }
            }
        }
    }
}

@Composable
fun PersistentAudioController(
    viewModel: DivineViewModel,
    modifier: Modifier = Modifier
) {
    val currentlyPlaying by viewModel.currentlyPlayingStotram.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val playbackPos by viewModel.playbackPosition.collectAsState()
    val elapsedStr by viewModel.playbackElapsed.collectAsState()
    val durationStr by viewModel.playbackDuration.collectAsState()

    currentlyPlaying?.let { stotram ->
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp)
                .testTag("persistent_audio_controller"),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🎵",
                            fontSize = 20.sp,
                            modifier = Modifier.padding(end = 10.dp)
                        )
                        Column {
                            Text(
                                text = stotram.type,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = stotram.title,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = { viewModel.togglePlayPause() },
                            modifier = Modifier.size(36.dp).testTag("persistent_play_pause_button")
                        ) {
                            Text(
                                text = if (isPlaying) "⏸" else "▶",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        IconButton(
                            onClick = {
                                viewModel.pauseStotram()
                                viewModel.seekPlayback(0f)
                            },
                            modifier = Modifier.size(36.dp).testTag("persistent_stop_button")
                        ) {
                            Text(
                                text = "⏹",
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = elapsedStr,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(36.dp)
                    )

                    Slider(
                        value = playbackPos,
                        onValueChange = { viewModel.seekPlayback(it) },
                        modifier = Modifier
                            .weight(1f)
                            .height(24.dp)
                            .testTag("persistent_audio_slider"),
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        )
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    Text(
                        text = durationStr,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(36.dp),
                        textAlign = TextAlign.End
                    )
                }
            }
        }
    }
}
