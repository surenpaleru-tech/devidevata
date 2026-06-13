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
import com.example.data.api.LocalLanguageConfig
import com.example.data.database.DivineDao
import com.example.data.database.Festival
import com.example.data.database.GodCategory
import com.example.data.database.GodImage
import com.example.data.database.StotramPuja
import com.example.data.database.TempleInfo
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.Date

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

        // Always check and ensure authentic Nirjala Ekadashi is loaded at its real date (June 25th)
        try {
            val correctNirjala = Festival(
                id = 508, title = "Nirjala Ekadashi", dateStr = "2026-06-25", dayOfWeek = "Thursday",
                Month = "Jyeshtha (Shukla Ekadashi)", tithi = "Shukla Paksha Ekadashi", nakshatra = "Swati",
                deityCategoryId = 3, description = "The most sacred, strict, and powerful waterless fast of the year. Destroys all negative karmic debris and elevates spiritual consciousness directly to Vaikuntha.",
                rituals = "Absolute waterless fast, continuous chanting of Vishnu Sahasranama, and distributing sweet water (Sherbet) and charity to those in need."
            )
            divineDao.insertFestivals(listOf(correctNirjala))
        } catch (e: Exception) {
            Log.e("DivineRepository", "Failed to insert correct Nirjala Ekadashi at June 25th", e)
        }

        // Pull the latest categories from the cloud server each time to keep server updates
        val sharedPrefs = context.getSharedPreferences("devi_devata_prefs", Context.MODE_PRIVATE)
        val defaultUrl = com.example.data.util.Obfuscator.getDecodedUrl()
        val serverUrl = sharedPrefs.getString("server_url", defaultUrl) ?: defaultUrl
        try {
            Log.d("DivineRepository", "Startup check: Pulling latest categories from cloud server $serverUrl")
            val response = fetchCombinedOrGranularSyncData(serverUrl)
            if (response.categories.isNotEmpty()) {
                // Keep local categories perfectly synchronized with server overrides
                divineDao.clearCategories()
                divineDao.insertCategories(response.categories.map {
                    GodCategory(
                        id = it.id,
                        name = it.name,
                        description = it.description,
                        thumbnail = it.thumbnail,
                        defaultColor = it.defaultColor
                    )
                })
                Log.d("DivineRepository", "Successfully updated ${response.categories.size} categories from cloud server.")
            }
        } catch (e: Exception) {
            Log.w("DivineRepository", "Could not synchronize categories from server. Falling back to local Room database cache.", e)
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
            // ================= SANSKRIT =================
            StotramPuja(
                id = 301, categoryId = 1, type = "MANTRA", title = "Ganesha Mula Mantra",
                sanskritText = "ॐ गं गणपतये नमः ॥\n\nOm Gam Ganapataye Namaha.",
                translation = "My salutations and surrender to Lord Ganesha, the lord of all Ganas and ruler of wisdom.",
                benefits = "Removes any obstacles from educational, vocational, or marital pathways and grants supreme mental clarity.",
                language = "Sanskrit"
            ),
            StotramPuja(
                id = 302, categoryId = 1, type = "STOTRAM", title = "Sankata Nashana Ganesha Stotram",
                sanskritText = "प्रणम्य शिरसा देवं गौरीपुत्रं विनायकम् ।\nभक्तावासं स्मरेन्नित्यमायुष्कामार्थसिद्धये ॥ १ ॥\nप्रथमं वक्रतुण्डं च एकदन्तं द्वितीयकम् ।\nतृतीयं कृष्णपिङ्गाक्षं गजवक्त्रं चतुर्थकम् ॥ २ ॥",
                translation = "The sage Narada explains the twelve holy names of Ganesha. Reciting this morning and evening destroys all griefs and troubles deeply.",
                benefits = "Grants child to the childless, wealth to the needy, and liberation to the seekers.",
                language = "Sanskrit"
            ),
            StotramPuja(
                id = 303, categoryId = 2, type = "STOTRAM", title = "Shiva Panchakshara Stotram",
                sanskritText = "नागेन्द्रहाराय त्रिलोचनाय\nभस्माङ्गरागाय महेश्वराय ।\nनित्याय शुद्घाय दिगम्बराय\nतस्मै नकाराय नमः शिवाय ॥ १ ॥\n\nमन्दाकिनीसलिलचन्दनचर्चिताय\nनन्दीश्वरप्रमथनाथमहेश्वराय ।\nमन्दारपुष्पबहुपुष्पसुपूजिताय\nतस्मै मकाराय नमः शिवाय ॥ २ ॥",
                translation = "Salutations to Shiva, who wears the king of snakes as his garland, who is three-eyed, whose pure body is smeared with sacred ash, the sovereign Lord representing structural nature.",
                benefits = "Chanting this purified hymn bestows deep concentration, inner peace, and aligns the elements of earth, water, fire, air, and space inside your yogic system.",
                language = "Sanskrit"
            ),
            StotramPuja(
                id = 304, categoryId = 3, type = "STOTRAM", title = "Madhurashtakam (Sweet Octet)",
                sanskritText = "अधरं मधुरं वदनं मधुरं नयनं मधुरं हसितं मधुरम् ।\nहृदयं मधुरं गमनं मधुरं मधुराधिपतेरखिलं मधुरम् ॥ १ ॥\n\nवचनं मधुरं चरितं मधुरं वसनं मधुरं वलितं मधुरम् ।\nचलितं मधुरं भ्रमितं मधुरं मधुराधिपतेरखिलं मधुरम् ॥ २ ॥",
                translation = "His lips are sweet, His face is sweet, His eyes are sweet, His smile is sweet. Everything about the King of Sweetness is utterly sweet!",
                benefits = "Attracts absolute pure cosmic love devotion (Bhakti), heals emotional trauma, and infuses sweet tranquility in the household.",
                language = "Sanskrit"
            ),
            StotramPuja(
                id = 305, categoryId = 4, type = "AARTI", title = "Ambe Tu Hai Jagdambe Aarti",
                sanskritText = "अम्बे तू है जगदम्बे काली, जय दुर्गे खप्पर वाली ।\nतेरे ही गुण गावें भारती, ओ मैया हम सब उतारे तेरी आरती ॥\n\nतेरे भक्त जनों पर मैया भीर पड़ी है भारी ।\nदानव दल पर टूट पड़ो मां करके सिंह सवारी ॥",
                translation = "O Mother Ambe! You are Goddess Kali, Victorious Durga who holds a trident and skull cup. All children of India sing your praise as we wave this light of worship.",
                benefits = "Destroys negative blockages, fills the mind with invincible strength, courage, and protects the yogi from dark intentions.",
                language = "Sanskrit"
            ),
            StotramPuja(
                id = 306, categoryId = 5, type = "STOTRAM", title = "Shree Ram Raksha Stotram",
                sanskritText = "चरितं रघुनाथस्य शतकोटि प्रविस्तरम् ।\nएकैकमक्षरं पुंसां महापातकनाशनम् ॥ १ ॥\n\nध्यात्वा नीलोत्पलश्यामं रामं राजीवलोचनम् ।\nजानकीलक्ष्मणोपेतं जटामुकुटमण्डितम् ॥ २ ॥",
                translation = "The story of Rama is detailed in billions of verses, yet every single word possesses the power to destroy the gravest sins.",
                benefits = "Surrounds the practitioner with an impenetrable shield of pure protection, wards off bad dreams, and clears dark obstacles.",
                language = "Sanskrit"
            ),
            StotramPuja(
                id = 307, categoryId = 2, type = "STOTRAM", title = "Shiva Tandava Stotram",
                sanskritText = "जटाटवीगलज्जलप्रवाहपावितस्थले\nगलेऽवलम्ब्य लम्बितां भुजङ्गतुङ्गमालिकाम् ।\nडमड्डमड्डमड्डमन्निनादवड्डमर्वयं\nचकार चण्डताण्डवं तनोतु नः शिवः शिवम् ॥ १ ॥",
                translation = "With his neck consecrated by the flow of water trickling from his forest of matted hair, wearing a lofty garland of serpents, and playing his damaru drum 'damat-damat', Shiva performs his dance of power.",
                benefits = "Increases vitality, removes planetary afflictions, improves deep voice projection and builds powerful cosmic magnetism.",
                language = "Sanskrit"
            ),
            StotramPuja(
                id = 308, categoryId = 3, type = "MANTRA", title = "Mahamantra Chanting",
                sanskritText = "हरे कृष्ण हरे कृष्ण कृष्ण कृष्ण हरे हरे ।\nहरे राम हरे राम राम राम हरे हरे ॥",
                translation = "Praising the supreme pleasure potency of God (Hara) along with Lord Krishna and Lord Rama.",
                benefits = "Directly links your soul with absolute spiritual ecstasy, resolves existential anxiety, and purifies karmic cycles.",
                language = "Sanskrit"
            ),

            // ================= TELUGU =================
            StotramPuja(
                id = 601, categoryId = 1, type = "MANTRA", title = "గణేష మూల మంత్రం",
                sanskritText = "ఓం గం గణపతయే నమః ॥\n\nOm Gam Ganapataye Namaha.",
                translation = "విఘ్నరాజైన శ్రీ గణేషునికి నా భక్తిపూర్వక నమస్కారాలు.",
                benefits = "సమస్త పనులలో ఆటంకాలను తొలగించి జయాన్ని చేకూరుస్తుంది.",
                language = "Telugu"
            ),
            StotramPuja(
                id = 602, categoryId = 1, type = "STOTRAM", title = "సంకట నాశన గణేశ స్తోత్రం",
                sanskritText = "ప్రణమ్య శిరసా దేవం గౌరీపుత్రం వినాయకమ్ ।\nభక్తావాసం స్మరేన్నిత్యమాయుష్కామార్థసిద్ధయే ॥ 1 ॥\nప్రథమం వక్రతుండం చ ఏకదంతం ద్వితీయకమ్ ।",
                translation = "నారద మహర్షి చెప్పిన వినాయకుని పన్నెండు పవిత్ర నామాలు స్మరించడం వల్ల సమస్త ఇబ్బందులు తొలగిపోతాయి.",
                benefits = "కీర్తి, సంపద, ఐశ్వర్యం లభించుటయే కాక భయాందోళనలు నశిస్తాయి.",
                language = "Telugu"
            ),
            StotramPuja(
                id = 603, categoryId = 2, type = "STOTRAM", title = "శివ పంచాక్షరి స్తోత్రం",
                sanskritText = "నాగేంద్రహారాయ త్రిలోచనాయ భస్మాంగరాగాయ మహేశ్వరాయ ।\nనిత్యాయ శుద్ధాయ దిగంబరాయ తస్మై నకారాయ నమః శివాయ ॥ 1 ॥",
                translation = "పాములను హారముగా ధరించి, మూడు కన్నులు కలిగి, పవిత్రమైన విభూతిని శరీరమంతటా అలంకరించుకున్న పరమశివునికి ప్రణామాలు.",
                benefits = "పంచభూతాలను నియంత్రించి, మానసిక అలజడిని తగ్గించి ప్రశాంతతను ఇస్తుంది.",
                language = "Telugu"
            ),
            StotramPuja(
                id = 604, categoryId = 3, type = "STOTRAM", title = "మధురాష్టకం (శ్రీ కృష్ణ స్తోత్రం)",
                sanskritText = "అధరం మధురం వదనం మధురం నయనం మధురం హసితం మధురమ్ ।\nహృదయం మధురం గమనం మధురం మధురాధిపతేరఖిలం మధురమ్ ॥ 1 ॥",
                translation = "శ్రీకృష్ణుని పెదవులు మధురం, ఆయన ముఖం మధురం, కనులు మధురం, ఆయన మందహాసం మధురం. మధుర నాయకుడైన శ్రీకృష్ణుని సమస్తం మధురం.",
                benefits = "ఇల్లంతటా హృదయపూర్వక సుఖశాంతులను నింపుతుంది మరియు మానసిక వత్తిడిని తగ్గిస్తుంది.",
                language = "Telugu"
            ),
            StotramPuja(
                id = 605, categoryId = 4, type = "AARTI", title = "అంబే తూ హై జగదంబే కాలీ హారతి",
                sanskritText = "అమ్బె తూ హై జగదమ్బె కాలీ, జయ దుర్గే ఖప్పర్ వాలీ।\nతేరే హీ గుణ్ గావే భారతమ్మ, ఓ మయ్యా హమ్ సబ్ ఉతారే తేరీ ఆరతీ ॥",
                translation = "ఓ జగన్మాతా దుర్గా దేవీ! నీవు సింహవాహినివై దుష్ట రాక్షసులను సంహరించావు. భక్తులు నీకు కర్పూర హారతిని సమర్పిస్తున్నారు.",
                benefits = "దుష్ట శక్తుల నుండి రక్షణ కల్పిస్తుంది, ధైర్యాన్ని మరియు కార్య విజయాన్ని చేకూరుస్తుంది.",
                language = "Telugu"
            ),
            StotramPuja(
                id = 606, categoryId = 5, type = "STOTRAM", title = "శ్రీ రామ రక్షా స్తోత్రం",
                sanskritText = "చరితం రఘునాథస్య శతకోటి ప్రవిస్తరమ్ ।\nఏకైకమక్షరం పుంసాం మహాపాతకనాశనమ్ ॥ 1 ॥",
                translation = "శ్రీరామచంద్రుని చరిత్ర నూరు కోట్ల శ్లోకాల విస్తీర్ణం కలది. దీనిలోని ప్రతి అక్షరం మహా పాపాలను నశింపజేస్తుంది.",
                benefits = "జీవిత రక్షణ కవచం వలె పనిచేస్తూ సర్వ భయాలను తొలగిస్తుంది.",
                language = "Telugu"
            ),
            StotramPuja(
                id = 607, categoryId = 2, type = "STOTRAM", title = "శివ తాండవ స్తోత్రం",
                sanskritText = "జటాటవీగలజ్జలప్రవాహపావితస్థలే\nగలేऽవలంబ్య లంబితాం భుజంగతుంగమాలికామ్ ।",
                translation = "జటాజూటం నుండి గంగ ప్రవహిస్తుండగా, మెడలో సర్పరాజాన్ని అలంకరించుకున్న పరమశివుడు చేసిన తాండవ నృత్య విహారం.",
                benefits = "శరీరంలో ఆత్మవిశ్వాసాన్ని నింపి, మానసిక స్థైర్యాన్ని మరియు తేజస్సును పెంపొందిస్తుంది.",
                language = "Telugu"
            ),
            StotramPuja(
                id = 608, categoryId = 3, type = "MANTRA", title = "హరే కృష్ణ మహా మంత్రం",
                sanskritText = "హరే కృష్ణ హరే కృష్ణ కృష్ణ కృష్ణ హరే హరే ।\nహరే రామ హరే రామ రామ రామ హరే హరే ॥",
                translation = "భగవంతుని దివ్య నామాలను జపించడం ద్వారా జీవుడు జన్మ మృత్యు బంధాల నుండి విముక్తి పొందుతాడు.",
                benefits = "మనస్సు ప్రశాంతమవుతుంది, జన్మ జన్మల కర్మలను ప్రక్షాళనం చేస్తుంది.",
                language = "Telugu"
            ),

            // ================= HINDI =================
            StotramPuja(
                id = 401, categoryId = 1, type = "MANTRA", title = "गणेश मूल मंत्र",
                sanskritText = "ॐ गं गणपतये नमः ॥\n\nOm Gam Ganapataye Namaha.",
                translation = "बाधाओं को हरने वाले और बुद्धि के स्वामी श्री गणेश को मेरा बारंबार प्रणाम।",
                benefits = "विद्या, बुद्धि, और व्यापार में आने वाली सभी अड़चनों को दूर करता है।",
                language = "Hindi"
            ),
            StotramPuja(
                id = 402, categoryId = 1, type = "STOTRAM", title = "संकटनाशन गणेश स्तोत्र",
                sanskritText = "प्रणम्य शिरसा देवं गौरीपुत्रं विनायकम् ।\nभक्तावासं स्मरेन्नित्यमायुष्कामार्थसिद्धये ॥ १ ॥",
                translation = "नारद पुराण से उद्धृत श्री गणेश के १२ नामों का स्मरण करने से जीवन के सभी दुख सदा के लिए समाप्त हो जाते हैं।",
                benefits = "गरीबों को धन, बीमार को स्वास्थ्य और विद्यार्थियों को विद्या प्राप्त होती है।",
                language = "Hindi"
            ),
            StotramPuja(
                id = 403, categoryId = 2, type = "STOTRAM", title = "शिव पंचाक्षर स्तोत्र",
                sanskritText = "नागेन्द्रहाराय त्रिलोचनाय भस्माङ्गरागाय महेश्वराय ।\nनित्याय शुद्घाय दिगम्बराय तस्मै नकाराय नमः शिवाय ॥ १ ॥",
                translation = "जो शिव कंठ में नागराज का हार पहनते हैं, तीन नेत्रों वाले हैं और भस्म रमाते हैं, उन शिव 'न' स्वरूप को नमस्कार।",
                benefits = "सभी इंद्रियों को शांत कर उत्तम स्वास्थ्य एवं एकाग्रता प्रदान करता है।",
                language = "Hindi"
            ),
            StotramPuja(
                id = 404, categoryId = 3, type = "STOTRAM", title = "मधुराष्टकम (श्री कृष्ण स्तुति)",
                sanskritText = "अधरं मधुरं वदनं मधुरं नयनं मधुरं हसितं मधुरम् ।\nहृदयं मधुरं गमनं मधुरं मधुराधिपतेरखिलं मधुरम् ॥ १ ॥",
                translation = "श्री कृष्ण का सब कुछ अत्यंत मीठा और आनंदमयी है - उनके होंठ, उनका चेहरा, उनकी आंखें और उनकी चाल।",
                benefits = "मन में शुद्ध प्रेम का संचार करता है और चिंता को दूर करता है।",
                language = "Hindi"
            ),
            StotramPuja(
                id = 405, categoryId = 4, type = "AARTI", title = "अम्बे तू है जगदम्बे आरती",
                sanskritText = "अम्बे तू है जगदम्बे काली, जय दुर्गे खप्पर वाली ।\nतेरे ही गुण गावें भारती, ओ मैया हम सब उतारे तेरी आरती ॥\n\nतेरे भक्त जनों पर मैया भीर पड़ी है भारी ।\nदानव दल पर टूट पड़ो मां करके सिंह सवारी ॥",
                translation = "हे माँ जगदम्बे, आप ही काली और महाशक्ति दुर्गा हो। हम सब आपकी आरती उतारते हैं।",
                benefits = "नकारात्मक तरंगों को नष्ट करता है और घर में सुख-समृद्धि लाता है।",
                language = "Hindi"
            ),
            StotramPuja(
                id = 406, categoryId = 5, type = "STOTRAM", title = "श्री राम रक्षा स्तोत्र",
                sanskritText = "चरितं रघुनाथस्य शतकोटि प्रविस्तरम् ।\nएकैकमक्षरं पुंसां महापातकनाशनम् ॥ १ ॥",
                translation = "श्री राम का चरित्र १०० करोड़ श्लोकों में वर्णित है, जिसका एक-एक अक्षर महान पापों का नाश करता है।",
                benefits = "यह एक अभेद्य सुरक्षा कवच है जो भय और अकाल मृत्यु से रक्षा करता है।",
                language = "Hindi"
            ),
            StotramPuja(
                id = 407, categoryId = 2, type = "STOTRAM", title = "शिव तांडव स्तोत्र",
                sanskritText = "जटाटवीगलज्जलप्रवाहपावितस्थले\nगलेऽवलम्ब्य लम्बितां भुजङ्गतुङ्गमालिकाम् ।",
                translation = "अपनी जटाओं से गंगा की धाराओं को पवित्र करने वाले और कंठ में नागमाला धारण करने वाले भगवान शिव का अद्भुत तांडव नृत्य।",
                benefits = "आत्मबल, तेज और समृद्धि बढ़ाता है। सभी ग्रहों के विपरीत प्रभाव शांत करता है।",
                language = "Hindi"
            ),
            StotramPuja(
                id = 408, categoryId = 3, type = "MANTRA", title = "महामंत्र संकीर्तन",
                sanskritText = "हरे कृष्ण हरे कृष्ण कृष्ण कृष्ण हरे हरे ।\nहरे राम हरे राम राम राम हरे हरे ॥",
                translation = "भगवान के परम पावन नाम का जप ही कलयुग के बंधनों से मुक्ति का एकमात्र साधन है।",
                benefits = "मानसिक अशांति और तनाव से तुरंत मुक्ति मिलती है।",
                language = "Hindi"
            ),

            // ================= ENGLISH =================
            StotramPuja(
                id = 501, categoryId = 1, type = "MANTRA", title = "Ganesha Root Chant",
                sanskritText = "Om Gam Ganapataye Namaha.",
                translation = "O Lord Ganesha, ruler of the universe and remover of obstacles, I bow down to you in complete surrender.",
                benefits = "Grants continuous success, destroys external blockages, and brings incredible intellect.",
                language = "English"
            ),
            StotramPuja(
                id = 502, categoryId = 1, type = "STOTRAM", title = "Sankata Nashana Ganesha Stotram",
                sanskritText = "Pranamya Shirasa Devam Gauri Putram Vinayakam...",
                translation = "By worshipping Gauri's son Ganesha daily, one achieves a long life and the fulfillment of all righteous desires.",
                benefits = "Directly solves severe life crises, grants steady wealth, and brings family peace.",
                language = "English"
            ),
            StotramPuja(
                id = 503, categoryId = 2, type = "STOTRAM", title = "Shiva Holy Syllable Hymn",
                sanskritText = "Nagendra Haraya Trilochanaya Bhasmangaragaya...",
                translation = "Sincere obeisance to Lord Shiva, decorated by the cobra garland, possessing three eyes, smeared in holy white ash, representing the prime syllable 'Na'.",
                benefits = "Establishes supreme mental focus and harmonizes energy pathways.",
                language = "English"
            ),
            StotramPuja(
                id = 504, categoryId = 3, type = "STOTRAM", title = "Madhurashtakam (Hymn of Sweetness)",
                sanskritText = "Adharam Madhuram Vadanam Madhuram Nayanam Madhuram...",
                translation = "Everything associated with Sri Krishna, the master of absolute sweetness, is exceptionally sweet - His lips, eyes, gait, and heart.",
                benefits = "Fills the heart with pure devotional bliss and heals psychological stress.",
                language = "English"
            ),
            StotramPuja(
                id = 505, categoryId = 4, type = "AARTI", title = "Mother Durga's Triumph Aarti",
                sanskritText = "Ambe Tu Hai Jagdambe Kali, Jai Durge Khappar Wali...",
                translation = "Glory to the protective cosmic Mother Durga, who rides a majestic lion and conquers evil forces to bless her devotees.",
                benefits = "Builds absolute inner strength, courage, and protects against malice or dark energy.",
                language = "English"
            ),
            StotramPuja(
                id = 506, categoryId = 5, type = "STOTRAM", title = "Sri Ram Protection Hymn",
                sanskritText = "Charitam Raghunathasya Shatakoti Pravistaram...",
                translation = "The legendary story of Rama spans billions of verses; reciting even a single letter dissolves the heaviest karmic baggage.",
                benefits = "Constructs an invisible protective energy dome around the practitioner.",
                language = "English"
            ),
            StotramPuja(
                id = 507, categoryId = 2, type = "STOTRAM", title = "Shiva Tandava Hymn of Power",
                sanskritText = "Jatatavigalajjala Pravahapavitasthale Galebavalambya...",
                translation = "Praising Shiva's ecstatic cosmic dance, with the flowing Ganga, the rhythm of his damaru drum, and his majestic crest crescent moon.",
                benefits = "Enhances personal magnetism, speech fluency, and shields the mind profoundly.",
                language = "English"
            ),
            StotramPuja(
                id = 508, categoryId = 3, type = "MANTRA", title = "Hare Krishna Maha Mantra",
                sanskritText = "Hare Krishna Hare Krishna Krishna Krishna Hare Hare, Hare Rama Hare Rama Rama Rama Hare Hare.",
                translation = "Chanting the ultimate holy names of God's pleasure energy to awaken spiritual self-realization.",
                benefits = "Breathes transcendent joy, purifies historical karma, and solves existential dread.",
                language = "English"
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
            ),
            Festival(
                id = 506, title = "Ganga Dussehra", dateStr = "2026-06-10", dayOfWeek = "Wednesday",
                Month = "Jyeshtha (Shukla Dashami)", tithi = "Shukla Dashami", nakshatra = "Hasta",
                deityCategoryId = 4, description = "The holy day when the celestial Goddess Ganga descended from Heaven to purify Earth.",
                rituals = "Take a holy bath in sacred rivers, light earthen lamps (diyas), chant Ganga Gayatri, and distribute water and charity."
            ),
            Festival(
                id = 508, title = "Nirjala Ekadashi", dateStr = "2026-06-25", dayOfWeek = "Thursday",
                Month = "Jyeshtha (Shukla Ekadashi)", tithi = "Shukla Paksha Ekadashi", nakshatra = "Swati",
                deityCategoryId = 3, description = "The most sacred, strict, and powerful waterless fast of the year. Destroys all negative karmic debris and elevates spiritual consciousness directly to Vaikuntha.",
                rituals = "Absolute waterless fast, continuous chanting of Vishnu Sahasranama, and distributing sweet water (Sherbet) and charity to those in need."
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
    private suspend fun fetchCombinedOrGranularSyncData(serverUrl: String): com.example.data.api.SyncResponse {
        val datasetUrl = if (serverUrl.endsWith("/")) "${serverUrl}sync.json" else "$serverUrl/sync.json"
        return try {
            Log.d("DivineRepository", "Attempting unified sync from $datasetUrl")
            apiService.syncAllData(datasetUrl)
        } catch (e: Exception) {
            Log.w("DivineRepository", "Unified sync.json not found, attempting modular separate JSON files fallback from $serverUrl", e)
            
            val categoriesUrl = if (serverUrl.endsWith("/")) "${serverUrl}categories.json" else "$serverUrl/categories.json"
            val imagesUrl = if (serverUrl.endsWith("/")) "${serverUrl}images.json" else "$serverUrl/images.json"
            val stotramsUrl = if (serverUrl.endsWith("/")) "${serverUrl}stotrams.json" else "$serverUrl/stotrams.json"
            val templesUrl = if (serverUrl.endsWith("/")) "${serverUrl}temples.json" else "$serverUrl/temples.json"
            val festivalsUrl = if (serverUrl.endsWith("/")) "${serverUrl}festivals.json" else "$serverUrl/festivals.json"
            
            val remoteCategories = try { apiService.getCategoriesList(categoriesUrl) } catch (ex: Exception) { Log.e("DivineRepository", "categories.json download failed", ex); emptyList() }
            val remoteImages = try { apiService.getImagesList(imagesUrl) } catch (ex: Exception) { Log.e("DivineRepository", "images.json download failed", ex); emptyList() }
            val remoteStotrams = try { apiService.getStotramsList(stotramsUrl) } catch (ex: Exception) { Log.e("DivineRepository", "stotrams.json download failed", ex); emptyList() }
            val remoteTemples = try { apiService.getTemplesList(templesUrl) } catch (ex: Exception) { Log.e("DivineRepository", "temples.json download failed", ex); emptyList() }
            val remoteFestivals = try { apiService.getFestivalsList(festivalsUrl) } catch (ex: Exception) { Log.e("DivineRepository", "festivals.json download failed", ex); emptyList() }
            
            if (remoteCategories.isEmpty() && remoteImages.isEmpty() && remoteStotrams.isEmpty() && remoteTemples.isEmpty() && remoteFestivals.isEmpty()) {
                throw Exception("All modular separate JSON files failed or are empty at $serverUrl", e)
            }
            
            com.example.data.api.SyncResponse(
                version = 1,
                categories = remoteCategories,
                images = remoteImages,
                stotrams = remoteStotrams,
                temples = remoteTemples,
                festivals = remoteFestivals
            )
        }
    }

    suspend fun syncDataFromServer(serverUrl: String): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d("DivineRepository", "Syncing with cloud server at $serverUrl")
            // Fetch configuration first
            val configUrl = if (serverUrl.endsWith("/")) "${serverUrl}config.json" else "$serverUrl/config.json"
            val config = try {
                apiService.getRemoteConfig(configUrl)
            } catch (e: Exception) {
                Log.w("DivineRepository", "Could not fetch custom config.json, using default fallback config.", e)
                ServerConfig()
            }
            
            // Save Remote config to SharedPreferences for LLM routing and Category Image settings
            val sharedPrefs = context.getSharedPreferences("devi_devata_prefs", Context.MODE_PRIVATE)
            sharedPrefs.edit()
                .putString("active_model", config.activeModel)
                .putString("llm_prompt", config.llmTuningPrompt)
                .putString("server_base_url", config.imgServerBaseUrl)
                .putString("push_alert", config.pushAlertMessage)
                .putString("llm_override_url", config.llmOverrideUrl)
                .apply()

            // Fetch whole synced dataset either in unified sync.json or modular separate JSONs
            val response = fetchCombinedOrGranularSyncData(serverUrl)

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
                    StotramPuja(it.id, it.categoryId, it.type, it.title, it.sanskritText, it.translation, it.benefits, it.language ?: "Sanskrit")
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

    // --- Pull and store language-specific Stotrams/Aartis/Mantras ---
    suspend fun syncStotramsByLanguage(serverUrl: String, language: String): Boolean = withContext(Dispatchers.IO) {
        // Pre-load local asset-based dynamic localization immediately for instant offline-ready response!
        val localLoaded = loadLanguageDataFromAssets(language)
        
        try {
            Log.d("DivineRepository", "Selective Sync: Fetching stotrams for language $language from $serverUrl")
            val langCode = language.lowercase(Locale.getDefault())
            
            // Try language-specific JSON first, e.g., sync_telugu.json, sync_hindi.json etc. 
            val datasetUrl = if (serverUrl.endsWith("/")) "${serverUrl}sync_$langCode.json" else "$serverUrl/sync_$langCode.json"
            val response = try {
                apiService.syncAllData(datasetUrl)
            } catch (e: Exception) {
                Log.w("DivineRepository", "Language-specific file ($datasetUrl) not found, using standard sync.json")
                val standardUrl = if (serverUrl.endsWith("/")) "${serverUrl}sync.json" else "$serverUrl/sync.json"
                apiService.syncAllData(standardUrl)
            }

            if (response.stotrams.isNotEmpty()) {
                val mappedStotrams = response.stotrams.map {
                    StotramPuja(
                        id = it.id,
                        categoryId = it.categoryId,
                        type = it.type,
                        title = it.title,
                        sanskritText = it.sanskritText,
                        translation = it.translation,
                        benefits = it.benefits,
                        language = it.language ?: language // Map retrieved items, default to the chosen language
                    )
                }
                
                // Keep pre-existing stotrams of OTHER languages, delete existing items of THIS language to prevent duplicates, then insert
                // But Dao's insertStotrams has REPLACE conflict strategy, which is extremely robust!
                // Let's filter stotram records in local db if needed, or simply let insertStotrams do REPLACE.
                // We will insert our newly fetched language-specific hymns cleanly.
                divineDao.insertStotrams(mappedStotrams)
            }
            true
        } catch (e: Exception) {
            Log.e("DivineRepository", "Special localized data sync failed.", e)
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

    private suspend fun prepopulateMultiLanguageStotrams() {
        return // Superseded by prepopulateStotrams()
        val list = listOf(
            // --- SANSKRIT ---
            StotramPuja(
                id = 301, categoryId = 1, type = "MANTRA", title = "Ganesha Mula Mantra",
                sanskritText = "ॐ गं गणपतये नमः ॥\n\nOm Gam Ganapataye Namaha.",
                translation = "My salutations and surrender to Lord Ganesha, the lord of all Ganas and ruler of wisdom.",
                benefits = "Removes any obstacles from educational, vocational, or marital pathways and grants supreme mental clarity.",
                language = "Sanskrit"
            ),
            StotramPuja(
                id = 302, categoryId = 1, type = "STOTRAM", title = "Sankata Nashana Ganesha Stotram",
                sanskritText = "प्रणम्य शिरसा देवं गौरीपुत्रं विनायकम् ।\nभक्तावासं स्मरेन्नित्यमायुष्कामार्थसिद्धये ॥ १ ॥\nप्रथमं वक्रतुण्डं च एकदन्तं द्वितीयकम् ।\nतृतीयं कृष्णपिङ्गाक्षं गजवक्त्रं चतुर्थकम् ॥ २ ॥",
                translation = "The sage Narada explains the twelve holy names of Ganesha. Reciting this morning and evening destroys all griefs and troubles deeply.",
                benefits = "Grants child to the childless, wealth to the needy, and liberation to the seekers.",
                language = "Sanskrit"
            ),
            StotramPuja(
                id = 303, categoryId = 2, type = "STOTRAM", title = "Shiva Panchakshara Stotram",
                sanskritText = "नागेन्द्रहाराय त्रिलोचनाय\nभस्माङ्गरागाय महेश्वराय ।\nनित्याय शुद्घाय दिगम्बराय\nतस्मै नकाराय नमः शिवाय ॥ १ ॥\n\nमन्दाकिनीसलिलचन्दनचर्चिताय\nनन्दीश्वरप्रमथनाथमहेश्वराय ।\nमन्दारपुष्पबहुपुष्पсуपूजिताय\nतस्मै मकाराय नमः शिवाय ॥ २ ॥",
                translation = "Salutations to Shiva, who wears the king of snakes as his garland, who is three-eyed, whose pure body is smeared with sacred ash, the sovereign Lord representing structural nature.",
                benefits = "Chanting this purified hymn bestows deep concentration, inner peace, and aligns the elements inside your yogic system.",
                language = "Sanskrit"
            ),
            StotramPuja(
                id = 304, categoryId = 3, type = "STOTRAM", title = "Madhurashtakam (Sweet Octet)",
                sanskritText = "अधरं मधुरं वदनं मधुरं नयनं मधुरं हसितं मधुरम् ।\nहृदयं मधुरं गमनं मधुरं मधुराधिपतेरखिलं मधुरम् ॥ १ ॥\n\nवचनं मधुरं चरितं मधुरं वसनं मधुरं वलितं मधुरम् ।\nचलितं मधुरं भ्रमितं मधुरं मधुराधिपतेरखिलं मधुरम् ॥ २ ॥",
                translation = "His lips are sweet, His face is sweet, His eyes are sweet, His smile is sweet. Everything about the King of Sweetness is utterly sweet!",
                benefits = "Attracts absolute pure cosmic love devotion (Bhakti), heals emotional trauma, and infuses sweet tranquility.",
                language = "Sanskrit"
            ),
            StotramPuja(
                id = 307, categoryId = 2, type = "STOTRAM", title = "Shiva Tandava Stotram",
                sanskritText = "जटाटवीगलज्जलप्रवाहपावितस्थле\nगलेऽवलम्ब्य लम्बितां भुजङ्गतुङ्गमालिकाम् ।\nडमड्डमड्डमड्डमन्निनादवड्डमर्वयं\nचकार चण्डताण्डवं तनोतु नः शिवः शिवम् ॥ १ ॥",
                translation = "With his neck consecrated by the flow of water trickling from his forest of matted hair, wearing a garland of serpents, Shiva performs his dance of power.",
                benefits = "Increases vitality, removes planetary afflictions, improves voice projection.",
                language = "Sanskrit"
            ),
            StotramPuja(
                id = 308, categoryId = 3, type = "MANTRA", title = "Mahamantra Chanting",
                sanskritText = "हरे कृष्ण हरे कृष्ण कृष्ण कृष्ण हरे हरे ।\nहरे राम हरे राम राम राम हरे हरे ॥",
                translation = "Praising the supreme pleasure potency of God (Hara) along with Lord Krishna and Lord Rama.",
                benefits = "Directly links your soul with absolute spiritual ecstasy, resolves anxiety, and purifies karma.",
                language = "Sanskrit"
            ),

            // --- HINDI ---
            StotramPuja(
                id = 305, categoryId = 4, type = "AARTI", title = "Ambe Tu Hai Jagdambe Aarti",
                sanskritText = "अम्बे तू है जगदम्बे काली, जय दुर्गे खप्पर वाली ।\nतेरे ही गुण गावें भारती, ओ मैया हम सब उतारे तेरी आरती ॥\n\nतेरे भक्त जनों पर मैया भीर पड़ी है भारी ।\nदानव दल पर टूट पड़ो मां करके सिंह सवारी ॥",
                translation = "ओ माँ अंबे! आप ही महाकाली और दुर्गा हैं। हम सब आपकी सुंदर आरती उतारते हैं। भक्त जनों की कठिनाइयों को दूर करो माँ।",
                benefits = "नकारात्मक शक्तियों का नाश होता है, सांसारिक भय से मुक्ति मिलती है और मन में दृढ़ विश्वास जागृत होता है।",
                language = "Hindi"
            ),
            StotramPuja(
                id = 310, categoryId = 3, type = "AARTI", title = "Aarti Kunj Bihari Ki",
                sanskritText = "आरती कुंजबिहारी की, श्री गिरिधर कृष्णमुरारी की ॥\nगले में बैजंती माला, बजावै मुरली मधुर बाला ।\nश्रवण में कुंडल झलकाला, नंद के आनंद नंदलाला की ॥",
                translation = "हम सब आपके कुंजों में निवास करने वाले प्यारे कृष्णमुरारी की आरती उतारते हैं जिन्होंने गोवर्धन पर्वत उठाया था।",
                benefits = "हृदय पवित्र और शांत बनता है तथा गृह क्लेशों और बाधाओं से मुक्ति मिलती है।",
                language = "Hindi"
            ),
            StotramPuja(
                id = 311, categoryId = 4, type = "MANTRA", title = "Durga Gayatri Mantra",
                sanskritText = "ॐ गिरिजायै विद्महे शिवप्रियायै धीमहि, तन्नो दुर्गा प्रचोदयात् ॥",
                translation = "हम माँ पार्वती शिवप्रिया को जानते हैं और उन पर ध्यान लगाते हैं। माँ दुर्गा हमारी बुद्धि को धर्म की ओर प्रेरित करें।",
                benefits = "तीव्र आत्मरक्षा, भय विनाश, सुख-समृद्धि और तेज की प्राप्ति।",
                language = "Hindi"
            ),

            // --- ENGLISH ---
            StotramPuja(
                id = 320, categoryId = 1, type = "MANTRA", title = "Lord Ganesha Divine Blessing",
                sanskritText = "Om Shreem Hreem Kleem Glaum Gam Ganapataye\nVara Varada Sarva Janam Me Vashamanaya Swaha.",
                translation = "May Lord Ganesha shower blessings, wealth, and dissolve all struggles. I surrender to his supreme intelligence.",
                benefits = "Brings continuous prosperity, clears professional disputes, and enhances focused intellect.",
                language = "English"
            ),
            StotramPuja(
                id = 321, categoryId = 2, type = "STOTRAM", title = "Shiva's Meditative Chant",
                sanskritText = "Om Namah Shivaya.\nPure cosmic vibration of absolute consciousness, non-dual bliss, and light.",
                translation = "I bow to the Supreme Teacher, Lord Shiva, who dwells in the temple of our hearts as silent peace.",
                benefits = "Heals old emotional distresses and brings transcendental stillness to the meditator.",
                language = "English"
            ),

            // --- TELUGU ---
            StotramPuja(
                id = 330, categoryId = 1, type = "MANTRA", title = "Ganesha Shobha Dandamu",
                sanskritText = "శుక్లాంబరధరం విష్ణుం శశివర్ణం చతుర్భుజం ।\nప్రసన్నవదనం ధ్యాయేత్ సర్వవిఘ్నోపశాంతయే ॥",
                translation = "తెల్లటి కాంతివంతమైన వస్త్రాలు ధరించి, ప్రశాంత ముఖము కలిగిన ఆ వినాయకుని విఘ్న నివారణకై ధ్యానిస్తున్నాను.",
                benefits = "చేపట్టిన పనులలో ఆటంకాలు తొలగిపోయి కార్యసిద్ధి చేకూరును.",
                language = "Telugu"
            ),
            StotramPuja(
                id = 331, categoryId = 2, type = "STOTRAM", title = "Siva Stuti Mallikarjuna",
                sanskritText = "నమశ్శివాయ శంభవే, శ్రీశైల మల్లికార్జునాయ వై ।\nకరుణాకరా కాయకల్ప, జటాజూట విభూషితాయ ॥",
                translation = "శ్రీశైల క్షేత్రమున వెలసిన కరుణామయుడు జటాజూటధరుడైన మల్లికార్జున స్వామికి నా నమస్కారాలు.",
                benefits = "ఆధ్యాత్మిక తేజస్సు, మనశ్శాంతి మరియు సంకల్ప బలం కలుగును.",
                language = "Telugu"
            )
        )
        divineDao.insertStotrams(list)
    }

    // --- Dynamic Asset-Driven Localization Loader ---
    suspend fun loadLanguageDataFromAssets(language: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val normalizedLang = language.lowercase(Locale.ROOT)
            val fileName = "localization/$normalizedLang.json"
            Log.d("DivineRepository", "Loading dynamic localization asset: $fileName")
            
            val jsonString = try {
                context.assets.open(fileName).bufferedReader().use { it.readText() }
            } catch (ex: Exception) {
                Log.w("DivineRepository", "Localization file $fileName not found, trying English fallback")
                try {
                    context.assets.open("localization/english.json").bufferedReader().use { it.readText() }
                } catch (e: Exception) {
                    Log.e("DivineRepository", "Fallback english.json failed to read", e)
                    return@withContext false
                }
            }
            
            val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
            val adapter = moshi.adapter(LocalLanguageConfig::class.java)
            val config = adapter.fromJson(jsonString)
            
            if (config != null) {
                if (config.stotrams.isNotEmpty()) {
                    val mappedStotrams = config.stotrams.map {
                        StotramPuja(
                            id = it.id,
                            categoryId = it.categoryId,
                            type = it.type,
                            title = it.title,
                            sanskritText = it.sanskritText,
                            translation = it.translation,
                            benefits = it.benefits,
                            language = language
                        )
                    }
                    divineDao.insertStotrams(mappedStotrams)
                }
                
                if (config.festivals.isNotEmpty()) {
                    val mappedFestivals = config.festivals.map {
                        Festival(
                            id = it.id,
                            title = it.title,
                            dateStr = it.dateStr,
                            dayOfWeek = it.dayOfWeek,
                            Month = it.Month,
                            tithi = it.tithi,
                            nakshatra = it.nakshatra,
                            deityCategoryId = it.deityCategoryId,
                            description = it.description,
                            rituals = it.rituals,
                            language = language
                        )
                    }
                    divineDao.insertFestivals(mappedFestivals)
                }
                Log.d("DivineRepository", "Successfully synchronized dynamic cached database from asset for $language!")
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e("DivineRepository", "Failed loading dynamic asset localized config for $language", e)
            false
        }
    }

    // --- Dynamic Content Addition ---
    suspend fun addCustomStotram(stotram: StotramPuja) = withContext(Dispatchers.IO) {
        divineDao.insertStotrams(listOf(stotram))
    }

    suspend fun addCustomFestival(festival: Festival) = withContext(Dispatchers.IO) {
        divineDao.insertFestivals(listOf(festival))
    }
}
