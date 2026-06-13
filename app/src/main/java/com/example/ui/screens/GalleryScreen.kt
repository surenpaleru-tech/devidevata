package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.shadow
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.database.GodCategory
import com.example.data.database.GodImage
import com.example.data.database.StotramPuja
import com.example.data.database.TempleInfo
import com.example.ui.viewmodel.DivineViewModel

@Composable
fun GalleryScreen(
    viewModel: DivineViewModel,
    modifier: Modifier = Modifier
) {
    val categories by viewModel.categories.collectAsState()
    val images by viewModel.images.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val selectedImage by viewModel.selectedImage.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    Box(modifier = modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = Triple(selectedCategory, selectedImage, categories),
            transitionSpec = {
                fadeIn() togetherWith fadeOut()
            },
            label = "gallery_transition"
        ) { (category, image, catList) ->
            when {
                image != null -> {
                    val highlightColor = category?.defaultColor?.let { runCatching { Color(android.graphics.Color.parseColor(it)) }.getOrNull() } ?: MaterialTheme.colorScheme.primary
                    ImageDetailSheet(
                        image = image,
                        onBack = { viewModel.selectImage(null) },
                        onFavoriteToggle = { viewModel.toggleFavorite(image) },
                        onDownload = { viewModel.downloadImageToDevice(image) },
                        onSetWallpaper = { imageUrl, type -> viewModel.setWallpaper(imageUrl, type) },
                        isOperationLoading = viewModel.isOperationLoading.collectAsState().value,
                        operationMessage = viewModel.operationMessage.collectAsState().value,
                        clearMessage = { viewModel.clearOperationMessage() },
                        themeHexColor = highlightColor
                    )
                }
                category != null -> {
                    GodDetailView(
                        category = category,
                        images = images,
                        viewModel = viewModel,
                        onBack = { viewModel.selectCategory(null) }
                    )
                }
                else -> {
                    // Normal grid of God Categories
                    CategoryListView(
                        categories = catList,
                        viewModel = viewModel,
                        onCategorySelect = { viewModel.selectCategory(it) },
                        searchQuery = searchQuery,
                        onSearchChange = { viewModel.updateSearchQuery(it) }
                    )
                }
            }
        }
    }
}

@Composable
fun CategoryListView(
    categories: List<GodCategory>,
    viewModel: DivineViewModel,
    onCategorySelect: (GodCategory) -> Unit,
    searchQuery: String,
    onSearchChange: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Large Premium Title
        Text(
            text = "Divine Categories",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Serif
            ),
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Select any supreme deity to explore high-res offline-cached wallpapers, sacred stotrams, daily pujas and historic temple info.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 4.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Spiritual insights now live in the floating screen accessible from Top App Bar!

        // Compact Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text("Search sacred scriptures, gods, temples...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search icon") },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchChange("") }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear inquiry")
                    }
                }
            },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("gallery_search_bar"),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (categories.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = "Empty",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(52.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Initializing Divine Records...",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        "Synchronizing local databases. Please wait.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            val filteredList = categories.filter {
                it.name.contains(searchQuery, ignoreCase = true) || it.description.contains(searchQuery, ignoreCase = true)
            }

            val configuration = LocalConfiguration.current
            val columnCount = when {
                configuration.screenWidthDp >= 900 -> 4
                configuration.screenWidthDp >= 600 -> 3
                else -> 2
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(columnCount),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 16.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("categories_grid")
            ) {
                items(filteredList) { category ->
                    GodCategoryGridCard(
                        category = category,
                        onClick = { onCategorySelect(category) }
                    )
                }
            }

            // High-quality non-intrusive bottom visual advertisement banner
            FullBannerAdView()
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun DailySpiritualInsightWidget(
    viewModel: DivineViewModel,
    modifier: Modifier = Modifier
) {
    val allStotrams by viewModel.allStotrams.collectAsState()
    
    val calendar = java.util.Calendar.getInstance()
    val dayOfYear = calendar.get(java.util.Calendar.DAY_OF_YEAR)
    var customOffset by remember { mutableStateOf(0) }
    
    val stotram = remember(allStotrams, dayOfYear, customOffset) {
        if (allStotrams.isNotEmpty()) {
            val index = (dayOfYear + customOffset) % allStotrams.size
            allStotrams[index]
        } else {
            null
        }
    }
    
    var showFullDialog by remember { mutableStateOf(false) }
    
    // Fallbacks
    val displayTitle = stotram?.title ?: "Upanishad Shanti Mantra"
    val displaySanskrit = stotram?.sanskritText ?: "ॐ असतो मा सद्गमय ।\nतमसो मा ज्योतिर्गमय ।\nमृत्योर्माऽमृतं गमय ॥"
    val displayTranslation = stotram?.translation ?: "Lead us from the unreal to the real, from darkness to light, from death to immortality."
    val displayBenefits = stotram?.benefits ?: "Installs supreme peace, calms mind, and increases spiritual wisdom."
    val displayType = stotram?.type ?: "MANTRA"

    // Card Gradient Background
    val gradientColors = listOf(
        Color(0xFFFF6D00), // Vibrant Saffron
        Color(0xFFDD2C00)  // Deep Saffron-Red
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("daily_insight_widget")
            .clip(RoundedCornerShape(16.dp))
            .clickable { showFullDialog = true },
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(androidx.compose.ui.graphics.Brush.linearGradient(gradientColors))
                .drawBehind {
                    // Draw sacred concentric circles on the GPU
                    val radius = size.minDimension / 1.5f
                    drawCircle(
                        color = Color(0xFFFFD700).copy(alpha = 0.08f),
                        radius = radius,
                        center = androidx.compose.ui.geometry.Offset(size.width, size.height * 0.2f),
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                    drawCircle(
                        color = Color(0xFFFFD700).copy(alpha = 0.04f),
                        radius = radius * 0.7f,
                        center = androidx.compose.ui.geometry.Offset(size.width, size.height * 0.2f),
                        style = Stroke(width = 1.dp.toPx())
                    )
                    drawCircle(
                        color = Color(0xFFFFD700).copy(alpha = 0.03f),
                        radius = radius * 1.3f,
                        center = androidx.compose.ui.geometry.Offset(0f, size.height * 0.8f),
                        style = Stroke(width = 1.dp.toPx())
                    )
                }
                .padding(16.dp)
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Sadhana Insight icon",
                            tint = Color(0xFFFFD700),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "DAILY SPIRITUAL INSIGHT",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.2.sp
                            ),
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        // Type Badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.White.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = displayType,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        // Refresh/Next button
                        var rotationState by remember { mutableStateOf(0f) }
                        val animatedRotation by animateFloatAsState(
                            targetValue = rotationState,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                        )
                        IconButton(
                            onClick = {
                                rotationState += 360f
                                customOffset += 1
                            },
                            modifier = Modifier
                                .size(32.dp)
                                .graphicsLayer(rotationZ = animatedRotation)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Next spiritual insight",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // Title
                Text(
                    text = displayTitle,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif
                    ),
                    color = Color.White
                )

                // Sanskrit Quote Block
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.12f))
                        .padding(10.dp)
                ) {
                    Text(
                        text = displaySanskrit.lines().take(3).joinToString("\n") { it.trim() } +
                                if (displaySanskrit.lines().size > 3) "\n..." else "",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Medium,
                            lineHeight = 20.sp,
                            textAlign = TextAlign.Center
                        ),
                        color = Color(0xFFFFE082), // Golden Yellow text
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Translation
                Text(
                    text = displayTranslation,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = Color.White.copy(alpha = 0.9f)
                )

                // Prompt user to tap to expand
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Tap to view full ritual context, chants & benefits",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Light),
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Expand details",
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }

    if (showFullDialog) {
        Dialog(onDismissRequest = { showFullDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp)
                    .testTag("daily_insight_dialog"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    // Title section
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = displayTitle,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Serif,
                                color = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { showFullDialog = false }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close dialog",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Saffron original card panel
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(androidx.compose.ui.graphics.Brush.linearGradient(gradientColors))
                                .padding(16.dp)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "Sacred Emblem",
                                    tint = Color(0xFFFFD700),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = displaySanskrit,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontFamily = FontFamily.Serif,
                                        fontWeight = FontWeight.Bold,
                                        lineHeight = 24.sp,
                                        textAlign = TextAlign.Center
                                    ),
                                    color = Color(0xFFFFE082),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        // Translation Section
                        Column {
                            Text(
                                text = "Sacred Interpretation (Translation)",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = displayTranslation,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Benefits Section
                        Column {
                            Text(
                                text = "Chanting Benefits & Astrological Significance",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = displayBenefits,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // TTS Audio Play Button inside dialog
                        Button(
                            onClick = {
                                if (stotram != null) {
                                    viewModel.speakStotram(stotram)
                                } else {
                                    // Custom fallback speech
                                    Toast.makeText(viewModel.getApplication(), "Playing Shanti Mantra chanting...", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play chant narration",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Chant Narration", fontSize = 13.sp)
                        }

                        // Close button
                        OutlinedButton(
                            onClick = { showFullDialog = false },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Close", fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GodCategoryGridCard(
    category: GodCategory,
    onClick: () -> Unit
) {
    val themeColor = runCatching { Color(android.graphics.Color.parseColor(category.defaultColor)) }.getOrDefault(MaterialTheme.colorScheme.primary)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "category_press_scale"
    )

    val borderAlpha by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 0.35f,
        label = "category_border_alpha"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .scale(scale)
            .border(
                width = 1.5.dp,
                color = themeColor.copy(alpha = borderAlpha),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = androidx.compose.foundation.LocalIndication.current,
                onClick = onClick
            )
            .testTag("category_card_${category.id}"),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp,
            pressedElevation = 8.dp
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            var isImageLoading by remember { mutableStateOf(true) }
            val imageAlpha by animateFloatAsState(
                targetValue = if (isImageLoading) 0.0f else 1.0f,
                animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
                label = "category_image_alpha"
            )
            val imageScale by animateFloatAsState(
                targetValue = if (isImageLoading) 0.94f else 1.0f,
                animationSpec = tween(durationMillis = 600, easing = LinearOutSlowInEasing),
                label = "category_image_scale"
            )

            // Full background thumbnail image
            AsyncImage(
                model = category.thumbnail,
                contentDescription = category.name,
                contentScale = ContentScale.Crop,
                alpha = imageAlpha,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(scaleX = imageScale, scaleY = imageScale),
                onLoading = { isImageLoading = true },
                onSuccess = { isImageLoading = false },
                onError = { isImageLoading = false }
            )

            // Modern subtle CSS loading spinner overlay
            if (isImageLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center
                ) {
                    SubtleCssSpinner(spinnerColor = themeColor)
                }
            }

            // Sleek vignette vertical gradient to pop colors and guarantee text legibility
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.1f),
                                Color.Black.copy(alpha = 0.4f),
                                Color.Black.copy(alpha = 0.85f)
                            )
                        )
                    )
            )

            // Subtle colored glow under-layer to radiate specific deity core energy aura
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        androidx.compose.ui.graphics.Brush.radialGradient(
                            colors = listOf(themeColor.copy(alpha = 0.3f), Color.Transparent),
                            radius = 400f,
                            center = androidx.compose.ui.geometry.Offset(x = 100f, y = 350f)
                        )
                    )
            )

            // Bottom glassmorphic card representation for the deity category title
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(8.dp)
                    .background(
                        color = Color.Black.copy(alpha = 0.65f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .border(
                        width = 0.5.dp,
                        color = Color.White.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Column {
                    Text(
                        text = category.name,
                        color = Color.White,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif,
                            fontSize = 14.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    Spacer(modifier = Modifier.height(2.dp))
                    
                    Text(
                        text = category.description,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.LightGray.copy(alpha = 0.8f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 11.sp
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GodDetailView(
    category: GodCategory,
    images: List<GodImage>,
    viewModel: DivineViewModel,
    onBack: () -> Unit
) {
    val stotrams by viewModel.activeStotrams.collectAsState()
    val temples by viewModel.activeTemples.collectAsState()

    var activeSubTab by remember { mutableStateOf("wallpapers") } // "wallpapers", "stotrams", "temples"
    val themeHexColor = runCatching { Color(android.graphics.Color.parseColor(category.defaultColor)) }.getOrDefault(MaterialTheme.colorScheme.primary)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // App top bar area
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, start = 8.dp, end = 16.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("god_detail_back_button")
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Go back to grid",
                    tint = themeHexColor
                )
            }
            Text(
                text = category.name,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif
                ),
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // Deity Description Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = themeHexColor.copy(alpha = 0.08f)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(8.dp))
                ) {
                    AsyncImage(
                        model = category.thumbnail,
                        contentDescription = "God",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Divine Bio",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = themeHexColor
                    )
                    Text(
                        text = category.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Custom M3 Sub Tab Row
        TabRow(
            selectedTabIndex = when (activeSubTab) {
                "wallpapers" -> 0
                "stotrams" -> 1
                else -> 2
            },
            containerColor = Color.Transparent,
            contentColor = themeHexColor,
            indicator = { tabPositions ->
                val selectedIndex = when (activeSubTab) {
                    "wallpapers" -> 0
                    "stotrams" -> 1
                    else -> 2
                }
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedIndex]),
                    color = themeHexColor
                )
            }
        ) {
            Tab(
                selected = activeSubTab == "wallpapers",
                onClick = { activeSubTab = "wallpapers" },
                text = { Text("Wallpapers", fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = activeSubTab == "stotrams",
                onClick = { activeSubTab = "stotrams" },
                text = { Text("Puja / Stotram", fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = activeSubTab == "temples",
                onClick = { activeSubTab = "temples" },
                text = { Text("Temples", fontWeight = FontWeight.Bold) }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Tab Content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            when (activeSubTab) {
                "wallpapers" -> {
                    if (images.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                "No High-Res wallpapers configured for ${category.name}.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        val configuration = LocalConfiguration.current
                        val columnCount = when {
                            configuration.screenWidthDp >= 900 -> 4
                            configuration.screenWidthDp >= 600 -> 3
                            else -> 2
                        }

                        LazyVerticalGrid(
                            columns = GridCells.Fixed(columnCount),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(bottom = 100.dp),
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("wallpapers_list")
                        ) {
                            items(images) { image ->
                                WallpaperGridItem(
                                    image = image,
                                    themeHexColor = themeHexColor,
                                    onClick = { viewModel.selectImage(image) },
                                    onFavoriteClick = { viewModel.toggleFavorite(image) }
                                )
                            }
                        }
                    }
                }
                "stotrams" -> {
                    if (stotrams.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                "No hymns, stotrams or puja materials recorded yet. Sync with the cloud admin panel under Sadhana settings to update.",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(bottom = 80.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(stotrams) { stotram ->
                                StotramCard(stotram = stotram, viewModel = viewModel, headerColor = themeHexColor)
                            }
                        }
                    }
                }
                "temples" -> {
                    var templeSearchQuery by remember { mutableStateOf("") }
                    val categoriesList by viewModel.categories.collectAsState()
                    val filteredTemples = remember(temples, templeSearchQuery, categoriesList) {
                        if (templeSearchQuery.isBlank()) {
                            temples
                        } else {
                            temples.filter { temple ->
                                val deityName = categoriesList.find { it.id == temple.categoryId }?.name ?: ""
                                temple.location.contains(templeSearchQuery, ignoreCase = true) ||
                                        temple.name.contains(templeSearchQuery, ignoreCase = true) ||
                                        deityName.contains(templeSearchQuery, ignoreCase = true)
                            }
                        }
                    }

                    if (temples.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                "No sacred temples recorded for ${category.name}. Check your offline data.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        Column(modifier = Modifier.fillMaxSize()) {
                            OutlinedTextField(
                                value = templeSearchQuery,
                                onValueChange = { templeSearchQuery = it },
                                placeholder = { Text("Search temples by city or deity (e.g. Mumbai, Shiva)...") },
                                leadingIcon = { 
                                    Icon(
                                        imageVector = Icons.Default.Search, 
                                        contentDescription = "Search icon",
                                        tint = themeHexColor
                                    ) 
                                },
                                trailingIcon = {
                                    if (templeSearchQuery.isNotEmpty()) {
                                        IconButton(onClick = { templeSearchQuery = "" }) {
                                            Icon(
                                                imageVector = Icons.Default.Close, 
                                                contentDescription = "Clear search",
                                                tint = themeHexColor
                                            )
                                        }
                                    }
                                },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp)
                                    .testTag("temple_search_bar"),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = themeHexColor,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                                )
                            )

                            if (filteredTemples.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(16.dp)
                                    ) {
                                        Text(
                                            text = "No Temples Found",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "No matches for \"$templeSearchQuery\" in our offline database for ${category.name}. Check spellings or search by another location.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            } else {
                                LazyColumn(
                                    contentPadding = PaddingValues(bottom = 80.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                        .testTag("temples_list")
                                ) {
                                    items(filteredTemples) { temple ->
                                        TempleCard(temple = temple, themeColor = themeHexColor)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WallpaperGridItem(
    image: GodImage,
    themeHexColor: Color,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "press_scale"
    )
    
    val borderAlpha by animateFloatAsState(
        targetValue = if (isPressed) 0.8f else 0.25f,
        label = "border_glow"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(230.dp)
            .scale(scale)
            .border(
                width = 1.dp,
                color = themeHexColor.copy(alpha = borderAlpha),
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = androidx.compose.foundation.LocalIndication.current,
                onClick = onClick
            )
            .testTag("wallpaper_item_card_${image.id}"),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp,
            pressedElevation = 8.dp
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            var isImageLoading by remember { mutableStateOf(true) }
            val imageAlpha by animateFloatAsState(
                targetValue = if (isImageLoading) 0.0f else 1.0f,
                animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
                label = "wallpaper_image_alpha"
            )
            val imageScale by animateFloatAsState(
                targetValue = if (isImageLoading) 0.94f else 1.0f,
                animationSpec = tween(durationMillis = 600, easing = LinearOutSlowInEasing),
                label = "wallpaper_image_scale"
            )

            // High-res wallpaper thumbnail
            AsyncImage(
                model = image.thumbUrl.ifEmpty { image.url },
                contentDescription = image.title,
                contentScale = ContentScale.Crop,
                alpha = imageAlpha,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(scaleX = imageScale, scaleY = imageScale),
                onLoading = { isImageLoading = true },
                onSuccess = { isImageLoading = false },
                onError = { isImageLoading = false }
            )

            // Modern subtle CSS loading spinner overlay
            if (isImageLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center
                ) {
                    SubtleCssSpinner(spinnerColor = themeHexColor)
                }
            }
            
            // Subtle dark wash over the image to guarantee text contrast
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.1f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.75f)
                            )
                        )
                    )
            )

            // Top-Right: Floating tactile favorite indicator button
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            ) {
                IconButton(
                    onClick = onFavoriteClick,
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color.Black.copy(alpha = 0.45f), CircleShape)
                        .testTag("favorite_button_${image.id}")
                ) {
                    Icon(
                        imageVector = if (image.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite image icon",
                        tint = if (image.isFavorite) Color.Red else Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Bottom Glassmorphic Card Overlay for title info
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(8.dp)
                    .background(
                        color = Color.Black.copy(alpha = 0.65f),
                        shape = RoundedCornerShape(10.dp)
                    )
                    .border(
                        width = 0.5.dp,
                        color = Color.White.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(10.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Column {
                    Text(
                        text = image.title,
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Serif
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (image.credit.isNotEmpty()) image.credit else "Divine",
                            color = Color.LightGray.copy(alpha = 0.75f),
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 8.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = "↓ ${image.downloadCount}",
                                color = themeHexColor,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                fontSize = 8.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SubtleCssSpinner(
    modifier: Modifier = Modifier,
    spinnerColor: Color = Color.White
) {
    val infiniteTransition = rememberInfiniteTransition(label = "css_spinner")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "css_spinner_angle"
    )

    Box(
        modifier = modifier
            .size(34.dp)
            .drawBehind {
                val strokeWidth = 3.dp.toPx()
                // Outer tracking circle (faint)
                drawCircle(
                    color = spinnerColor.copy(alpha = 0.12f),
                    style = Stroke(width = strokeWidth)
                )
                // Rotating highlight segment
                rotate(angle) {
                    drawArc(
                        color = spinnerColor,
                        startAngle = -90f,
                        sweepAngle = 110f,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }
            }
    )
}

@Composable
fun EqualizerAnimation(isPlaying: Boolean, tint: Color) {
    Row(
        modifier = Modifier
            .height(18.dp)
            .padding(horizontal = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        val transition = rememberInfiniteTransition(label = "equalizer_loop")
        val heights = listOf(0.3f, 0.8f, 0.5f, 0.9f, 0.4f)

        heights.forEachIndexed { index, baseHeight ->
            val animatedHeight by if (isPlaying) {
                transition.animateFloat(
                    initialValue = 3f,
                    targetValue = 18f * baseHeight,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 350 + index * 90, easing = androidx.compose.animation.core.LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "eq_bar_$index"
                )
            } else {
                remember { mutableStateOf(3f) }
            }

            Box(
                modifier = Modifier
                    .width(2.5.dp)
                    .height(animatedHeight.dp)
                    .background(tint, shape = RoundedCornerShape(1.dp))
            )
        }
    }
}

@Composable
fun StotramCard(stotram: StotramPuja, viewModel: DivineViewModel, headerColor: Color) {
    var expanded by remember { mutableStateOf(false) }

    val currentlyPlaying by viewModel.currentlyPlayingStotram.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val playbackPos by viewModel.playbackPosition.collectAsState()
    val elapsedStr by viewModel.playbackElapsed.collectAsState()
    val durationStr by viewModel.playbackDuration.collectAsState()

    val isThisPlaying = currentlyPlaying?.id == stotram.id
    val isCurrentActiveAndPlaying = isThisPlaying && isPlaying

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("stotram_card_${stotram.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (isThisPlaying) {
                headerColor.copy(alpha = 0.08f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            }
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).clickable { expanded = !expanded }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SuggestionChip(
                            onClick = { },
                            label = { Text(stotram.type, fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = headerColor.copy(alpha = 0.15f),
                                labelColor = headerColor
                            )
                        )
                        if (isThisPlaying) {
                            Spacer(modifier = Modifier.width(8.dp))
                            EqualizerAnimation(isPlaying = isCurrentActiveAndPlaying, tint = headerColor)
                        }
                    }
                    Text(
                        text = stotram.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            if (isThisPlaying) {
                                viewModel.togglePlayPause()
                            } else {
                                viewModel.playStotram(stotram)
                                expanded = true
                            }
                        },
                        modifier = Modifier.testTag("play_btn_${stotram.id}")
                    ) {
                        Text(
                            text = if (isCurrentActiveAndPlaying) "⏸" else "▶",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = headerColor
                        )
                    }

                    IconButton(
                        onClick = { expanded = !expanded },
                        modifier = Modifier.testTag("expand_btn_${stotram.id}")
                    ) {
                        Icon(
                            imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = "Expand icon",
                            tint = headerColor
                        )
                    }
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    
                    if (isThisPlaying) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "🎵",
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Sacred Chant Audio Playback",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = headerColor
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Slider(
                                    value = playbackPos,
                                    onValueChange = { viewModel.seekPlayback(it) },
                                    modifier = Modifier.fillMaxWidth().testTag("audio_slider_${stotram.id}"),
                                    colors = SliderDefaults.colors(
                                        thumbColor = headerColor,
                                        activeTrackColor = headerColor,
                                        inactiveTrackColor = headerColor.copy(alpha = 0.2f)
                                    )
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = elapsedStr,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = durationStr,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Devanagari Sanskrit Hymn:",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = headerColor
                    )
                    Text(
                        text = stotram.sanskritText,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontFamily = FontFamily.Serif,
                            lineHeight = 22.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(headerColor.copy(alpha = 0.05f))
                            .padding(10.dp)
                            .clip(RoundedCornerShape(6.dp))
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Satyavachan (Deep Translation):",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = stotram.translation,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Text(
                        text = "Phalashruti (Spiritual Benefits):",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = headerColor
                    )
                    Text(
                        text = stotram.benefits,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun TempleCard(temple: TempleInfo, themeColor: Color) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
            ) {
                AsyncImage(
                    model = temple.imageUrl,
                    contentDescription = temple.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                // Bottom Gradient
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            androidx.compose.ui.graphics.Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp)
                ) {
                    Text(
                        text = temple.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Place,
                            contentDescription = "Place marker",
                            tint = themeColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = temple.location,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.LightGray
                        )
                    }
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Puranic History & Lore:",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = themeColor
                    )
                    Text(
                        text = temple.history,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Text(
                        text = "Cosmic Significance:",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = temple.significance,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Text(
                        text = "Darshan Timings:",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = themeColor
                    )
                    Text(
                        text = temple.timing,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun ImageDetailSheet(
    image: GodImage,
    onBack: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onDownload: () -> Unit,
    onSetWallpaper: (String, Int) -> Unit,
    isOperationLoading: Boolean,
    operationMessage: String?,
    clearMessage: () -> Unit,
    themeHexColor: Color = MaterialTheme.colorScheme.primary
) {
    val context = LocalContext.current
    var wallPaperDialogVisible by remember { mutableStateOf(false) }
    var isHighResLoading by remember { mutableStateOf(true) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // High-Quality image occupies the entire viewport bounds
        AsyncImage(
            model = image.url,
            contentDescription = image.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            onLoading = { isHighResLoading = true },
            onSuccess = { isHighResLoading = false },
            onError = { isHighResLoading = false }
        )

        // Modern, subtle CSS loading spinner overlay
        if (isHighResLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SubtleCssSpinner(spinnerColor = themeHexColor)
                    Text(
                        text = "Fetching High-Res Wallpaper...",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.LightGray.copy(alpha = 0.8f)
                    )
                }
            }
        }

        // Backdrop tint to ensure top bar and bottom buttons are crystal clear
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.5f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.8f)
                        )
                    )
                )
        )

        // Top Utility row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 28.dp, start = 12.dp, end = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(50.dp))
                    .testTag("wallpaper_detail_back")
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }

            IconButton(
                onClick = onFavoriteToggle,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(50.dp))
            ) {
                Icon(
                    imageVector = if (image.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Favorite image icon",
                    tint = if (image.isFavorite) Color.Red else Color.White
                )
            }
        }

        // Bottom Controls area
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.7f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = image.title,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Text(
                        text = "Artwork Credit: ${image.credit}",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.LightGray
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = image.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onDownload,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("download_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Download HD")
                        }

                        Button(
                            onClick = { wallPaperDialogVisible = true },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("set_wallpaper_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Set Wallpaper")
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(30.dp)) // lift above system navigation bars
        }

        // Operation status popup dialog info
        if (operationMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .width(280.dp)
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (isOperationLoading) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                        Text(
                            text = operationMessage,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (!isOperationLoading) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { clearMessage() }) {
                                Text("OK")
                            }
                        }
                    }
                }
            }
        }

        // Wallpaper Location Selector Dialog
        if (wallPaperDialogVisible) {
            Dialog(onDismissRequest = { wallPaperDialogVisible = false }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Set Sacred Wallpaper",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Where would you like to apply this divine wallpaper?",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = {
                                    onSetWallpaper(image.url, 1)
                                    wallPaperDialogVisible = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Home Screen")
                            }
                            Button(
                                onClick = {
                                    onSetWallpaper(image.url, 2)
                                    wallPaperDialogVisible = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Lock Screen")
                            }
                            Button(
                                onClick = {
                                    onSetWallpaper(image.url, 3)
                                    wallPaperDialogVisible = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Both Screens")
                            }
                            TextButton(
                                onClick = { wallPaperDialogVisible = false },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Cancel")
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun DailySpiritualInsightDialog(
    viewModel: DivineViewModel,
    onDismiss: () -> Unit
) {
    val allStotrams by viewModel.allStotrams.collectAsState()
    
    val calendar = java.util.Calendar.getInstance()
    val dayOfYear = calendar.get(java.util.Calendar.DAY_OF_YEAR)
    var customOffset by remember { mutableStateOf(0) }
    
    val stotram = remember(allStotrams, dayOfYear, customOffset) {
        if (allStotrams.isNotEmpty()) {
            val index = (dayOfYear + customOffset) % allStotrams.size
            allStotrams[index]
        } else {
            null
        }
    }
    
    // Fallbacks
    val displayTitle = stotram?.title ?: "Upanishad Shanti Mantra"
    val displaySanskrit = stotram?.sanskritText ?: "ॐ असतो मा सद्गमय ।\nतमसो मा ज्योतिर्गमय ।\nमृत्योर्माऽमृतं गमय ॥"
    val displayTranslation = stotram?.translation ?: "Lead us from the unreal to the real, from darkness to light, from death to immortality."
    val displayBenefits = stotram?.benefits ?: "Installs supreme peace, calms mind, and increases spiritual wisdom."
    val displayType = stotram?.type ?: "MANTRA"

    // Card Gradient Background (Luxury Saffron-Orange-Crimson Theme)
    val gradientColors = listOf(
        Color(0xFFFF6D00), // Vibrant Saffron
        Color(0xFFE65100), // Dark Saffron
        Color(0xFFDD2C00)  // Deep Saffron-Red
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .testTag("daily_insight_float_screen"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                // Header (Top Row with Title and Close Button)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Spiritual Icon",
                            tint = Color(0xFFFFB300),
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Daily Insight",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Serif,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("daily_insight_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close floating screen",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // AnimatedContent wraps the dynamic stotram states to perform beautiful, smooth cross-fading transitions!
                AnimatedContent(
                    targetState = stotram,
                    transitionSpec = {
                        (fadeIn(animationSpec = tween(350)) + 
                         scaleIn(initialScale = 0.95f, animationSpec = tween(350))) togetherWith
                        (fadeOut(animationSpec = tween(250)) +
                         scaleOut(targetScale = 0.95f, animationSpec = tween(250)))
                    },
                    label = "SadhanaInsightTransition"
                ) { currentStotram ->
                    val animTitle = currentStotram?.title ?: displayTitle
                    val animSanskrit = currentStotram?.sanskritText ?: displaySanskrit
                    val animTranslation = currentStotram?.translation ?: displayTranslation
                    val animBenefits = currentStotram?.benefits ?: displayBenefits
                    val animType = currentStotram?.type ?: displayType

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Card with Sacred Background
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(androidx.compose.ui.graphics.Brush.linearGradient(gradientColors))
                                .drawBehind {
                                    val radius = size.minDimension / 1.3f
                                    drawCircle(
                                        color = Color(0xFFFFD700).copy(alpha = 0.08f),
                                        radius = radius,
                                        center = androidx.compose.ui.geometry.Offset(size.width * 0.9f, size.height * 0.2f),
                                        style = Stroke(width = 1.5.dp.toPx())
                                    )
                                    drawCircle(
                                        color = Color(0xFFFFD700).copy(alpha = 0.04f),
                                        radius = radius * 0.7f,
                                        center = androidx.compose.ui.geometry.Offset(size.width * 0.9f, size.height * 0.2f),
                                        style = Stroke(width = 1.dp.toPx())
                                    )
                                }
                                .padding(16.dp)
                        ) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Badge & Next Button trigger
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color.White.copy(alpha = 0.2f))
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = animType,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }

                                    // Refresh next button with click feedback & rotation
                                    var rotationState by remember { mutableStateOf(0f) }
                                    val animatedRotation by animateFloatAsState(
                                        targetValue = rotationState,
                                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                                    )

                                    IconButton(
                                        onClick = {
                                            rotationState += 360f
                                            customOffset += 1
                                        },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(Color.White.copy(alpha = 0.15f), CircleShape)
                                            .graphicsLayer(rotationZ = animatedRotation)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = "Next spiritual insight",
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                // Title of Stotram/Mantra
                                Text(
                                    text = animTitle,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Serif
                                    ),
                                    color = Color.White
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                // Sacred Sanskrit Quote Block
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color.Black.copy(alpha = 0.2f))
                                        .padding(12.dp)
                                ) {
                                    Text(
                                        text = animSanskrit,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontFamily = FontFamily.Serif,
                                            fontWeight = FontWeight.Bold,
                                            lineHeight = 22.sp,
                                            textAlign = TextAlign.Center
                                        ),
                                        color = Color(0xFFFFE082),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }

                        // Translation Section
                        Column(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Sacred Translation",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = animTranslation,
                                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Benefits Section
                        Column(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Divine Benefits & Sadhana Fruit",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Benefit Icon",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp).padding(top = 2.dp)
                                )
                                Text(
                                    text = animBenefits,
                                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // TTS Audio Play Button inside dialog
                            Button(
                                onClick = {
                                    if (currentStotram != null) {
                                        viewModel.speakStotram(currentStotram)
                                    } else {
                                        // Custom fallback speech
                                        Toast.makeText(viewModel.getApplication(), "Playing Shanti Mantra chanting...", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Play chant narration",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Chant Narration", fontSize = 13.sp)
                            }

                            // Close button inside dialog
                            OutlinedButton(
                                onClick = onDismiss,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Dismiss", fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

