package com.example.kakeibo_compose.data.repository

import com.example.kakeibo_compose.data.entity.BudgetEntity
import com.example.kakeibo_compose.data.entity.BudgetHistoryEntity
import com.example.kakeibo_compose.data.entity.CategorySelectionItem
import com.example.kakeibo_compose.data.entity.MiddleCategoryEntity
import com.example.kakeibo_compose.data.entity.MiddleCategoryWithBudget
import com.example.kakeibo_compose.data.entity.SubCategoryEntity
import com.example.kakeibo_compose.data.entity.KakeiboEntity
import com.example.kakeibo_compose.data.entity.KakeiboDisplayItem
import com.example.kakeibo_compose.data.entity.TargetEntity
import com.example.kakeibo_compose.data.local.KakeiboDao
import com.example.kakeibo_compose.data.local.KakeiboMonthSummary
import com.example.kakeibo_compose.data.local.MonthlyAchievementItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class KakeiboRepository(private val kakeiboDao: KakeiboDao) {

    // ==========================================
    // ユーザー様の既存のコード（完全保持）
    // ==========================================
    val allDisplayItems: Flow<List<KakeiboDisplayItem>> = kakeiboDao.getAllDisplayItems()
    val commonExpenseSubCategories: Flow<List<SubCategoryEntity>> = kakeiboDao.getCommonSubCategories(isIncome = false)
    val commonIncomeSubCategories: Flow<List<SubCategoryEntity>> = kakeiboDao.getCommonSubCategories(isIncome = true)

    fun getMiddleCategories(isIncome: Boolean): Flow<List<MiddleCategoryEntity>> = kakeiboDao.getMiddleCategories(isIncome)

    suspend fun deleteKakeiboById(id: Int) {
        kakeiboDao.deleteKakeiboById(id)
    }

    suspend fun updateKakeiboFull(id: Int, amount: Int, memo: String, subCategoryId: Int) {
        kakeiboDao.updateKakeiboFull(id, amount, memo, subCategoryId)
    }

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
    suspend fun isSubCategoryDuplicate(middleCategoryId: Int, name: String, excludeId: Int): Boolean {
        return kakeiboDao.getSubCategoryCount(middleCategoryId, name, excludeId) > 0
    }

    suspend fun isMiddleCategoryDuplicate(isIncome: Boolean, name: String, excludeId: Int): Boolean {
        return kakeiboDao.getMiddleCategoryCount(isIncome, name, excludeId) > 0
    }

    fun getSubCategoriesByMiddle(middleCategoryId: Int): Flow<List<SubCategoryEntity>> = kakeiboDao.getSubCategoriesByMiddle(middleCategoryId)

    suspend fun updateMiddleCategoryName(id: Int, newName: String) { kakeiboDao.updateMiddleCategoryName(id, newName) }
    suspend fun updateSubCategoryName(id: Int, newName: String) { kakeiboDao.updateSubCategoryName(id, newName) }



    fun getCategoriesSortedByUsage(isIncome: Boolean): Flow<List<CategorySelectionItem>> {
        return kakeiboDao.getCategoriesSortedByUsage(isIncome)
    }

    val allBudgets: Flow<List<BudgetEntity>> = kakeiboDao.getAllBudgets()

    // 💡 既存のsaveBudget関数
    suspend fun saveBudget(budget: BudgetEntity) { kakeiboDao.insertOrUpdateBudget(budget) }


    // ==========================================
    // 🌟 今回の「ツリー管理＆安全削除」機能用
    // ==========================================

    // 予算を含んだ中カテゴリ一覧の取得
    fun getMiddleCategoriesWithBudget(isIncome: Boolean): Flow<List<MiddleCategoryWithBudget>> {
        return kakeiboDao.getMiddleCategoriesWithBudget(isIncome)
    }

    // 中カテゴリの削除
    suspend fun deleteMiddleCategoryById(id: Int) {
        kakeiboDao.deleteMiddleCategoryById(id)
    }

    // 削除前の安全チェック：この中カテゴリに紐づく小カテゴリがいくつあるか？
    suspend fun getSubCategoryCountByMiddle(middleId: Int): Int {
        return kakeiboDao.getSubCategoryCountByMiddle(middleId)
    }

    // 予算の削除（編集画面で予算を空欄にした時に使用）
    suspend fun deleteBudgetByMiddle(middleId: Int) {
        kakeiboDao.deleteBudgetByMiddle(middleId)
    }

    // 小カテゴリの削除
    suspend fun deleteSubCategoryById(id: Int) {
        kakeiboDao.deleteSubCategoryById(id)
    }

    // 削除前の安全チェック：この小カテゴリを使った家計簿履歴がいくつあるか？
    suspend fun getKakeiboCountBySubCategory(subId: Int): Int {
        return kakeiboDao.getKakeiboCountBySubCategory(subId)
    }

    // 💡 新規追加ロジック側から「insertBudget」という名前で呼ばれても、
    // 既存の「insertOrUpdateBudget」へ綺麗に流れるように仲介します
    suspend fun insertBudget(budget: BudgetEntity) {
        kakeiboDao.insertOrUpdateBudget(budget)
    }

    fun getMiddleCategoryExpenseSum(middleCategoryId: Int, monthQuery: String): Flow<Int> {
        return kakeiboDao.getMiddleCategoryExpenseSum(middleCategoryId, monthQuery).map { it ?: 0 }
    }

    fun getBudgetAmount(middleCategoryId: Int): Flow<Int> {
        return kakeiboDao.getBudgetAmount(middleCategoryId).map { it ?: 0 }
    }

    // 総資産計算のためにシステムデータを含む生の家計簿データを取得
    val allRawKakeiboItems: Flow<List<KakeiboEntity>> = kakeiboDao.getAllRawKakeiboItems()

    val allRawDisplayItems: Flow<List<KakeiboDisplayItem>> = kakeiboDao.getAllRawDisplayItems()

    // 🎯 目標関連のデータ中継
    val allTargets: Flow<List<TargetEntity>> = kakeiboDao.getAllTargets()
    val monthlyAnalysisData: Flow<List<KakeiboMonthSummary>> = kakeiboDao.getMonthlyAnalysisData()

    suspend fun insertTarget(target: TargetEntity) = kakeiboDao.insertTarget(target)
    suspend fun updateTarget(target: TargetEntity) = kakeiboDao.updateTarget(target)
    suspend fun deleteTarget(targetId: Int) = kakeiboDao.deleteTarget(targetId)

    // 📊 予算履歴 ＆ 月間実績の取得
    suspend fun upsertBudgetHistory(budgetHistory: BudgetHistoryEntity) = kakeiboDao.upsertBudgetHistory(budgetHistory)

    fun getMonthlyAchievement(yearMonth: String): Flow<List<MonthlyAchievementItem>> =
        kakeiboDao.getMonthlyAchievement(yearMonth)
}