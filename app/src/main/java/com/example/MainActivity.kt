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
import com.example.ui.screens.SadhanaScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.DivineViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

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
                            TextButton(
                                onClick = { viewModel.toggleTheme() },
                                modifier = Modifier.testTag("theme_toggle_button")
                            ) {
                                Text(
                                    text = if (isDarkTheme) "🌙 Dark" else "☀️ Light",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                )
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
                    }
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
