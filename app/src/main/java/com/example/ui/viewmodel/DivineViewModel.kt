package com.example.ui.viewmodel

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.example.R
import com.example.data.database.Festival
import com.example.data.database.GodCategory
import com.example.data.database.GodImage
import com.example.data.database.StotramPuja
import com.example.data.database.TempleInfo
import com.example.data.repository.DivineRepository
import android.speech.tts.TextToSpeech
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Simple model for conversation history
data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

class DivineViewModel(
    application: Application,
    private val repository: DivineRepository
) : AndroidViewModel(application) {

    private val _sharedPrefs = application.getSharedPreferences("devi_devata_prefs", Context.MODE_PRIVATE)

    // --- Dark Theme Toggle State ---
    private val _isDarkTheme = MutableStateFlow(_sharedPrefs.getBoolean("is_dark_theme", false))
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    fun toggleTheme() {
        val nextVal = !_isDarkTheme.value
        _isDarkTheme.value = nextVal
        _sharedPrefs.edit().putBoolean("is_dark_theme", nextVal).apply()
    }

    // --- Simulated Audio Stotram Player State Engine ---
    private val _currentlyPlayingStotram = MutableStateFlow<StotramPuja?>(null)
    val currentlyPlayingStotram: StateFlow<StotramPuja?> = _currentlyPlayingStotram.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _playbackPosition = MutableStateFlow(0f)
    val playbackPosition: StateFlow<Float> = _playbackPosition.asStateFlow()

    private val _playbackElapsed = MutableStateFlow("00:00")
    val playbackElapsed: StateFlow<String> = _playbackElapsed.asStateFlow()

    private val _playbackDuration = MutableStateFlow("03:45")
    val playbackDuration: StateFlow<String> = _playbackDuration.asStateFlow()

    private val _playbackDurationSec = MutableStateFlow(225)
    private val _playbackElapsedSec = MutableStateFlow(0)
    private var playbackJob: kotlinx.coroutines.Job? = null

    fun playStotram(stotram: StotramPuja) {
        stopTts() // Ensure active spoken TTS is stopped first
        if (_currentlyPlayingStotram.value?.id != stotram.id) {
            _currentlyPlayingStotram.value = stotram
            _playbackElapsedSec.value = 0
            _playbackPosition.value = 0f
            // Generate a realistic duration (e.g. 120-270s) depending on stotram ID
            val totalSec = 120 + (stotram.id * 35) % 150
            _playbackDurationSec.value = totalSec
            _playbackDuration.value = formatSeconds(totalSec)
            _playbackElapsed.value = "00:00"
        }
        _isPlaying.value = true
        startProgressJob()
    }

    fun pauseStotram() {
        _isPlaying.value = false
        playbackJob?.cancel()
    }

    fun togglePlayPause() {
        if (_isPlaying.value) {
            pauseStotram()
        } else {
            val current = _currentlyPlayingStotram.value
            if (current != null) {
                _isPlaying.value = true
                startProgressJob()
            }
        }
    }

    private fun startProgressJob() {
        playbackJob?.cancel()
        playbackJob = viewModelScope.launch(Dispatchers.Main) {
            while (_isPlaying.value) {
                kotlinx.coroutines.delay(1000L)
                val nextSec = _playbackElapsedSec.value + 1
                val totalSec = _playbackDurationSec.value
                if (nextSec >= totalSec) {
                    _playbackElapsedSec.value = totalSec
                    _playbackPosition.value = 1f
                    _playbackElapsed.value = formatSeconds(totalSec)
                    _isPlaying.value = false
                    break
                } else {
                    _playbackElapsedSec.value = nextSec
                    _playbackPosition.value = nextSec.toFloat() / totalSec
                    _playbackElapsed.value = formatSeconds(nextSec)
                }
            }
        }
    }

    private fun formatSeconds(sec: Int): String {
        val m = sec / 60
        val s = sec % 60
        return String.format(Locale.getDefault(), "%02d:%02d", m, s)
    }

    fun seekPlayback(pos: Float) {
        val totalSec = _playbackDurationSec.value
        val targetSec = (pos * totalSec).toInt().coerceIn(0, totalSec)
        _playbackElapsedSec.value = targetSec
        _playbackPosition.value = pos
        _playbackElapsed.value = formatSeconds(targetSec)
    }

    // --- Native Android Text-To-Speech (TTS) Engine ---
    private var textToSpeech: TextToSpeech? = null
    private var isTtsInitialized = false

    private val _ttsActiveStotram = MutableStateFlow<StotramPuja?>(null)
    val ttsActiveStotram: StateFlow<StotramPuja?> = _ttsActiveStotram.asStateFlow()

    private val _isTtsSpeaking = MutableStateFlow(false)
    val isTtsSpeaking: StateFlow<Boolean> = _isTtsSpeaking.asStateFlow()

    private val _ttsSelectedType = MutableStateFlow("translation") // "sanskrit", "translation", "benefits"
    val ttsSelectedType: StateFlow<String> = _ttsSelectedType.asStateFlow()

    fun setTtsSelectedType(type: String) {
        _ttsSelectedType.value = type
        val current = _ttsActiveStotram.value
        if (current != null && _isTtsSpeaking.value) {
            speakStotram(current)
        }
    }

    fun speakStotram(stotram: StotramPuja) {
        pauseStotram() // Pause simulated chant playback to prevent overlap
        _ttsActiveStotram.value = stotram

        if (textToSpeech == null) {
            _isTtsSpeaking.value = true
            textToSpeech = TextToSpeech(getApplication()) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    isTtsInitialized = true
                    performSpeak(stotram)
                } else {
                    Log.e("DivineViewModel", "TTS Init Failed: $status")
                    _isTtsSpeaking.value = false
                }
            }
        } else if (isTtsInitialized) {
            performSpeak(stotram)
        }
    }

    private fun performSpeak(stotram: StotramPuja) {
        val tts = textToSpeech ?: return
        val type = _ttsSelectedType.value
        val textToSpeak = when (type) {
            "sanskrit" -> {
                tts.language = Locale("hi", "IN") // Hindi/Sanskrit locale beautifully reads Devanagari script
                stotram.sanskritText
            }
            "benefits" -> {
                tts.language = Locale.US
                "Spiritual benefits of chanting ${stotram.title}: ${stotram.benefits}"
            }
            else -> {
                tts.language = Locale.US
                "English translation of ${stotram.title}: ${stotram.translation}"
            }
        }

        tts.setPitch(1.0f)
        tts.setSpeechRate(0.88f) // Divine and peaceful slow pace

        val utteranceId = "stotram_${stotram.id}_$type"
        _isTtsSpeaking.value = true

        tts.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                _isTtsSpeaking.value = true
            }

            override fun onDone(utteranceId: String?) {
                _isTtsSpeaking.value = false
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                _isTtsSpeaking.value = false
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                _isTtsSpeaking.value = false
                Log.e("DivineViewModel", "TTS Error: $errorCode for $utteranceId")
            }
        })

        val params = android.os.Bundle()
        params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
        tts.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
    }

    fun stopTts() {
        textToSpeech?.stop()
        _isTtsSpeaking.value = false
    }

    // --- Active UI Management States ---
    private val _activeTab = MutableStateFlow("gallery")
    val activeTab: StateFlow<String> = _activeTab.asStateFlow()

    private val _selectedCategory = MutableStateFlow<GodCategory?>(null)
    val selectedCategory: StateFlow<GodCategory?> = _selectedCategory.asStateFlow()

    private val _selectedImage = MutableStateFlow<GodImage?>(null)
    val selectedImage: StateFlow<GodImage?> = _selectedImage.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // --- Server Sync Inputs ---
    private val _serverUrl = com.example.data.util.Obfuscator.getDecodedUrl().let { defaultVal ->
        MutableStateFlow(_sharedPrefs.getString("server_url", defaultVal) ?: defaultVal)
    }
    val serverUrl: StateFlow<String> = _serverUrl.asStateFlow()

    private val _syncStatus = MutableStateFlow<String?>(null) // null = idle, "LOADING", "SUCCESS", "ERROR"
    val syncStatus: StateFlow<String?> = _syncStatus.asStateFlow()

    // --- Image Operation Feedback States ---
    private val _operationMessage = MutableStateFlow<String?>(null)
    val operationMessage: StateFlow<String?> = _operationMessage.asStateFlow()

    private val _isOperationLoading = MutableStateFlow(false)
    val isOperationLoading: StateFlow<Boolean> = _isOperationLoading.asStateFlow()

    // --- Language Preference & Content Filter State ---
    private val _selectedLanguage = MutableStateFlow(_sharedPrefs.getString("selected_language", "Sanskrit") ?: "Sanskrit")
    val selectedLanguage: StateFlow<String> = _selectedLanguage.asStateFlow()

    fun changeLanguage(language: String) {
        _selectedLanguage.value = language
        _sharedPrefs.edit().putString("selected_language", language).apply()
        
        // Pull stotrams, aartis, mantras for this language from the user's custom server if they hit language sync
        viewModelScope.launch {
            _syncStatus.value = "LOADING"
            _operationMessage.value = "Synchronizing $language spiritual texts from server..."
            val success = repository.syncStotramsByLanguage(_serverUrl.value, language)
            if (success) {
                _syncStatus.value = "SUCCESS"
                _operationMessage.value = "Loaded authentic $language texts into offline vault!"
            } else {
                _syncStatus.value = "ERROR"
                _operationMessage.value = "Using offline cached $language library."
            }
        }
    }

    // --- Mythology Chat Conversation ---
    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage("Saddharanam! Welcome to DeviDevata Mythology AI guide. Ask me any question about Vedas, deities, mantra protocols, rituals or Puranic histories.", false)
        )
    )
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isChatLoading = MutableStateFlow(false)
    val isChatLoading: StateFlow<Boolean> = _isChatLoading.asStateFlow()

    // --- Reactive Database Queries with Search Query mapping ---
    val categories: StateFlow<List<GodCategory>> = repository.allCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val images: StateFlow<List<GodImage>> = _selectedCategory
        .combine(repository.allImages) { category, allImgs ->
            if (category == null) {
                allImgs
            } else {
                allImgs.filter { it.categoryId == category.id }
            }
        }.combine(_searchQuery) { list, query ->
            if (query.isBlank()) list else list.filter { it.title.contains(query, ignoreCase = true) || it.description.contains(query, ignoreCase = true) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favorites: StateFlow<List<GodImage>> = repository.favoriteImages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allStotrams: StateFlow<List<StotramPuja>> = repository.divineDao.getAllStotrams()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeStotrams: StateFlow<List<StotramPuja>> = _selectedCategory
        .combine(allStotrams) { category, allHymns ->
            if (category == null) {
                allHymns
            } else {
                allHymns.filter { it.categoryId == category.id }
            }
        }.combine(_selectedLanguage) { list, language ->
            list.filter { it.language.equals(language, ignoreCase = true) }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeTemples: StateFlow<List<TempleInfo>> = _selectedCategory
        .flatMapLatestFlow { category ->
            if (category == null) repository.divineDao.getAllTemples()
            else repository.getTemplesByGod(category.id)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val festivals: StateFlow<List<Festival>> = repository.allFestivals
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Helpers to flatten flows ---
    private fun <T, R> StateFlow<T>.flatMapLatestFlow(transform: (T) -> kotlinx.coroutines.flow.Flow<R>): kotlinx.coroutines.flow.Flow<R> {
        return kotlinx.coroutines.flow.flow {
            collect { value ->
                transform(value).collect { emit(it) }
            }
        }
    }

    init {
        // Enforce prepopulation immediately on startup
        viewModelScope.launch {
            repository.checkAndPrepopulateData()
            setupNotificationChannel()

            try {
                // Clean up any previously inserted 24-hour demo festival
                repository.divineDao.deleteDemoFestival()
            } catch (e: Exception) {
                Log.e("DivineViewModel", "Failed to clean up 24-hour demo festival", e)
            }
        }
    }

    // --- Navigation Actions ---
    fun selectTab(tab: String) {
        _activeTab.value = tab
        _searchQuery.value = "" // clear searches on transition
    }

    fun selectCategory(category: GodCategory?) {
        _selectedCategory.value = category
    }

    fun selectImage(image: GodImage?) {
        _selectedImage.value = image
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setServerUrl(url: String) {
        _serverUrl.value = url
        _sharedPrefs.edit().putString("server_url", url).apply()
    }

    fun clearOperationMessage() {
        _operationMessage.value = null
    }

    // --- Image Operations (Set Wallpaper & Download to Disk) ---
    fun setWallpaper(imageUrl: String, locationType: Int) { // 1 = Home, 2 = Lock, 3 = Both
        viewModelScope.launch {
            _isOperationLoading.value = true
            _operationMessage.value = "Downloading image high-quality payload..."
            try {
                val bitmap = downloadBitmap(imageUrl)
                if (bitmap != null) {
                    val wpm = WallpaperManager.getInstance(getApplication())
                    withContext(Dispatchers.IO) {
                        when (locationType) {
                            1 -> {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                    wpm.setBitmap(bitmap, null, true, WallpaperManager.FLAG_SYSTEM)
                                } else {
                                    wpm.setBitmap(bitmap)
                                }
                            }
                            2 -> {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                    wpm.setBitmap(bitmap, null, true, WallpaperManager.FLAG_LOCK)
                                } else {
                                    wpm.setBitmap(bitmap)
                                }
                            }
                            3 -> {
                                wpm.setBitmap(bitmap)
                            }
                        }
                    }
                    _operationMessage.value = "Divine Wallpaper refreshed successfully!"
                    _selectedImage.value?.let { repository.updateImageDownloadCount(it) }
                } else {
                    _operationMessage.value = "Could not decode wallpaper payload. Try again."
                }
            } catch (e: Exception) {
                Log.e("DivineViewModel", "Wallpaper set failure", e)
                _operationMessage.value = "Failed to apply wallpaper: ${e.localizedMessage}"
            } finally {
                _isOperationLoading.value = false
            }
        }
    }

    fun downloadImageToDevice(godImage: GodImage) {
        viewModelScope.launch {
            _isOperationLoading.value = true
            _operationMessage.value = "Connecting to high-speed cloud mirror..."
            try {
                val bitmap = downloadBitmap(godImage.url)
                if (bitmap != null) {
                    val savedFile = saveBitmapToPictures(bitmap, godImage.title)
                    if (savedFile != null) {
                        _operationMessage.value = "Downloaded: Pictures/DeviDevata/${savedFile.name}"
                        repository.updateImageDownloadCount(godImage)
                    } else {
                        _operationMessage.value = "Failed saving image. Verify disk space."
                    }
                } else {
                    _operationMessage.value = "Failed fetching file payload."
                }
            } catch (e: Exception) {
                _operationMessage.value = "Error: ${e.localizedMessage}"
            } finally {
                _isOperationLoading.value = false
            }
        }
    }

    private suspend fun downloadBitmap(url: String): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val loader = ImageLoader(getApplication())
            val request = ImageRequest.Builder(getApplication())
                .data(url)
                .allowHardware(false) // Required to convert to drawable -> bitmap
                .build()
            val result = loader.execute(request)
            if (result is SuccessResult) {
                (result.drawable as BitmapDrawable).bitmap
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("DivineViewModel", "Error fetching image", e)
            null
        }
    }

    private suspend fun saveBitmapToPictures(bitmap: Bitmap, title: String): File? = withContext(Dispatchers.IO) {
        try {
            val dir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                "DeviDevata"
            )
            if (!dir.exists()) dir.mkdirs()

            val sanitizedTitle = title.replace("\\s+".toRegex(), "_").lowercase()
            val file = File(dir, "devidevata_${sanitizedTitle}_${System.currentTimeMillis()}.jpg")
            val fos = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, fos)
            fos.flush()
            fos.close()
            file
        } catch (e: Exception) {
            Log.e("DivineViewModel", "Save file error", e)
            null
        }
    }

    fun toggleFavorite(godImage: GodImage) {
        viewModelScope.launch {
            repository.toggleFavorite(godImage)
            // also update selected image ref to maintain state
            if (_selectedImage.value?.id == godImage.id) {
                _selectedImage.value = _selectedImage.value?.copy(isFavorite = !godImage.isFavorite)
            }
        }
    }

    // --- Synchronization trigger ---
    fun syncWithCloudServer() {
        val url = _serverUrl.value
        if (url.isBlank()) {
            _syncStatus.value = "ERROR"
            _operationMessage.value = "Error: Server address cannot be empty."
            return
        }
        viewModelScope.launch {
            _syncStatus.value = "LOADING"
            _operationMessage.value = "Synchronizing with Cloud server database..."
            val outcome = repository.syncDataFromServer(url)
            if (outcome) {
                _syncStatus.value = "SUCCESS"
                _operationMessage.value = "Divine Sync complete! Offline database updated."
                // Trigger push notification for sync outcome
                dispatchLocalNotification("System Sync", "Database successfully synchronized with admin panel.")
            } else {
                _syncStatus.value = "ERROR"
                _operationMessage.value = "Remote sync failed. Retaining active offline database."
            }
        }
    }

    // --- Mythology AI Chat ---
    fun submitMessageToAI(prompt: String) {
        if (prompt.isBlank()) return
        val currentList = _chatMessages.value.toMutableList()
        currentList.add(ChatMessage(prompt, true))
        _chatMessages.value = currentList
        _isChatLoading.value = true

        viewModelScope.launch {
            val answer = repository.getMythologyAnswer(prompt)
            val updatedList = _chatMessages.value.toMutableList()
            updatedList.add(ChatMessage(answer, false))
            _chatMessages.value = updatedList
            _isChatLoading.value = false
        }
    }

    fun clearChat() {
        _chatMessages.value = listOf(
            ChatMessage("Saddharanam! Welcome to DeviDevata Mythology AI guide. Ask me any question about Vedas, deities, mantra protocols, rituals or Puranic histories.", false)
        )
    }

    // --- Panchang & Notifications (Push alerts simulation) ---
    fun getPanchangDetailsForToday(): Map<String, String> {
        val formatter = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault())
        val dateStr = formatter.format(Date())

        // Calculate a simulated authentic Indian Tithi based on calendar days to look highly realistic
        val dayOfMonth = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_MONTH)
        val tithiList = listOf(
            "Shukla Prathama", "Shukla Dwitiya", "Shukla Tritiya", "Shukla Chaturthi",
            "Shukla Panchami", "Shukla Shashti", "Shukla Saptami", "Shukla Ashtami",
            "Shukla Navami", "Shukla Dashami", "Shukla Ekadasi", "Shukla Dwadashi",
            "Shukla Trayodashi", "Shukla Chaturdashi", "Purnima",
            "Krishna Prathama", "Krishna Dwitiya", "Krishna Tritiya", "Krishna Chaturthi",
            "Krishna Panchami", "Krishna Shashti", "Krishna Saptami", "Krishna Ashtami",
            "Krishna Navami", "Krishna Dashami", "Krishna Ekadasi", "Krishna Dwadashi",
            "Krishna Trayodashi", "Krishna Chaturdashi", "Amavasya"
        )
        val index = (dayOfMonth - 1) % tithiList.size
        val tithiValue = tithiList[index]

        val stars = listOf("Rohini", "Ashwini", "Krittika", "Mrigashirsha", "Ardra", "Punarvasu", "Pushya", "Ashlesha", "Magha", "Purva Phalguni", "Uttara Phalguni", "Hasta", "Chitra", "Swati", "Vishakha", "Anuradha", "Jyeshtha", "Mula", "Purva Ashadha", "Uttara Ashadha", "Shravana", "Dhanishta", "Shatabhisha", "Purva Bhadrapada", "Uttara Bhadrapada", "Revati")
        val starValue = stars[dayOfMonth % stars.size]

        return mapOf(
            "date" to dateStr,
            "tithi" to tithiValue,
            "nakshatra" to starValue,
            "ayana" to "Uttarayana (Northward Sun)",
            "samvat" to "Vikram Samvat 2083",
            "rahu_kaal" to "1:30 PM - 3:00 PM",
            "abhijit" to "11:50 AM - 12:40 PM"
        )
    }

    fun dispatchLocalNotification(title: String, body: String) {
        val context = getApplication<Application>()
        val channelId = "devidevata_notifications"

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_notification_overlay) // Fallback standard system decorative notification
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(System.currentTimeMillis().toInt(), builder.build())
    }

    private fun setupNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val context = getApplication<Application>()
            val channelId = "devidevata_notifications"
            val channelName = "Daily Divine Alerts & Reminders"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(channelId, channelName, importance).apply {
                description = "Channels reminders for Hindu festivals, daily Panchang tithis and puja chants."
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onCleared() {
        super.onCleared()
        textToSpeech?.stop()
        textToSpeech?.shutdown()
    }
}
