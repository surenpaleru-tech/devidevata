package com.example.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DivineDao {

    // God Categories
    @Query("SELECT * FROM god_categories ORDER BY id ASC")
    fun getAllCategories(): Flow<List<GodCategory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<GodCategory>)

    @Query("DELETE FROM god_categories")
    suspend fun clearCategories()

    // God Images
    @Query("SELECT * FROM god_images WHERE categoryId = :categoryId ORDER BY id ASC")
    fun getImagesByCategory(categoryId: Int): Flow<List<GodImage>>

    @Query("SELECT * FROM god_images ORDER BY id ASC")
    fun getAllImages(): Flow<List<GodImage>>

    @Query("SELECT * FROM god_images WHERE isFavorite = 1")
    fun getFavoriteImages(): Flow<List<GodImage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImages(images: List<GodImage>)

    @Update
    suspend fun updateImage(image: GodImage)

    @Query("DELETE FROM god_images")
    suspend fun clearImages()

    // Stotrams & Pujas
    @Query("SELECT * FROM stotrams_pujas WHERE categoryId = :categoryId")
    fun getStotramsByGod(categoryId: Int): Flow<List<StotramPuja>>

    @Query("SELECT * FROM stotrams_pujas ORDER BY id ASC")
    fun getAllStotrams(): Flow<List<StotramPuja>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStotrams(stotrams: List<StotramPuja>)

    @Query("DELETE FROM stotrams_pujas")
    suspend fun clearStotrams()

    // Temples
    @Query("SELECT * FROM temple_infos WHERE categoryId = :categoryId")
    fun getTemplesByGod(categoryId: Int): Flow<List<TempleInfo>>

    @Query("SELECT * FROM temple_infos ORDER BY id ASC")
    fun getAllTemples(): Flow<List<TempleInfo>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemples(temples: List<TempleInfo>)

    @Query("DELETE FROM temple_infos")
    suspend fun clearTemples()

    // Festivals & Calendar
    @Query("SELECT * FROM festivals ORDER BY dateStr ASC")
    fun getAllFestivals(): Flow<List<Festival>>

    @Query("SELECT * FROM festivals WHERE dateStr = :dateStr LIMIT 1")
    suspend fun getFestivalByDate(dateStr: String): Festival?

    @Query("SELECT * FROM festivals WHERE deityCategoryId = :godId")
    fun getFestivalsByGod(godId: Int): Flow<List<Festival>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFestivals(festivals: List<Festival>)

    @Query("DELETE FROM festivals WHERE id = 507")
    suspend fun deleteDemoFestival()

    @Query("DELETE FROM festivals")
    suspend fun clearFestivals()
}
