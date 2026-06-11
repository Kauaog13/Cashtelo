package com.cashtelo.data.repository

import androidx.lifecycle.LiveData
import com.cashtelo.data.dao.CategoryDao
import com.cashtelo.data.entity.Category

class CategoryRepository(private val dao: CategoryDao) {
    val allCategories: LiveData<List<Category>> = dao.getAllCategories()

    suspend fun insert(category: Category) = dao.insert(category)
    suspend fun update(category: Category) = dao.update(category)
    suspend fun delete(category: Category) = dao.delete(category)
}
