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
        if (categoryDao.getCategoryById(id)?.isDefault == false) {
            categoryDao.deleteCategoryById(id)
        } else {
            throw IllegalArgumentException("Cannot delete default category")
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