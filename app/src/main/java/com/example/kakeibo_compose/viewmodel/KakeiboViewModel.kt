package com.example.kakeibo_compose.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.kakeibo_compose.data.entity.BudgetEntity
import com.example.kakeibo_compose.data.entity.MiddleCategoryEntity
import com.example.kakeibo_compose.data.entity.SubCategoryEntity
import com.example.kakeibo_compose.data.entity.KakeiboEntity
import com.example.kakeibo_compose.data.local.KakeiboDatabase
import com.example.kakeibo_compose.data.repository.KakeiboRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class KakeiboViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: KakeiboRepository = KakeiboRepository(
        KakeiboDatabase.getDatabase(application).kakeiboDao()
    )

    // 💡 MainScreenやHistoryScreenが呼び出しているプロパティ名を最新のRepositoryとガチッと結合！
    val allItems = repository.allDisplayItems
    val commonExpenseSubCategories = repository.commonExpenseSubCategories
    val commonIncomeSubCategories = repository.commonIncomeSubCategories

    // 💡 入力画面のドロップダウン用：中カテゴリのリストを直接取得する関数
    fun getMiddleCategories(isIncome: Boolean): Flow<List<MiddleCategoryEntity>> {
        return repository.getMiddleCategories(isIncome)
    }

    // 💡 総資産の計算（KakeiboDisplayItemの型に合わせて正しく計算）
    val totalAsset = allItems.map { items ->
        items.sumOf { if (it.isIncome) it.amount else -it.amount }
    }

    // 💡 今月の出費の計算（KakeiboDisplayItemの型に合わせて正しくフィルタリング）
    val thisMonthExpense = allItems.map { items ->
        val currentMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
        items.filter { !it.isIncome && it.date.startsWith(currentMonth) }.sumOf { it.amount }
    }

    /**
     * 💡 【確定版】家計簿データの保存
     * 選択された小カテゴリのIDをセットして安全にDBに挿入します。
     */
    fun saveItem(subCategoryId: Int, amount: Int, memo: String) {
        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val entity = KakeiboEntity(
            date = currentDate,
            subCategoryId = subCategoryId,
            amount = amount,
            memo = memo
        )
        viewModelScope.launch {
            repository.insertKakeibo(entity)
        }
    }

    /**
     * 【その場登録用】新しい中カテゴリを追加する（新規なので除外IDは0）
     */
    fun addMiddleCategory(name: String, isIncome: Boolean, onResult: (Boolean, String) -> Unit) {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) {
            onResult(false, "カテゴリ名を入力してください")
            return
        }
        viewModelScope.launch {
            if (repository.isMiddleCategoryDuplicate(isIncome, trimmedName, excludeId = 0)) { // 💡 excludeIdに0を指定
                onResult(false, "既に同じ名前の中カテゴリが存在します")
            } else {
                repository.insertMiddleCategory(MiddleCategoryEntity(name = trimmedName, isIncome = isIncome))
                onResult(true, "中カテゴリを追加しました")
            }
        }
    }

    /**
     * 【その場登録用】新しい小カテゴリを追加する（新規なので除外IDは0）
     */
    fun addSubCategory(middleCategoryId: Int, name: String, onResult: (Boolean, String) -> Unit) {
        val trimmedName = name.trim()
        if (middleCategoryId == 0) {
            onResult(false, "中カテゴリを先に選択してください")
            return
        }
        if (trimmedName.isEmpty()) {
            onResult(false, "小カテゴリ名を入力してください")
            return
        }
        viewModelScope.launch {
            if (repository.isSubCategoryDuplicate(middleCategoryId, trimmedName, excludeId = 0)) { // 💡 excludeIdに0を指定
                onResult(false, "この中カテゴリ内に既に同じ小カテゴリが存在します")
            } else {
                repository.insertSubCategory(SubCategoryEntity(middleCategoryId = middleCategoryId, name = trimmedName))
                onResult(true, "小カテゴリを追加しました")
            }
        }
    }

    /**
     * 【修正】中カテゴリ名の変更（自分のIDを除外して重複チェック）
     */
    fun updateMiddleCategoryName(id: Int, isIncome: Boolean, newName: String, onResult: (Boolean, String) -> Unit) {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) return onResult(false, "名前を入力してください")
        viewModelScope.launch {
            if (repository.isMiddleCategoryDuplicate(isIncome, trimmed, excludeId = id)) { // 💡 自分のIDを渡す！
                onResult(false, "既に他のカテゴリで同じ名前が使われています")
            } else {
                repository.updateMiddleCategoryName(id, trimmed)
                onResult(true, "中カテゴリ名を変更しました")
            }
        }
    }

    /**
     * 【修正】小カテゴリ名の変更（自分のIDを除外して重複チェック）
     */
    fun updateSubCategoryName(id: Int, middleCategoryId: Int, newName: String, onResult: (Boolean, String) -> Unit) {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) return onResult(false, "名前を入力してください")
        viewModelScope.launch {
            if (repository.isSubCategoryDuplicate(middleCategoryId, trimmed, excludeId = id)) { // 💡 自分のIDを渡す！
                onResult(false, "この中カテゴリ内に既に同じ名前の小カテゴリがあります")
            } else {
                repository.updateSubCategoryName(id, trimmed)
                onResult(true, "小カテゴリ名を変更しました")
            }
        }
    }

    // 固定予算マスターを全件監視
    val allBudgets = repository.allBudgets

    // 💡 予算の保存（月の概念を無くし、純粋に中カテゴリに金額を紐付けるだけ！）
    fun saveBudget(middleCategoryId: Int, amount: Int) {
        viewModelScope.launch {
            repository.saveBudget(
                BudgetEntity(middleCategoryId = middleCategoryId, amount = amount)
            )
        }
    }

    // 💡 【これを追加！】特定の中カテゴリに紐づく小カテゴリのストリームをUIに中継します
    fun getSubCategoriesByMiddle(middleCategoryId: Int): Flow<List<SubCategoryEntity>> {
        return repository.getSubCategoriesByMiddle(middleCategoryId)
    }
}