package com.example.ui.screens

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.database.GodImage
import com.example.ui.viewmodel.DivineViewModel
import com.example.data.util.AdMobManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SadhanaScreen(
    viewModel: DivineViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val favorites by viewModel.favorites.collectAsState()
    val serverUrl by viewModel.serverUrl.collectAsState()
    val syncStatus by viewModel.syncStatus.collectAsState()
    val operationMessage by viewModel.operationMessage.collectAsState()

    val sharedPrefs = remember { context.getSharedPreferences("devi_devata_prefs", Context.MODE_PRIVATE) }
    var activeModel by remember { mutableStateOf(sharedPrefs.getString("active_model", "gemini-3.5-flash") ?: "gemini-3.5-flash") }
    var llmOverrideUrl by remember { mutableStateOf(sharedPrefs.getString("llm_override_url", "") ?: "") }

    // Synchronize locally stored settings as they update from server sync
    LaunchedEffect(syncStatus) {
        if (syncStatus == "SUCCESS") {
            activeModel = sharedPrefs.getString("active_model", "gemini-3.5-flash") ?: "gemini-3.5-flash"
            llmOverrideUrl = sharedPrefs.getString("llm_override_url", "") ?: ""
        }
    }

    var selectedFullscreenImage by remember { mutableStateOf<GodImage?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Large Premium Title
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "My Sadhana",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Your daily spiritual companion, mantra Japa, and local divine logs.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // SECTION 1: INTERACTIVE JAPA MALA BEAD COUNTER
        JapaMalaCounter()

        Spacer(modifier = Modifier.height(20.dp))

        // SECTION 2: SAVED DEVOTIONAL WALLPAPERS
        Text(
            text = "My Sacred Gallery",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Serif
            ),
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Your favorited divine wallpapers. Tapping on any god wallpaper displays a glorious high-res preview.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 10.dp)
        )

        if (favorites.isEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .testTag("empty_sadhanan_gallery"),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.FavoriteBorder,
                        contentDescription = "No favorites",
                        tint = MaterialTheme.colorScheme.outlineVariant,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Realms of favorites are currently empty.",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Explore categories on the Gallery. Press and hold down on any high-resolution wallpaper to favorite it for daily dhyana (meditation).",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .testTag("favorites_row")
            ) {
                items(favorites) { image ->
                    Card(
                        modifier = Modifier
                            .width(130.dp)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { selectedFullscreenImage = image }
                            .testTag("favorite_card_${image.id}"),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            AsyncImage(
                                model = image.thumbUrl.ifEmpty { image.url },
                                contentDescription = image.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                                        )
                                    )
                            )
                            Text(
                                text = image.title,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                ),
                                maxLines = 1,
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(8.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // SECTION 3: SPIRITUAL ALERT TIMERS & REMINDRS (ACTIVE NOTIFICATIONS)
        Text(
            text = "Reminders & Alerts",
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Serif
            ),
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Activate daily sandhya or subha-vela reminders dynamically.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 10.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
                .testTag("reminders_card"),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Auspicious Sandhyakaal Notification",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Enables a local push notification designed to cue you during twilight times to transition offline, practice evening prayers, or perform mantra chanting.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Button(
                    onClick = {
                        viewModel.dispatchLocalNotification(
                            "Evening Sandhyakaal Beginning",
                            "Pradosh-vela timing: Settle your mind, chant Om Namah Shivaya or practice mental Dhyana."
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sandhya_alert_trigger_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Icon(Icons.Default.Notifications, contentDescription = "Reminder", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Trigger Sandhya Notification", style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        // Inline Full Banner / Rectangle Advertisement inside scrollable area
        FullBannerAdView()
        Spacer(modifier = Modifier.height(24.dp))
    }

    // Full-screen Premium Wallpaper Dialog
    selectedFullscreenImage?.let { godImage ->
        Dialog(
            onDismissRequest = { selectedFullscreenImage = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                AsyncImage(
                    model = godImage.url,
                    contentDescription = godImage.title,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )

                // Top Panel row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 24.dp)
                        .statusBarsPadding()
                        .align(Alignment.TopCenter),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { selectedFullscreenImage = null },
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                            .testTag("fullscreen_close")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close Detail Screen", tint = Color.White)
                    }

                    Text(
                        text = godImage.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }

                // Bottom Panel row
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f))
                            )
                        )
                        .padding(horizontal = 16.dp, vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = godImage.description.ifEmpty { "High-Resolution divine wallpaper details cached locally." },
                        color = Color.LightGray,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.setWallpaper(godImage.url, 0) // Home
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("fullscreen_set_home"),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Set Home Screen")
                        }
                        Button(
                            onClick = {
                                viewModel.setWallpaper(godImage.url, 1) // Lock
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("fullscreen_set_lock"),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Text("Set Lock Screen")
                        }
                    }
                }
            }
        }
    }
}

// Auspicious Japa Mala Counter Sub-Component
@Composable
fun JapaMalaCounter() {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var count by remember { mutableStateOf(0) }
    var totalMalas by remember { mutableStateOf(0) }
    var activeMantraIndex by remember { mutableStateOf(0) }

    val mantras = listOf(
        "Om Namah Shivaya",
        "Om Namo Narayanaya",
        "Om Dum Durgayei Namaha",
        "Om Gam Ganapataye Namaha",
        "Hare Krishna Hare Rama"
    )

    // Breathing effect for counter circle glow
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_counter")
    val pulseGlowRadius by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 28f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    // Touch scale animation on bead tap
    var isTapped by remember { mutableStateOf(false) }
    val tapScale by animateFloatAsState(
        targetValue = if (isTapped) 0.92f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "bead_tap_scale"
    )

    // Sparkle overlay triggering upon mala completion (108)
    var showLotusCelebration by remember { mutableStateOf(false) }

    val activeMantra = mantras[activeMantraIndex]

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("japa_mala_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "🌸 Sacred Mantra Japa Counter",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif
                ),
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Tap on the golden mandala bead to chant. Practice mindfulness. Senses tactile vibrational haptic pulse on completing one holy Mala cycle (108 beads).",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 4.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Mantra selector dropdown
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                    .clickable {
                        activeMantraIndex = (activeMantraIndex + 1) % mantras.size
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Active Divine Mantra",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = activeMantra,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Switch mantra to chant",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Sacred Counter Bead
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(200.dp)
                    .scale(tapScale)
                    .clickable {
                        scope.launch {
                            isTapped = true
                            // Increment chant count
                            count += 1
                            // Feel small vibration feedback
                            try {
                                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    vibrator.vibrate(VibrationEffect.createOneShot(35L, VibrationEffect.DEFAULT_AMPLITUDE))
                                } else {
                                    @Suppress("DEPRECATION")
                                    vibrator.vibrate(35L)
                                }
                            } catch (e: Exception) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }

                            if (count >= 108) {
                                count = 0
                                totalMalas += 1
                                showLotusCelebration = true

                                // Auspicious rate-limited Interstitial Ad triggers on completing a holy Mala
                                val activity = context as? android.app.Activity
                                if (activity != null) {
                                    AdMobManager.showInterstitialAtCriticalSection(activity)
                                }

                                // Heavy completed Mala vibration flow
                                try {
                                    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                        vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 100, 80, 200, 80, 300), -1))
                                    } else {
                                        @Suppress("DEPRECATION")
                                        vibrator.vibrate(300L)
                                    }
                                } catch (e: Exception) {}
                                delay(4000L)
                                showLotusCelebration = false
                            }
                            delay(80L)
                            isTapped = false
                        }
                    }
                    .testTag("japa_counter_bead")
            ) {
                // Glow Background
                val primaryColor = MaterialTheme.colorScheme.primary
                val goldColor = Color(0xFFFFB300)
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        color = goldColor.copy(alpha = 0.12f),
                        radius = size.width / 2f + pulseGlowRadius,
                        center = center
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(primaryColor.copy(alpha = 0.3f), Color.Transparent),
                            center = center,
                            radius = size.width / 1.6f
                        ),
                        radius = size.width / 1.8f,
                        center = center
                    )
                    // Golden sacred border
                    drawCircle(
                        color = goldColor,
                        radius = size.width / 2.2f,
                        center = center,
                        style = Stroke(width = 3.dp.toPx())
                    )
                }

                // Inner sacred text and counts
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "JAPA BEADS",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "$count",
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Serif
                        ),
                        color = goldColor,
                        modifier = Modifier.offset(y = (-4).dp)
                    )
                    Text(
                        text = "of 108",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Completed celebration particle floating text overlay
                if (showLotusCelebration) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.6f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Star, contentDescription = "Blessed celebration icon", tint = goldColor, modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "MALA COMPLETE!",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            Text(
                                "108 Chants Blessed",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.LightGray
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Mala Rounds Done",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$totalMalas",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.testTag("mala_rounds_completed")
                    )
                }

                Button(
                    onClick = {
                        count = 0
                        totalMalas = 0
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.testTag("reset_japa_button")
                ) {
                    Text("Reset Japa", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}

@Composable
fun BoxBorder(color: Color) = androidx.compose.foundation.BorderStroke(1.dp, color)
