package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.StotramPuja
import com.example.ui.viewmodel.DivineViewModel
import com.example.data.util.AdMobManager

@Composable
fun LibraryScreen(
    viewModel: DivineViewModel,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf<Int?>(null) } // null means "All Deities"
    var selectedType by remember { mutableStateOf("ALL") } // "ALL", "STOTRAM", "AARTI", "MANTRA"

    // Collect pre-populated stotrams direct from active database flows
    val stotramsList by viewModel.allStotrams.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val selectedLanguage by viewModel.selectedLanguage.collectAsState()

    // Filtered list based on Search, Category filter, Type, and Language selection
    val filteredStotrams = remember(stotramsList, searchQuery, selectedCategoryId, selectedType, selectedLanguage) {
        stotramsList.filter { stotram ->
            val matchesSearch = searchQuery.isBlank() || 
                stotram.title.contains(searchQuery, ignoreCase = true) ||
                stotram.sanskritText.contains(searchQuery, ignoreCase = true) ||
                stotram.translation.contains(searchQuery, ignoreCase = true) ||
                stotram.benefits.contains(searchQuery, ignoreCase = true)

            val matchesCategory = selectedCategoryId == null || stotram.categoryId == selectedCategoryId
            val matchesType = selectedType == "ALL" || stotram.type.equals(selectedType, ignoreCase = true)
            val matchesLanguage = stotram.language.equals(selectedLanguage, ignoreCase = true)

            matchesSearch && matchesCategory && matchesType && matchesLanguage
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        // 1. Premium Screen Header
        item {
            Spacer(modifier = Modifier.height(20.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    MaterialTheme.colorScheme.surface
                                )
                            ),
                            shape = CircleShape
                        )
                        .border(
                            1.5.dp, 
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), 
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.List,
                        contentDescription = "Sacred Library icon",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "Sacred Library",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Explore hymns, chants & pujas with real-time text-to-speech recitation.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // AdMob Sacred Banner Advertisement Section
        item {
            FullBannerAdView()
            Spacer(modifier = Modifier.height(12.dp))
        }

        // 1.5. Premium Language Quick Switching Row
        item {
            val languages = remember(selectedLanguage) {
                val base = listOf("Sanskrit", "Hindi", "English", "Telugu")
                if (selectedLanguage !in base) base + selectedLanguage else base
            }
            val syncStatus by viewModel.syncStatus.collectAsState()
            val opMessage by viewModel.operationMessage.collectAsState()

            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                Text(
                    text = "Sacred Language Preference",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f).testTag("language_selector_row")
                    ) {
                        items(languages) { lang ->
                            val isSelected = lang.equals(selectedLanguage, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.outlineVariant,
                                        shape = RoundedCornerShape(20.dp)
                                    )
                                    .clickable { viewModel.changeLanguage(lang) }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                                    .testTag("language_chip_${lang.lowercase()}"),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = when (lang) {
                                            "Sanskrit" -> Icons.Default.Star
                                            "Hindi" -> Icons.Default.Home
                                            "English" -> Icons.Default.Info
                                            else -> Icons.Default.Favorite
                                        },
                                        contentDescription = "$lang Content",
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary
                                               else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = lang,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                                else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // Cloud pull shortcut button
                    IconButton(
                        onClick = { viewModel.changeLanguage(selectedLanguage) },
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape)
                            .size(36.dp)
                            .testTag("language_cloud_pull"),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Sync language from server",
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                if (opMessage != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = opMessage ?: "",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = when (syncStatus) {
                            "LOADING" -> MaterialTheme.colorScheme.primary
                            "SUCCESS" -> MaterialTheme.colorScheme.tertiary
                            else -> MaterialTheme.colorScheme.error
                        },
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // 2. Search Box
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("library_search_input"),
                placeholder = {
                    Text(
                        "Search titles, Sanskrit verses, translations...",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "SearchIcon",
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { searchQuery = "" },
                            modifier = Modifier.testTag("library_search_clear")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear search query"
                            )
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                )
            )
            Spacer(modifier = Modifier.height(14.dp))
        }

        // 3. Deity Category Scrollable Row
        item {
            Text(
                text = "Filter by Deity",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(end = 16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .testTag("deity_filter_row")
            ) {
                // "All Deities" option
                item {
                    val isSelected = selectedCategoryId == null
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategoryId = null },
                        label = { Text("All Deities") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }

                items(categories) { category ->
                    val isSelected = selectedCategoryId == category.id
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategoryId = category.id },
                        label = { Text(category.name) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        // 4. Type Filter Chips Row (STOTRAM, AARTI, MANTRA)
        item {
            Text(
                text = "Filter by Guide Type",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            val types = listOf("ALL", "STOTRAM", "AARTI", "MANTRA")
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("type_filter_row")
            ) {
                types.forEach { type ->
                    val isSelected = selectedType == type
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedType = type },
                        label = { Text(type) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // 5. Results Counter and Reset filters shortcut
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val resultsCount = filteredStotrams.count()
                Text(
                    text = "Showing $resultsCount Guides",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )

                if (selectedCategoryId != null || selectedType != "ALL" || searchQuery.isNotEmpty()) {
                    TextButton(
                        onClick = {
                            selectedCategoryId = null
                            selectedType = "ALL"
                            searchQuery = ""
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Clear", modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reset Filters", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // 6. Stotrams List
        if (filteredStotrams.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                        .testTag("empty_library_card"),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "No results icon",
                            tint = MaterialTheme.colorScheme.outlineVariant,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No devotional guidelines found",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "No devotional guidelines found for $selectedLanguage in offline storage. Refeed your local library from the active server feed:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 8.dp)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = { viewModel.changeLanguage(selectedLanguage) },
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.testTag("pull_stotrams_by_language_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Pull language from server",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Download $selectedLanguage Content", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        } else {
            items(items = filteredStotrams) { stotram ->
                val categoryName = categories.find { it.id == stotram.categoryId }?.name ?: "Divine Deva"
                val categoryColorStr = categories.find { it.id == stotram.categoryId }?.defaultColor ?: "#FFA726"
                val godColor = remember(categoryColorStr) {
                    runCatching { Color(android.graphics.Color.parseColor(categoryColorStr)) }
                        .getOrDefault(Color(0xFFFFA726))
                }

                LibraryStotramCard(
                    stotram = stotram,
                    categoryName = categoryName,
                    headerColor = godColor,
                    viewModel = viewModel
                )
                Spacer(modifier = Modifier.height(14.dp))
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun LibraryStotramCard(
    stotram: StotramPuja,
    categoryName: String,
    headerColor: Color,
    viewModel: DivineViewModel
) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }

    // Collect speech and playback states
    val currentlyPlayingSimulated by viewModel.currentlyPlayingStotram.collectAsState()
    val isPlayingSimulated by viewModel.isPlaying.collectAsState()
    
    val ttsActiveStotram by viewModel.ttsActiveStotram.collectAsState()
    val isTtsSpeaking by viewModel.isTtsSpeaking.collectAsState()
    val ttsSelectedType by viewModel.ttsSelectedType.collectAsState()

    val isThisSimulatedActive = currentlyPlayingSimulated?.id == stotram.id
    val isThisSimulatedPlaying = isThisSimulatedActive && isPlayingSimulated

    val isThisTtsActive = ttsActiveStotram?.id == stotram.id
    val isThisTtsSpeaking = isThisTtsActive && isTtsSpeaking

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("lib_stotram_card_${stotram.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (isThisSimulatedActive || isThisTtsActive) {
                headerColor.copy(alpha = 0.08f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
            }
        ),
        shape = RoundedCornerShape(16.dp),
        border = if (isThisSimulatedActive || isThisTtsActive) {
            borderStrokeGradient(headerColor)
        } else {
            null
        }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Chips and expand arrow
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Guide Type Chip
                    SuggestionChip(
                        onClick = { },
                        label = { Text(stotram.type, fontSize = 10.sp, fontWeight = FontWeight.Black) },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = headerColor.copy(alpha = 0.15f),
                            labelColor = headerColor
                        )
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    // Deity Connection Chip
                    SuggestionChip(
                        onClick = { },
                        label = { Text(categoryName, fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )

                    // Show active indicator if either simulated or TTS is buzzing inside this card context
                    if (isThisSimulatedPlaying || isThisTtsSpeaking) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isThisSimulatedPlaying) "🎵 Playing" else "🗣️ Reading",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 9.sp
                            ),
                            color = headerColor
                        )
                    }
                }

                IconButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.testTag("lib_expand_button_${stotram.id}")
                ) {
                    Icon(
                        imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Expand/Collapse card contents",
                        tint = headerColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Title
            Text(
                text = stotram.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif
                ),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.clickable { expanded = !expanded }
            )

            // Preview subtitle if collapsed
            if (!expanded) {
                Text(
                    text = stotram.translation,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                    Spacer(modifier = Modifier.height(10.dp))

                    // Dynamic Audio Chanting & Real TTS Dual Engine Command Panel
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "🔊 CHANT AUDIO & VOICEOVER CONTROLS",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = headerColor
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            // TTS voice pack settings (Sanskrit, Translation, Benefits)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Speak:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                val speechModes = listOf("Sanskrit" to "sanskrit", "Translation" to "translation", "Benefits" to "benefits")
                                speechModes.forEach { (label, value) ->
                                    val isCurrentMode = isThisTtsActive && ttsSelectedType == value
                                    AssistChip(
                                        onClick = {
                                            viewModel.setTtsSelectedType(value)
                                            if (!isThisTtsActive) {
                                                viewModel.speakStotram(stotram)
                                            }
                                        },
                                        label = { Text(label, fontSize = 9.sp) },
                                        colors = AssistChipDefaults.assistChipColors(
                                            containerColor = if (isCurrentMode) headerColor.copy(alpha = 0.15f) else Color.Transparent,
                                            labelColor = if (isCurrentMode) headerColor else MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        border = androidx.compose.foundation.BorderStroke(
                                            width = 1.dp,
                                            color = if (isCurrentMode) headerColor else MaterialTheme.colorScheme.outlineVariant
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Action buttons row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Real Text-To-Speech Button
                                Button(
                                    onClick = {
                                        if (isThisTtsSpeaking) {
                                            viewModel.stopTts()
                                        } else {
                                            val activity = context as? android.app.Activity
                                            if (activity != null) {
                                                AdMobManager.showInterstitialAtCriticalSection(activity)
                                            }
                                            viewModel.speakStotram(stotram)
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1.1f)
                                        .height(38.dp)
                                        .testTag("lib_tts_btn_${stotram.id}"),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isThisTtsSpeaking) MaterialTheme.colorScheme.error else headerColor
                                    ),
                                    contentPadding = PaddingValues(horizontal = 4.dp)
                                ) {
                                    Text(
                                        text = if (isThisTtsSpeaking) "🔇" else "🗣️",
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isThisTtsSpeaking) "Stop TTS" else "Read Aloud (TTS)",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                // Simulated Chanting Player Button
                                OutlinedButton(
                                    onClick = {
                                        if (isThisSimulatedPlaying) {
                                            viewModel.pauseStotram()
                                        } else {
                                            val activity = context as? android.app.Activity
                                            if (activity != null) {
                                                AdMobManager.showInterstitialAtCriticalSection(activity)
                                            }
                                            viewModel.playStotram(stotram)
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(0.9f)
                                        .height(38.dp)
                                        .testTag("lib_chant_btn_${stotram.id}"),
                                    border = borderStrokeGradient(headerColor),
                                    contentPadding = PaddingValues(horizontal = 4.dp)
                                ) {
                                    Text(
                                        text = if (isThisSimulatedPlaying) "⏸" else "▶",
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isThisSimulatedPlaying) "Pause Chant" else "Listen Chant",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = headerColor
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Sanskrit Hymn Devanagari text display
                    Text(
                        text = "Devanagari Sanskrit Verse:",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                        color = headerColor
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stotram.sanskritText,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontFamily = FontFamily.Serif,
                            lineHeight = 22.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        ),
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(headerColor.copy(alpha = 0.05f), RoundedCornerShape(10.dp))
                            .border(1.dp, headerColor.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                            .padding(14.dp)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // English/Hindi Word Translation
                    Text(
                        text = "Sacred Interpretation / Translation:",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stotram.translation,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Chanting Benefits
                    Text(
                        text = "Spiritual Rewards & Benefits:",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF4CAF50)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stotram.benefits,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f)
                    )
                }
            }
        }
    }
}

// Border Stroke Helper for Saffron Sensation Card Accent
fun borderStrokeGradient(color: Color) = androidx.compose.foundation.BorderStroke(
    width = 1.dp,
    brush = Brush.linearGradient(
        colors = listOf(
            color,
            color.copy(alpha = 0.1f)
        )
    )
)
