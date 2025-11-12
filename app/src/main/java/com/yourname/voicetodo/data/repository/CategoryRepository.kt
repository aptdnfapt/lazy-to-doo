package com.yourname.voicetodo.data.repository

import com.yourname.voicetodo.data.local.CategoryDao
import com.yourname.voicetodo.data.local.CategoryEntity
import com.yourname.voicetodo.domain.model.Category
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject

class CategoryRepository @Inject constructor(
    private val categoryDao: CategoryDao
) {
    fun getAllCategories(): Flow<List<Category>> = categoryDao.getAllCategoriesWithCount()
        .map { entities -> entities.map { it.toDomainModel() } }

    fun getDefaultCategories(): Flow<List<Category>> = categoryDao.getDefaultCategories()
        .map { entities -> entities.map { it.toDomainModel() } }

    suspend fun getCategoryById(id: String): Category? =
        categoryDao.getCategoryById(id)?.toDomainModel()

    suspend fun createCategory(name: String, displayName: String, color: String, icon: String?): Category {
        val entity = CategoryEntity(
            id = name.lowercase(), // Use name as ID for consistency
            name = name.uppercase(),
            displayName = displayName,
            color = color,
            icon = icon
        )
        categoryDao.insertCategory(entity)
        return entity.toDomainModel()
    }

    suspend fun updateCategory(category: Category) {
        categoryDao.updateCategory(category.toEntity())
    }

    suspend fun deleteCategory(id: String) {
        categoryDao.deleteCategoryById(id)
    }

    // Create default categories with hardcoded IDs
    suspend fun createDefaultCategoriesIfNeeded() {
        try {
            // Create default categories with hardcoded IDs
            val defaultCategories = listOf(
                CategoryEntity(
                    id = "work",
                    name = "WORK",
                    displayName = "Work",
                    color = "#137fec",
                    sortOrder = 0,
                    isDefault = true
                ),
                CategoryEntity(
                    id = "life",
                    name = "LIFE",
                    displayName = "Life",
                    color = "#4caf50",
                    sortOrder = 1,
                    isDefault = true
                ),
                CategoryEntity(
                    id = "study",
                    name = "STUDY",
                    displayName = "Study",
                    color = "#ff9800",
                    sortOrder = 2,
                    isDefault = true
                )
            )
            defaultCategories.forEach { category ->
                categoryDao.insertCategory(category)
            }
        } catch (e: Exception) {
            // Categories already exist, ignore
        }
    }
}

// Extension functions for entity-domain conversion
private fun CategoryEntity.toDomainModel(): Category {
    return Category(
        id = id,
        name = name,
        displayName = displayName,
        color = color,
        icon = icon,
        sortOrder = sortOrder,
        isDefault = isDefault,
        todoCount = 0
    )
}

// Extension function for CategoryWithCount conversion
private fun CategoryDao.CategoryWithCount.toDomainModel(): Category {
    return Category(
        id = id,
        name = name,
        displayName = displayName,
        color = color,
        icon = icon,
        sortOrder = sortOrder,
        isDefault = isDefault,
        todoCount = todoCount
    )
}

private fun Category.toEntity(): CategoryEntity {
    return CategoryEntity(
        id = id,
        name = name,
        displayName = displayName,
        color = color,
        icon = icon,
        sortOrder = sortOrder,
        isDefault = isDefault,
        createdAt = System.currentTimeMillis()  // TODO: Preserve original createdAt
    )
}