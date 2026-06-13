package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "god_categories")
data class GodCategory(
    @PrimaryKey val id: Int,
    val name: String,
    val description: String,
    val thumbnail: String, // String representation: can be solid local fallback drawable ID string, or web URL
    val defaultColor: String // Hex string for theme (golden, spiritual saffron, celestial blue etc)
)

@Entity(tableName = "god_images")
data class GodImage(
    @PrimaryKey val id: Int,
    val categoryId: Int,
    val title: String,
    val url: String, // Cloud server URL
    val thumbUrl: String, // Optimized smaller image for grid
    val credit: String,
    val description: String,
    val isFavorite: Boolean = false,
    val downloadCount: Int = 0
)

@Entity(tableName = "stotrams_pujas")
data class StotramPuja(
    @PrimaryKey val id: Int,
    val categoryId: Int,
    val type: String, // "STOTRAM", "AARTI", "MANTRA"
    val title: String,
    val sanskritText: String, // Devanagari Sanskrit verses
    val translation: String, // Detailed English/Hindi translation
    val benefits: String, // Spiritual benefits of chanting
    val language: String = "Sanskrit" // e.g. "Sanskrit", "Hindi", "English", "Telugu"
)

@Entity(tableName = "temple_infos")
data class TempleInfo(
    @PrimaryKey val id: Int,
    val categoryId: Int,
    val name: String,
    val location: String,
    val history: String,
    val significance: String,
    val timing: String,
    val imageUrl: String
)

@Entity(tableName = "festivals")
data class Festival(
    @PrimaryKey val id: Int,
    val title: String,
    val dateStr: String, // YYYY-MM-DD
    val dayOfWeek: String,
    val Month: String, // e.g. "Ashadha / Ashvin"
    val tithi: String, // Today's tithi (e.g. "Shukla Ekadasi")
    val nakshatra: String, // Nakshatra
    val deityCategoryId: Int, // God connected to this festival
    val description: String,
    val rituals: String,
    val notificationSent: Boolean = false,
    val language: String = "English"
)
