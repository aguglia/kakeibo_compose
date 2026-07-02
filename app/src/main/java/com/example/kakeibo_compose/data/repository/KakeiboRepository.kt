package com.example.kakeibo_compose.data.repository

import com.example.kakeibo_compose.data.entity.MiddleCategoryEntity
import com.example.kakeibo_compose.data.entity.SubCategoryEntity
import com.example.kakeibo_compose.data.entity.KakeiboEntity
import com.example.kakeibo_compose.data.entity.KakeiboDisplayItem
import com.example.kakeibo_compose.data.local.KakeiboDao
import kotlinx.coroutines.flow.Flow

class KakeiboRepository(private val kakeiboDao: KakeiboDao) {

    val allDisplayItems: Flow<List<KakeiboDisplayItem>> = kakeiboDao.getAllDisplayItems()
    val commonExpenseSubCategories: Flow<List<SubCategoryEntity>> = kakeiboDao.getCommonSubCategories(isIncome = false)
    val commonIncomeSubCategories: Flow<List<SubCategoryEntity>> = kakeiboDao.getCommonSubCategories(isIncome = true)

    fun getMiddleCategories(isIncome: Boolean): Flow<List<MiddleCategoryEntity>> = kakeiboDao.getMiddleCategories(isIncome)

    suspend fun insertKakeibo(item: KakeiboEntity) {
        kakeiboDao.insertKakeibo(item)
    }

    suspend fun insertMiddleCategory(cat: MiddleCategoryEntity): Long {
        return kakeiboDao.insertMiddleCategory(cat)
    }

    suspend fun insertSubCategory(subCat: SubCategoryEntity) {
        kakeiboDao.insertSubCategory(subCat)
    }

    // 重複チェックの仲介
    suspend fun isSubCategoryDuplicate(middleCategoryId: Int, name: String): Boolean {
        return kakeiboDao.getSubCategoryCount(middleCategoryId, name) > 0
    }

    suspend fun isMiddleCategoryDuplicate(isIncome: Boolean, name: String): Boolean {
        return kakeiboDao.getMiddleCategoryCount(isIncome, name) > 0
    }
}