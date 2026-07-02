package com.example.kakeibo_compose.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
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
     * 💡 【その場登録用】新しい中カテゴリを追加する（重複がなければ登録）
     * 登録成功時に、画面側に通知できるようコールバック（onResult）を付けています。
     */
    fun addMiddleCategory(name: String, isIncome: Boolean, onResult: (Boolean, String) -> Unit) {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) {
            onResult(false, "カテゴリ名を入力してください")
            return
        }
        viewModelScope.launch {
            if (repository.isMiddleCategoryDuplicate(isIncome, trimmedName)) {
                onResult(false, "既に同じ名前の中カテゴリが存在します")
            } else {
                repository.insertMiddleCategory(MiddleCategoryEntity(name = trimmedName, isIncome = isIncome))
                onResult(true, "中カテゴリを追加しました")
            }
        }
    }

    /**
     * 💡 【その場登録用】新しい小カテゴリを追加する（同じ中カテゴリ内に同名がなければ登録）
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
            if (repository.isSubCategoryDuplicate(middleCategoryId, trimmedName)) {
                onResult(false, "この中カテゴリ内に既に同じ小カテゴリが存在します")
            } else {
                repository.insertSubCategory(SubCategoryEntity(middleCategoryId = middleCategoryId, name = trimmedName))
                onResult(true, "小カテゴリを追加しました")
            }
        }
    }
}