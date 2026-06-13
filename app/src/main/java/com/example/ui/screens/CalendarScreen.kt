package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.Festival
import com.example.ui.viewmodel.DivineViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CalendarScreen(
    viewModel: DivineViewModel,
    modifier: Modifier = Modifier
) {
    val festivals by viewModel.festivals.collectAsState()
    val panchang = remember { viewModel.getPanchangDetailsForToday() }
    var searchQuery by remember { mutableStateOf("") }
    val notificationStatesByFestival = remember { mutableStateMapOf<Int, Boolean>() }
    
    val todayStr = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    val todayReadable = remember {
        try {
            SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault()).format(Date())
        } catch (e: Exception) {
            "Today"
        }
    }

    val tomorrowStr = remember {
        val calendar = java.util.Calendar.getInstance()
        calendar.add(java.util.Calendar.DAY_OF_YEAR, 1)
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
    }

    // Identify festivals occurring within the next 24 hours (Today or Tomorrow)
    val next24HoursFestivals = remember(festivals, todayStr, tomorrowStr) {
        festivals.filter { it.dateStr == todayStr || it.dateStr == tomorrowStr }
    }

    var bannerDismissed by remember { mutableStateOf(false) }
    var showToastMessage by remember { mutableStateOf<String?>(null) }

    // Trigger floating toast when any active 24h event is present
    LaunchedEffect(next24HoursFestivals) {
        if (next24HoursFestivals.isNotEmpty()) {
            val names = next24HoursFestivals.joinToString(", ") { it.title }
            showToastMessage = "📯 Sacred Festival occurs within 24 hours: $names!"
        }
    }

    // Sort to ensure chronological display
    val sortedFestivals = remember(festivals) {
        festivals.sortedBy { it.dateStr }
    }

    // Filter by searchQuery
    val filteredFestivals = remember(sortedFestivals, searchQuery) {
        if (searchQuery.isBlank()) {
            sortedFestivals
        } else {
            sortedFestivals.filter {
                it.title.contains(searchQuery, ignoreCase = true) || 
                it.description.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    // Categorize into Today, Upcoming, and Past lists
    val todayFestivals = remember(filteredFestivals, todayStr) {
        filteredFestivals.filter { it.dateStr == todayStr }
    }

    val upcomingFestivals = remember(filteredFestivals, todayStr) {
        filteredFestivals.filter { it.dateStr > todayStr }
    }

    val pastFestivals = remember(filteredFestivals, todayStr) {
        filteredFestivals.filter { it.dateStr < todayStr }
    }

    var pastSectionExpanded by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(20.dp))

                // Premium Screen Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DateRange, // Core icon replacement for CalendarToday
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Vedic Calendar",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Serif
                            ),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Spiritual timeline & cosmic Panchang guides",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Visual Mini Alert Badge if banner was dismissed
                if (next24HoursFestivals.isNotEmpty() && bannerDismissed) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable { bannerDismissed = false }
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .testTag("restore_24h_banner_badge")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Color(0xFFFF5252), CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "🔔 ${next24HoursFestivals.size} Divine Alert Active! Tap to restore banner.",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

            // PANCHANG INFORMATION DRAWER
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("today_panchang_card"),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                ),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Today's Astrological Coordinates",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = panchang["date"] ?: "",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = panchang["samvat"] ?: "",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1.2f)) {
                            PanchangRowItem(
                                label = "Tithi (Lunar Phase)",
                                value = panchang["tithi"] ?: "",
                                icon = Icons.Default.Info // Core icon replacement for Brightness4
                            )
                            PanchangRowItem(
                                label = "Nakshatra (Astro Star)",
                                value = panchang["nakshatra"] ?: "",
                                icon = Icons.Default.Star
                            )
                            PanchangRowItem(
                                label = "Ayana (Solar Course)",
                                value = panchang["ayana"] ?: "",
                                icon = Icons.Default.Star // Core icon replacement for WbSunny
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            PanchangRowItem(
                                label = "Rahu Kaal (Avoid Actions)",
                                value = panchang["rahu_kaal"] ?: "",
                                icon = Icons.Default.Warning, // Core icon replacement for Cancel
                                isWarning = true
                            )
                            PanchangRowItem(
                                label = "Abhijit (Spiritual Era)",
                                value = panchang["abhijit"] ?: "",
                                icon = Icons.Default.Done, // Core icon replacement for CheckCircle
                                isSuccess = true
                            )
                        }
                    }
                }
            }

            // ================= NEXT 24 HOURS DIVINE ALERTS BANNER =================
            if (next24HoursFestivals.isNotEmpty() && !bannerDismissed && searchQuery.isEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize()
                        .testTag("next_24h_alert_banner"),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.5.dp,
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFFFF6D00), // Lord Rama Sunset Orange
                                Color(0xFFFFA726)  // Sacred Saffron Orange
                            )
                        )
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Pulsing red dot indicator
                                val infiniteTransition = rememberInfiniteTransition(label = "banner_badge_pulse")
                                val scale by infiniteTransition.animateFloat(
                                    initialValue = 0.85f,
                                    targetValue = 1.15f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(800, easing = LinearEasing),
                                        repeatMode = RepeatMode.Reverse
                                    ),
                                    label = "scale"
                                )
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .scale(scale)
                                        .background(Color(0xFFFF3D00), CircleShape)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "LIVE SACRED ALERT (NEXT 24 HOURS)",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 1.sp
                                    ),
                                    color = Color(0xFFFF6D00)
                                )
                            }
                            IconButton(
                                onClick = { bannerDismissed = true },
                                modifier = Modifier
                                    .size(24.dp)
                                    .testTag("dismiss_next_24h_banner")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss Banner",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        next24HoursFestivals.forEachIndexed { index, festival ->
                            if (index > 0) {
                                Spacer(modifier = Modifier.height(12.dp))
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                Spacer(modifier = Modifier.height(12.dp))
                            }

                            val themeColor = getCategoryColor(festival.deityCategoryId)
                            val isToday = festival.dateStr == todayStr

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .background(themeColor.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "🕭",
                                        fontSize = 18.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = festival.title,
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Serif
                                            ),
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Card(
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (isToday) Color(0xFFFFEBEE) else Color(0xFFFFF3E0)
                                            ),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = if (isToday) "TODAY" else "TOMORROW",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Black,
                                                    fontSize = 8.sp
                                                ),
                                                color = if (isToday) Color(0xFFC62828) else Color(0xFFEF6C00),
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Tithi: ${festival.tithi} | Nakshatra: ${festival.nakshatra}",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = festival.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        lineHeight = 15.sp
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "✨ Sacred Sadhana Power: ${festival.rituals}",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.Medium
                                        ),
                                        color = themeColor,
                                        lineHeight = 15.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = {
                                    next24HoursFestivals.forEach { festival ->
                                        viewModel.dispatchLocalNotification(
                                            "24h Devotional Vigil: ${festival.title}",
                                            "Sacred window begins! Chant mantras, perform pujas, or observe spiritual fast for Lord ${getCategoryName(festival.deityCategoryId)}."
                                        )
                                    }
                                },
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = Color(0xFFFF6D00)
                                ),
                                modifier = Modifier.testTag("banner_set_ritual_notification")
                            ) {
                                Text("🔔 Dispatch Active Devotion Vigil Alert", fontSize = 11.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // SEARCH BAR
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search sacred festivals of deities...", style = MaterialTheme.typography.bodyMedium) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("festival_search"),
                shape = RoundedCornerShape(14.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))
        }

        // ================= SECTION 1: TODAY'S HIGHLIGHTED EVENT =================
        if (searchQuery.isEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Happening Today",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    
                    // Explicit Today Indicator calculated on app initialization
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = todayReadable,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                if (todayFestivals.isEmpty()) {
                    // No festival today placeholder
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Today is a pristine day for introspective chanting and regular Sadhana prayers.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    // Render Today's highly prominent pulsing cards
                    todayFestivals.forEach { festival ->
                        val isNotified = notificationStatesByFestival[festival.id] ?: false
                        LiveFestivalTodayCard(
                            festival = festival,
                            isNotified = isNotified,
                            onNotificationToggle = {
                                val nextState = !isNotified
                                notificationStatesByFestival[festival.id] = nextState
                                if (nextState) {
                                    viewModel.dispatchLocalNotification(
                                        "Live Reminder Active: ${festival.title}",
                                        "Observe sacred procedures today for ${festival.title}! Open Sadhana logs."
                                    )
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }

        // ================= SECTION 2: UPCOMING FESTIVALS TIMELINE =================
        item {
            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Upcoming Maha Festivals",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        if (upcomingFestivals.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No future festivals found matching criteria.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(upcomingFestivals) { festival ->
                val isNotified = notificationStatesByFestival[festival.id] ?: false
                ChronologicalFestivalCard(
                    festival = festival,
                    isNotified = isNotified,
                    isToday = false,
                    onNotificationToggle = {
                        val nextState = !isNotified
                        notificationStatesByFestival[festival.id] = nextState
                        if (nextState) {
                            viewModel.dispatchLocalNotification(
                                "Scheduled Alert: ${festival.title}",
                                "We will send instructions on ${festival.dateStr} for celebrating ${festival.title}."
                            )
                        }
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        // ================= SECTION 3: COMPACT HISTORICAL/PAST FESTIVALS =================
        if (pastFestivals.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { pastSectionExpanded = !pastSectionExpanded }
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.List, // Core icon replacement for History
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Concluded Festivals (${pastFestivals.size})",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Icon(
                                imageVector = if (pastSectionExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        AnimatedVisibility(visible = pastSectionExpanded) {
                            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                                pastFestivals.forEach { festival ->
                                    val isNotified = notificationStatesByFestival[festival.id] ?: false
                                    ChronologicalFestivalCard(
                                        festival = festival,
                                        isNotified = isNotified,
                                        isPast = true,
                                        onNotificationToggle = {}
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

        // FLOATING DYNAMIC CUSTOM TOAST ALERT AT THE BOTTOM
        AnimatedVisibility(
            visible = showToastMessage != null,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = tween(500, easing = FastOutSlowInEasing)
            ) + fadeIn(),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(300)
            ) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
                .padding(horizontal = 24.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF323232) // Classic dark toast background
                ),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("twenty_four_hour_toast_alert")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "📯",
                            fontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = showToastMessage ?: "",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            ),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        onClick = { showToastMessage = null },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFFFB74D))
                    ) {
                        Text("Dismiss", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black))
                    }
                }
            }
        }
    }
}

@Composable
fun PanchangRowItem(
    label: String,
    value: String,
    icon: ImageVector,
    isWarning: Boolean = false,
    isSuccess: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(
                    color = when {
                        isWarning -> MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                        isSuccess -> Color(0xFF4CAF50).copy(alpha = 0.1f)
                        else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                    },
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = when {
                    isWarning -> MaterialTheme.colorScheme.error
                    isSuccess -> Color(0xFF388E3C)
                    else -> MaterialTheme.colorScheme.primary
                },
                modifier = Modifier.size(14.dp)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                fontSize = 9.sp
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun LiveFestivalTodayCard(
    festival: Festival,
    isNotified: Boolean,
    onNotificationToggle: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    val themeColor = remember(festival.deityCategoryId) { getCategoryColor(festival.deityCategoryId) }
    
    // Pulse animation for the Live indicator
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_live")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutBack),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_live_scale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded }
            .testTag("festival_item_${festival.id}")
            .border(
                width = 2.dp,
                brush = Brush.linearGradient(
                    colors = listOf(themeColor, Color(0xFFFFD700), themeColor)
                ),
                shape = RoundedCornerShape(16.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = Color.Black.copy(alpha = 0.9f) // Dynamic atmospheric deep wash
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Live Badge + Category Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Pulse badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .scale(pulseScale)
                        .background(Color(0xFFE53935), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(Color.White, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "LIVE TODAY",
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        fontSize = 8.sp,
                        letterSpacing = 0.5.sp
                    )
                }

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = themeColor.copy(alpha = 0.2f),
                        contentColor = themeColor
                    ),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = getCategoryName(festival.deityCategoryId).uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        fontSize = 8.sp,
                        letterSpacing = 0.8.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Main Title & Astronomical Metadata
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = festival.title,
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${festival.Month}  •  ${festival.tithi}",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.LightGray.copy(alpha = 0.85f)
                    )
                }
                
                IconButton(
                    onClick = {
                        onNotificationToggle()
                    },
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.15f), CircleShape)
                        .size(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications, // Core icon replacement for NotificationsActive
                        contentDescription = "Alert on update",
                        tint = if (isNotified) Color(0xFFFFD700) else Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Star, // Core icon replacement for Brightness5
                    contentDescription = null,
                    tint = Color(0xFFFFD700),
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Auspicious Star: ${festival.nakshatra}",
                    color = Color.LightGray.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp
                )
            }

            // Description and Rituals Area
            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    Divider(color = Color.White.copy(alpha = 0.15f))
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "DIVINE SIGNIFICANCE",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFFFFD700)
                    )
                    Text(
                        text = festival.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.85f),
                        lineHeight = 14.sp,
                        modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                    )

                    Text(
                        text = "VEDIC UPASANA & MANDU (RITUALS)",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFFFFD700)
                    )
                    Row(
                        modifier = Modifier.padding(top = 4.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star, // Core icon replacement for OfflineBolt
                            contentDescription = null,
                            tint = themeColor,
                            modifier = Modifier
                                .size(14.dp)
                                .padding(top = 2.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = festival.rituals,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.9f),
                            lineHeight = 14.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = Color.LightGray.copy(alpha = 0.5f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun ChronologicalFestivalCard(
    festival: Festival,
    isNotified: Boolean,
    isToday: Boolean = false,
    isPast: Boolean = false,
    onNotificationToggle: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    val themeColor = remember(festival.deityCategoryId) { getCategoryColor(festival.deityCategoryId) }
    val dateParts = remember(festival.dateStr) { parseFestivalDate(festival.dateStr) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded }
            .testTag("festival_item_${festival.id}"),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isPast -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.12f)
                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            }
        ),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isExpanded) 1.dp else 0.5.dp,
            color = if (isExpanded) themeColor.copy(alpha = 0.7f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // LEFT SIDE: TICKET STYLE DATE ACCENT HOUSING
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .background(
                            color = if (isPast) {
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                            } else {
                                themeColor.copy(alpha = 0.12f)
                            },
                            shape = RoundedCornerShape(10.dp)
                        )
                        .border(
                            width = 0.5.dp,
                            color = if (isPast) Color.Gray.copy(alpha = 0.2f) else themeColor.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(10.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = dateParts.first,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (isPast) MaterialTheme.colorScheme.outline else themeColor,
                            lineHeight = 16.sp
                        )
                        Text(
                            text = dateParts.second,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (isPast) MaterialTheme.colorScheme.outline.copy(alpha = 0.7f) else themeColor.copy(alpha = 0.75f),
                            fontSize = 8.sp,
                            lineHeight = 8.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // CENTER: MAIN INFO
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = festival.title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = FontFamily.Serif
                            ),
                            color = if (isPast) {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        
                        // Active reminder indicator small dot
                        if (isNotified && !isPast) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(themeColor, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }

                        // Countdown / Upcoming Days Left indicator
                        if (!isPast && !isToday) {
                            val daysLeft = getDaysRemaining(festival.dateStr)
                            if (daysLeft != null && daysLeft > 0) {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = themeColor.copy(alpha = 0.15f)
                                    ),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = if (daysLeft == 1) "TOMORROW" else "IN $daysLeft DAYS",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 8.sp,
                                            letterSpacing = 0.4.sp
                                        ),
                                        color = themeColor,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                    Text(
                        text = "${festival.Month} • ${festival.tithi}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                            alpha = if (isPast) 0.5f else 0.8f
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // RIGHT SIDE: ACTION REMINDER / STATUS
                if (!isPast) {
                    IconButton(
                        onClick = {
                            onNotificationToggle()
                        },
                        modifier = Modifier
                            .testTag("remind_bell_${festival.id}")
                            .size(44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications, // Core icon replacement for NotificationsActive
                            contentDescription = "Set Daily notification Reminder",
                            tint = if (isNotified) themeColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .scale(0.85f)
                            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.08f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "PAST",
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            fontSize = 7.sp
                        )
                    }
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Sacred Narrative & Origins",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = themeColor
                    )
                    Text(
                        text = festival.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 14.sp,
                        modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
                    )

                    Text(
                        text = "Pujas, Fasting & Custom Rituals",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = themeColor
                    )
                    Text(
                        text = festival.rituals,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 14.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}

// Helper color pick for aesthetic categorizations
fun getCategoryColor(deityId: Int): Color {
    return when (deityId) {
        1 -> Color(0xFFE65100) // Saffron Orange for Ganesha
        2 -> Color(0xFF00B0FF) // Shiva Blue / Gold
        3 -> Color(0xFF6200EA) // Royal Deep Blue-Violet for Krishna
        4 -> Color(0xFFD81B60) // Shakti Pink / Red
        5 -> Color(0xFFFF6D00) // Lord Rama Sunset Orange
        else -> Color(0xFFF57C00) // Vedic Gold Accent
    }
}

fun getCategoryName(deityId: Int): String {
    return when (deityId) {
        1 -> "Ganesha"
        2 -> "Shiva"
        3 -> "Krishna"
        4 -> "Shakti"
        5 -> "Rama"
        else -> "Vedic"
    }
}

// Parses "YYYY-MM-DD" safely into Pair("DD", "MMM")
fun parseFestivalDate(dateStr: String): Pair<String, String> {
    return try {
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateStr)
        if (date != null) {
            val day = SimpleDateFormat("dd", Locale.getDefault()).format(date)
            val month = SimpleDateFormat("MMM", Locale.getDefault()).format(date).uppercase()
            Pair(day, month)
        } else {
            Pair("??", "???")
        }
    } catch (e: Exception) {
        Pair("??", "???")
    }
}

fun getDaysRemaining(targetDateStr: String): Int? {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val today = sdf.parse(sdf.format(Date()))
        val target = sdf.parse(targetDateStr)
        if (today != null && target != null) {
            val diff = target.time - today.time
            (diff / (1000 * 60 * 60 * 24)).toInt()
        } else {
            null
        }
    } catch (e: Exception) {
        null
    }
}
