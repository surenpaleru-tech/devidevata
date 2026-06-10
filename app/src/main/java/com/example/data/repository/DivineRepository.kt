package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.api.Content
import com.example.data.api.CustomChatRequest
import com.example.data.api.DeviDevataApiService
import com.example.data.api.GenerateContentRequest
import com.example.data.api.Part
import com.example.data.api.RetrofitClient
import com.example.data.api.ServerConfig
import com.example.data.database.DivineDao
import com.example.data.database.Festival
import com.example.data.database.GodCategory
import com.example.data.database.GodImage
import com.example.data.database.StotramPuja
import com.example.data.database.TempleInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class DivineRepository(
    val divineDao: DivineDao,
    private val apiService: DeviDevataApiService,
    private val context: Context
) {

    // --- Database Source of Truth Flows (Offline Caching) ---
    val allCategories: Flow<List<GodCategory>> = divineDao.getAllCategories()
    val allImages: Flow<List<GodImage>> = divineDao.getAllImages()
    val favoriteImages: Flow<List<GodImage>> = divineDao.getFavoriteImages()
    val allFestivals: Flow<List<Festival>> = divineDao.getAllFestivals()

    fun getImagesByGod(categoryId: Int): Flow<List<GodImage>> = divineDao.getImagesByCategory(categoryId)
    fun getStotramsByGod(categoryId: Int): Flow<List<StotramPuja>> = divineDao.getStotramsByGod(categoryId)
    fun getTemplesByGod(categoryId: Int): Flow<List<TempleInfo>> = divineDao.getTemplesByGod(categoryId)

    // --- Local Pre-population (Robust Fallback & Onboarding) ---
    suspend fun checkAndPrepopulateData() = withContext(Dispatchers.IO) {
        val existingCategories = allCategories.first()
        if (existingCategories.isEmpty()) {
            Log.d("DivineRepository", "Database empty. Prepopulating authentic local data.")
            prepopulateCategories()
            prepopulateImages()
            prepopulateStotrams()
            prepopulateTemples()
            prepopulateFestivals()
        }
    }

    private suspend fun prepopulateCategories() {
        val list = listOf(
            GodCategory(
                id = 1,
                name = "Lord Ganesha",
                description = "The remover of obstacles, patron of arts and sciences, and the deva of intellect and wisdom.",
                thumbnail = "https://images.unsplash.com/photo-1567591974573-ef3c14bab113?auto=format&fit=crop&q=80&w=400",
                defaultColor = "#FFA726" // Sacred Saffron Orange
            ),
            GodCategory(
                id = 2,
                name = "Lord Shiva",
                description = "The Destroyer, the Transformer, the Supreme Yogic Adiyogi dwelling in Kailash.",
                thumbnail = "https://images.unsplash.com/photo-1609137144813-7d722de15c7e?auto=format&fit=crop&q=80&w=400",
                defaultColor = "#26C6DA" // Celestial Teal Blue
            ),
            GodCategory(
                id = 3,
                name = "Lord Krishna",
                description = "The eighth avatar of Vishnu, counselor of the Bhagavad Gita, deity of compassion, love and joy.",
                thumbnail = "https://images.unsplash.com/photo-1597113366853-fc192b6149ef?auto=format&fit=crop&q=80&w=400",
                defaultColor = "#5C6BC0" // Spiritual Royal Blue
            ),
            GodCategory(
                id = 4,
                name = "Goddess Durga",
                description = "The supreme protective mother goddess, vanquisher of Mahishasura, representing cosmic feminine power.",
                thumbnail = "https://images.unsplash.com/photo-1634818556600-47660ca4a7db?auto=format&fit=crop&q=80&w=400",
                defaultColor = "#EF5350" // Divine Red Rose
            ),
            GodCategory(
                id = 5,
                name = "Lord Rama",
                description = "The Maryada Purushottam, ideal king, husband, personification of righteousness and avatar of Vishnu.",
                thumbnail = "https://images.unsplash.com/photo-1614850523459-c2f4c699c52e?auto=format&fit=crop&q=80&w=400",
                defaultColor = "#FFCA28" // Sacred Golden Yellow
            )
        )
        divineDao.insertCategories(list)
    }

    private suspend fun prepopulateImages() {
        // High-quality imagery targets matching categories.
        val list = listOf(
            // Ganesha (Category 1)
            GodImage(
                id = 101, categoryId = 1, title = "Golden Siddhivinayak Ganesha",
                url = "https://images.unsplash.com/photo-1567591974573-ef3c14bab113?auto=format&fit=crop&q=100&w=1200",
                thumbUrl = "https://images.unsplash.com/photo-1567591974573-ef3c14bab113?auto=format&fit=crop&q=80&w=400",
                credit = "Unsplash - Siddhi", description = "Gorgeous golden sculpted icon showing Vinayaka holding modak, representing success and pure auspiciousness."
            ),
            GodImage(
                id = 102, categoryId = 1, title = "Lord Ganesha in meditation",
                url = "https://images.unsplash.com/photo-1544735716-392fe2489ffa?auto=format&fit=crop&q=100&w=1200",
                thumbUrl = "https://images.unsplash.com/photo-1544735716-392fe2489ffa?auto=format&fit=crop&q=80&w=400",
                credit = "Unsplash - Ganesa", description = "Beautiful mud sculpture of Ganesha seated under temple arches radiating divine light and calm energy."
            ),
            // Shiva (Category 2)
            GodImage(
                id = 201, categoryId = 2, title = "Supreme Adiyogi Shiva",
                url = "https://images.unsplash.com/photo-1609137144813-7d722de15c7e?auto=format&fit=crop&q=100&w=1200",
                thumbUrl = "https://images.unsplash.com/photo-1609137144813-7d722de15c7e?auto=format&fit=crop&q=80&w=400",
                credit = "Unsplash - Adiyogi", description = "Statue of Adiyogi Lord Shiva sitting in profound deep meditation, holding his Trishul, representing cosmic consciousness."
            ),
            GodImage(
                id = 202, categoryId = 2, title = "Dhyana Shiva in Himalayas",
                url = "https://images.unsplash.com/photo-1594732159032-15967f631169?auto=format&fit=crop&q=100&w=1200",
                thumbUrl = "https://images.unsplash.com/photo-1594732159032-15967f631169?auto=format&fit=crop&q=80&w=400",
                credit = "Unsplash - Shiva", description = "Cosmic blue statue of Shiva resting by the Ganga, holding the damru with moon crested on lock, perfect for lockscren wallpaper."
            ),
            // Krishna (Category 3)
            GodImage(
                id = 301, categoryId = 3, title = "Lord Krishna with Murali",
                url = "https://images.unsplash.com/photo-1597113366853-fc192b6149ef?auto=format&fit=crop&q=100&w=1200",
                thumbUrl = "https://images.unsplash.com/photo-1597113366853-fc192b6149ef?auto=format&fit=crop&q=80&w=400",
                credit = "Unsplash - Flute", description = "Lord Krishna playing his mystical flute, attracting cowherd maidens, wearing peacock feather, emanating absolute bliss."
            ),
            GodImage(
                id = 302, categoryId = 3, title = "Sri Radha Krishna Divine Love",
                url = "https://images.unsplash.com/photo-1631527375253-df9ce9a263ba?auto=format&fit=crop&q=100&w=1200",
                thumbUrl = "https://images.unsplash.com/photo-1631527375253-df9ce9a263ba?auto=format&fit=crop&q=80&w=400",
                credit = "Unsplash - Love", description = "Vibrant artistic painting of Radha and Krishna sharing an umbrella in dynamic monsoon rains, conveying celestial yoga."
            ),
            // Durga (Category 4)
            GodImage(
                id = 401, categoryId = 4, title = "Ma Durga Mahishasuramardini",
                url = "https://images.unsplash.com/photo-1634818556600-47660ca4a7db?auto=format&fit=crop&q=100&w=1200",
                thumbUrl = "https://images.unsplash.com/photo-1634818556600-47660ca4a7db?auto=format&fit=crop&q=80&w=400",
                credit = "Unsplash - Shakti", description = "Dynamic statue of Durga riding her lion, holding nine cosmic weapons of destruction, victorious over demonic forces."
            ),
            // Rama (Category 5)
            GodImage(
                id = 501, categoryId = 5, title = "Ayodhya Sri Ram Lalla",
                url = "https://images.unsplash.com/photo-1614850523459-c2f4c699c52e?auto=format&fit=crop&q=100&w=1200",
                thumbUrl = "https://images.unsplash.com/photo-1614850523459-c2f4c699c52e?auto=format&fit=crop&q=80&w=400",
                credit = "Unsplash - Ayodhya", description = "A majestic depiction of Ram Lalla holding his cosmic Kodanda bow, radiating gentle righteousness."
            )
        )
        divineDao.insertImages(list)
    }

    private suspend fun prepopulateStotrams() {
        val list = listOf(
            StotramPuja(
                id = 301, categoryId = 1, type = "MANTRA", title = "Ganesha Mula Mantra",
                sanskritText = "ॐ गं गणपतये नमः ॥\n\nOm Gam Ganapataye Namaha.",
                translation = "My salutations and surrender to Lord Ganesha, the lord of all Ganas and ruler of wisdom.",
                benefits = "Removes any obstacles from educational, vocational, or marital pathways and grants supreme mental clarity."
            ),
            StotramPuja(
                id = 302, categoryId = 1, type = "STOTRAM", title = "Sankata Nashana Ganesha Stotram",
                sanskritText = "प्रणम्य शिरसा देवं गौरीपुत्रं विनायकम् ।\nभक्तावासं स्मरेन्नित्यमायुष्कामार्थसिद्धये ॥ १ ॥\nप्रथमं वक्रतुण्डं च एकदन्तं द्वितीयकम् ।\nतृतीयं कृष्णपिङ्गाक्षं गजवक्त्रं चतुर्थकम् ॥ २ ॥",
                translation = "The sage Narada explains the twelve holy names of Ganesha. Reciting this morning and evening destroys all griefs and troubles deeply.",
                benefits = "Grants child to the childless, wealth to the needy, and liberation to the seekers."
            ),
            StotramPuja(
                id = 303, categoryId = 2, type = "STOTRAM", title = "Shiva Panchakshara Stotram",
                sanskritText = "नागेन्द्रहाराय त्रिलोचनाय\nभस्माङ्गरागाय महेश्वराय ।\nनित्याय शुद्घाय दिगम्बराय\nतस्मै नकाराय नमः शिवाय ॥ १ ॥\n\nमन्दाकिनीसलिलचन्दनचर्चिताय\nनन्दीश्वरप्रमथनाथमहेश्वराय ।\nमन्दारपुष्पबहुपुष्पसुपूजिताय\nतस्मै मकाराय नमः शिवाय ॥ २ ॥",
                translation = "Salutations to Shiva, who wears the king of snakes as his garland, who is three-eyed, whose pure body is smeared with sacred ash, the sovereign Lord representing structural nature.",
                benefits = "Chanting this purified hymn bestows deep concentration, inner peace, and aligns the elements of earth, water, fire, air, and space inside your yogic system."
            ),
            StotramPuja(
                id = 304, categoryId = 3, type = "STOTRAM", title = "Madhurashtakam (Sweet Octet)",
                sanskritText = "अधरं मधुरं वदनं मधुरं नयनं मधुरं हसितं मधुरम् ।\nहृदयं मधुरं गमनं मधुरं मधुराधिपतेरखिलं मधुरम् ॥ १ ॥\n\nवचनं मधुरं चरितं मधुरं वसनं मधुरं वलितं मधुरम् ।\nचलितं मधुरं भ्रमितं मधुरं मधुराधिपतेरखिलं मधुरम् ॥ २ ॥",
                translation = "His lips are sweet, His face is sweet, His eyes are sweet, His smile is sweet. Everything about the King of Sweetness is utterly sweet!",
                benefits = "Attracts absolute pure cosmic love devotion (Bhakti), heals emotional trauma, and infuses sweet tranquility in the household."
            ),
            StotramPuja(
                id = 305, categoryId = 4, type = "AARTI", title = "Ambe Tu Hai Jagdambe Aarti",
                sanskritText = "अम्बे तू है जगदम्बे काली, जय दुर्गे खप्पर वाली ।\nतेरे ही गुण गावें भारती, ओ मैया हम सब उतारे तेरी आरती ॥\n\nतेरे भक्त जनों पर मैया भीर पड़ी है भारी ।\nदानव दल पर टूट पड़ो मां करके सिंह सवारी ॥",
                translation = "O Mother Ambe! You are Goddess Kali, Victorious Durga who holds a trident and skull cup. All children of India sing your praise as we wave this light of worship.",
                benefits = "Destroys negative blockages, fills the mind with invincible strength, courage, and protects the yogi from dark intentions."
            )
        )
        divineDao.insertStotrams(list)
    }

    private suspend fun prepopulateTemples() {
        val list = listOf(
            TempleInfo(
                id = 401, categoryId = 1, name = "Shree Siddhivinayak Temple",
                location = "Prabhadevi, Mumbai, Maharashtra",
                history = "Constructed originally on November 19, 1801 by Lakshman Vithu and Deubai Patil. It evolved from a tiny brick shrine into one of the richest, most popular temples globally.",
                significance = "The trunk of Ganesha here turns to the right (Siddhi Vinayaka), which requires very strict, pure worship rituals, but instantly fulfills pure desires.",
                timing = "Daily: 5:30 AM - 10:00 PM. Special Kakad Aarti: Tuesdays 4:30 AM.",
                imageUrl = "https://images.unsplash.com/photo-1544735716-392fe2489ffa?auto=format&fit=crop&q=80&w=400"
            ),
            TempleInfo(
                id = 402, categoryId = 2, name = "Kedarnath Jyotirlinga",
                location = "Garhwal Himalayan Range, Uttarakhand",
                history = "Built originally by the Pandavas to atone for Mahabharat war. Later revived by Adi Shankaracharya in 8th century CE. It survived a massive glacier flood in 2013 untouched.",
                significance = "One of the 12 sacred Jyotirlingas, located at 11,755 ft altitude. Represents absolute detachment, yogic power, and destruction of ego.",
                timing = "Opens May to November. Daily: 4:00 AM - 9:00 PM.",
                imageUrl = "https://images.unsplash.com/photo-1594732159032-15967f631169?auto=format&fit=crop&q=80&w=400"
            ),
            TempleInfo(
                id = 403, categoryId = 3, name = "Prem Mandir",
                location = "Vrindavan, Mathura, Uttar Pradesh",
                history = "Inaugurated in February 2012, created under Jagadguru Kripalu Parishat entirely out of Italian white Carrara marble reflecting unparalleled artistic carving.",
                significance = "A monument of divine love displaying exquisite dioramas depicting Lord Krishna's youthful pastimes (Ras Leela & Govardhana Lila).",
                timing = "Daily: 8:30 AM - 12:00 PM, 4:30 PM - 8:30 PM. Musical Fountain shows at 7:00 PM.",
                imageUrl = "https://images.unsplash.com/photo-1631527375253-df9ce9a263ba?auto=format&fit=crop&q=80&w=400"
            )
        )
        divineDao.insertTemples(list)
    }

    private suspend fun prepopulateFestivals() {
        val list = listOf(
            Festival(
                id = 501, title = "Maha Shivaratri", dateStr = "2026-02-15", dayOfWeek = "Sunday",
                Month = "Phalguna (Krishna Chaturdashi)", tithi = "Krishna Paksha Chaturdashi", nakshatra = "Shatabhisha",
                deityCategoryId = 2, description = "The great night of Shiva celebrating his cosmic dance of creation and marriage to Parvati.",
                rituals = "Keep rigorous fast, visual vigil (Jagaran), and pour sacred milk, honey, bael leaves over the Shiva Lingam at midnight."
            ),
            Festival(
                id = 502, title = "Rama Navami", dateStr = "2026-03-27", dayOfWeek = "Friday",
                Month = "Chaitra (Shukla Navami)", tithi = "Shukla Navami", nakshatra = "Punarvasu",
                deityCategoryId = 5, description = "Commemorates the birth incarnation of Lord Sri Rama in Ayodhya.",
                rituals = "Continuous chanting of Ram Nama, singing Ramacharitamanas, and decorating shrines with holy flags."
            ),
            Festival(
                id = 503, title = "Janmashtami", dateStr = "2026-09-03", dayOfWeek = "Thursday",
                Month = "Bhadrapada (Krishna Ashtami)", tithi = "Krishna Paksha Ashtami", nakshatra = "Rohini",
                deityCategoryId = 3, description = "Celebrates the mystical birth of the dark Lord Krishna in Mathura prison at midnight.",
                rituals = "Fasting until midnight birth, swinging infant Laddu Gopal deities, singing bhajans, and Dahi Handi competitions."
            ),
            Festival(
                id = 504, title = "Ganesh Chaturthi", dateStr = "2026-09-15", dayOfWeek = "Tuesday",
                Month = "Bhadrapada (Shukla Chaturthi)", tithi = "Shukla Paksha Chaturthi", nakshatra = "Hasta",
                deityCategoryId = 1, description = "Arrival festival of Vinayakar down from the celestial abode to bless earth with wisdom.",
                rituals = "Enshrining Ganesha clay idols, offering continuous 21 modaks, chanting Atharvashirsha and immersion (Visarjan)."
            ),
            Festival(
                id = 505, title = "Vijayadashami / Dussehra", dateStr = "2026-10-20", dayOfWeek = "Tuesday",
                Month = "Ashvina (Shukla Dashami)", tithi = "Shukla Dashami", nakshatra = "Shravana",
                deityCategoryId = 4, description = "Mother Durga vanquishes buffalo demon Mahishasura, and Lord Rama slays ten-headed Ravana.",
                rituals = "Burning of Ravana effigies, worshipping work tools, books, and performing Shami tree worship."
            )
        )
        divineDao.insertFestivals(list)
    }

    // --- Wallpaper setting and Download (Storage/WallpaperManager) ---
    suspend fun updateImageDownloadCount(image: GodImage) = withContext(Dispatchers.IO) {
        val updated = image.copy(downloadCount = image.downloadCount + 1)
        divineDao.updateImage(updated)
    }

    suspend fun toggleFavorite(image: GodImage) = withContext(Dispatchers.IO) {
        val updated = image.copy(isFavorite = !image.isFavorite)
        divineDao.updateImage(updated)
    }

    // --- Synchronizing with Cloud API Server (Reliable Endpoint & Config mapping) ---
    suspend fun syncDataFromServer(serverUrl: String): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d("DivineRepository", "Syncing with cloud server at $serverUrl")
            // Fetch configuration first
            val configUrl = if (serverUrl.endsWith("/")) "${serverUrl}config.json" else "$serverUrl/config.json"
            val config = apiService.getRemoteConfig(configUrl)
            
            // Save Remote config to SharedPreferences for LLM routing and Category Image settings
            val sharedPrefs = context.getSharedPreferences("devi_devata_prefs", Context.MODE_PRIVATE)
            sharedPrefs.edit()
                .putString("active_model", config.activeModel)
                .putString("llm_prompt", config.llmTuningPrompt)
                .putString("server_base_url", config.imgServerBaseUrl)
                .putString("push_alert", config.pushAlertMessage)
                .putString("llm_override_url", config.llmOverrideUrl)
                .apply()

            // Fetch whole synced dataset
            val datasetUrl = if (serverUrl.endsWith("/")) "${serverUrl}sync.json" else "$serverUrl/sync.json"
            val response = apiService.syncAllData(datasetUrl)

            // Overwrite database to guarantee extreme clean performance and caching!
            if (response.categories.isNotEmpty()) {
                divineDao.clearCategories()
                divineDao.insertCategories(response.categories.map {
                    GodCategory(it.id, it.name, it.description, it.thumbnail, it.defaultColor)
                })
            }
            if (response.images.isNotEmpty()) {
                divineDao.clearImages()
                divineDao.insertImages(response.images.map {
                    GodImage(it.id, it.categoryId, it.title, it.url, it.thumbUrl, it.credit, it.description)
                })
            }
            if (response.stotrams.isNotEmpty()) {
                divineDao.clearStotrams()
                divineDao.insertStotrams(response.stotrams.map {
                    StotramPuja(it.id, it.categoryId, it.type, it.title, it.sanskritText, it.translation, it.benefits)
                })
            }
            if (response.temples.isNotEmpty()) {
                divineDao.clearTemples()
                divineDao.insertTemples(response.temples.map {
                    TempleInfo(it.id, it.categoryId, it.name, it.location, it.history, it.significance, it.timing, it.imageUrl)
                })
            }
            if (response.festivals.isNotEmpty()) {
                divineDao.clearFestivals()
                divineDao.insertFestivals(response.festivals.map {
                    Festival(it.id, it.title, it.dateStr, it.dayOfWeek, it.Month, it.tithi, it.nakshatra, it.deityCategoryId, it.description, it.rituals)
                })
            }

            Log.d("DivineRepository", "Successfully synchronized all cloud server data!")
            true
        } catch (e: Exception) {
            Log.e("DivineRepository", "Failed server sync, falling back to local cached database.", e)
            false
        }
    }

    // --- Mythology AI Chat answering (Direct Gemini REST implementation) ---
    suspend fun getMythologyAnswer(userPrompt: String): String = withContext(Dispatchers.IO) {
        val sharedPrefs = context.getSharedPreferences("devi_devata_prefs", Context.MODE_PRIVATE)
        val activeModel = sharedPrefs.getString("active_model", "gemini-3.5-flash") ?: "gemini-3.5-flash"
        val systemPrompt = sharedPrefs.getString("llm_prompt", "You are an expert Vedic scholar. Answer queries about Indian Gods and mythology.") ?: "You are an expert Vedic scholar."
        val overrideUrl = sharedPrefs.getString("llm_override_url", null)

        // If admin set a custom router on their Cloud Admin panel:
        if (!overrideUrl.isNullOrEmpty()) {
            try {
                Log.d("DivineRepository", "Routing LLM query to Cloud Gateway: $overrideUrl")
                val response = apiService.generateCustomServerContent(
                    url = overrideUrl,
                    request = CustomChatRequest(userPrompt, systemPrompt, activeModel)
                )
                return@withContext response.response
            } catch (e: Exception) {
                Log.e("DivineRepository", "Custom LLM endpoint failed, falling back to Gemini direct REST.", e)
            }
        }

        // Direct Gemini REST flow (Option B in gemini-api skill)
        val apiKey = com.example.BuildConfig.GEMINI_API_KEY
        if (apiKey == "MY_GEMINI_API_KEY" || apiKey.isBlank()) {
            return@withContext "Mythology AI Key is not configured yet. Please configure it in the AI Studio Secrets panel."
        }

        // Ensure we strictly follow supported models listed in the gemini-api skill:
        // 'gemini-3.5-flash' is standard. Let's make sure we query a valid preview model.
        val targetModel = when (activeModel.lowercase()) {
            "gemini-3.1-pro-preview", "pro" -> "gemini-3.1-pro-preview"
            "gemini-3.5-flash", "flash", "default" -> "gemini-3.5-flash"
            else -> "gemini-3.5-flash"
        }

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = userPrompt)))),
            systemInstruction = Content(parts = listOf(Part(text = systemPrompt)))
        )

        try {
            val response = apiService.generateGeminiContent(targetModel, apiKey, request)
            response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text 
                ?: "No response from AI spiritual guide. Please check your connectivity and try again."
        } catch (e: Exception) {
            Log.e("DivineRepository", "Gemini query errored: ${e.message}", e)
            "Divine connection is broken: ${e.localizedMessage}. Please verify your Gemini API key inside AI Studio secrets."
        }
    }
}
