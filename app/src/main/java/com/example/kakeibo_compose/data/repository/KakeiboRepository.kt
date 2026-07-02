package com.example.kakeibo_compose.data.repository

import com.example.kakeibo_compose.data.entity.KakeiboEntity
import com.example.kakeibo_compose.data.local.KakeiboDao
import kotlinx.coroutines.flow.Flow

class KakeiboRepository(private val kakeiboDao: KakeiboDao) {
    val allItems: Flow<List<KakeiboEntity>> = kakeiboDao.getAllItems()
    val commonExpenseCategories: Flow<List<String>> = kakeiboDao.getCommonExpenseCategories()
    val commonIncomeCategories: Flow<List<String>> = kakeiboDao.getCommonIncomeCategories()

    suspend fun insert(item: KakeiboEntity) {
        kakeiboDao.insert(item)
    }
}