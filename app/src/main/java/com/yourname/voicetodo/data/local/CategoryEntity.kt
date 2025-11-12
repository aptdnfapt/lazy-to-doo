package com.yourname.voicetodo.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String,    // Use provided ID, not auto-generated
    val name: String,              // "Work", "Life", "Study"
    val displayName: String,       // User-friendly name
    val color: String = "#137fec", // Hex color for UI
    val icon: String? = null,      // Optional emoji or icon name
    val sortOrder: Int = 0,        // Display order
    val isDefault: Boolean = false,// System default categories
    val createdAt: Long = System.currentTimeMillis()
)