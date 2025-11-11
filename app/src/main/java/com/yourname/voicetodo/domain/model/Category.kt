package com.yourname.voicetodo.domain.model

data class Category(
    val id: String,
    val name: String,
    val displayName: String,
    val color: String = "#137fec",
    val icon: String? = null,
    val sortOrder: Int = 0,
    val isDefault: Boolean = false,
    val todoCount: Int = 0  // Computed property
)