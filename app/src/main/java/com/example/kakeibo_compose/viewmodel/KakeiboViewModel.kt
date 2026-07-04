package com.example.kakeibo_compose.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.kakeibo_compose.data.entity.BudgetEntity
import com.example.kakeibo_compose.data.entity.CategorySelectionItem
import com.example.kakeibo_compose.data.entity.MiddleCategoryEntity
import com.example.kakeibo_compose.data.entity.SubCategoryEntity
import com.example.kakeibo_compose.data.entity.KakeiboEntity
import com.example.kakeibo_compose.data.entity.MiddleCategoryWithBudget
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

    // com.example.kakeibo_compose.viewmodel.KakeiboViewModel 内

    // 💡 履歴の削除
    fun deleteKakeibo(id: Int) {
        viewModelScope.launch {
            try {
                repository.deleteKakeiboById(id)
            } catch (e: Exception) {
                // 必要に応じてエラーログなどを出力してください
            }
        }
    }

    // 💡 履歴の更新（金額とメモ）
    fun updateKakeibo(id: Int, amount: Int, memo: String, subCategoryId: Int) {
        viewModelScope.launch {
            try {
                if (amount > 0) {
                    repository.updateKakeiboFull(id, amount, memo, subCategoryId)
                }
            } catch (e: Exception) {
                // 必要に応じてエラー処理
            }
        }
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

    fun getCategorySelectionList(isIncome: Boolean): Flow<List<CategorySelectionItem>> {
        return repository.getCategoriesSortedByUsage(isIncome)
    }

    // 💡 予算付き中カテゴリのFlowを画面に提供
    fun getMiddleCategoriesWithBudget(isIncome: Boolean): Flow<List<MiddleCategoryWithBudget>> {
        return repository.getMiddleCategoriesWithBudget(isIncome) // リポジトリ経由でDaoを呼び出し
    }

    // 💡 中カテゴリ＋予算の同時追加
    fun addMiddleCategoryWithBudget(name: String, isIncome: Boolean, budgetAmount: Int?, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                if (name.isBlank()) {
                    onResult(false, "カテゴリ名を入力してください")
                    return@launch
                }

                if (!isIncome && (budgetAmount == null || budgetAmount <= 0)) {
                    onResult(false, "毎月の予算を設定してください")
                    return@launch
                }

                // 🛑 重複チェック（新規追加なので、除外するIDは存在しない 0 を指定）
                val isDuplicate = repository.isMiddleCategoryDuplicate(isIncome, name, excludeId = 0)
                if (isDuplicate) {
                    onResult(false, "「${name}」は既に登録されています")
                    return@launch
                }

                // チェックをパスしたら挿入
                val middleId = repository.insertMiddleCategory(MiddleCategoryEntity(name = name, isIncome = isIncome))
                if (!isIncome && budgetAmount != null) {
                    repository.insertBudget(BudgetEntity(middleCategoryId = middleId.toInt(), amount = budgetAmount))
                }
                onResult(true, "中カテゴリを追加しました")
            } catch (e: Exception) {
                onResult(false, "追加に失敗しました: ${e.localizedMessage}")
            }
        }
    }

    // 💡 中カテゴリと予算の編集（更新）
    fun updateMiddleCategoryWithBudget(middleId: Int, newName: String, isIncome: Boolean, budgetAmount: Int?, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                if (newName.isBlank()) {
                    onResult(false, "カテゴリ名を入力してください")
                    return@launch
                }

                if (!isIncome && (budgetAmount == null || budgetAmount <= 0)) {
                    onResult(false, "毎月の予算を設定してください")
                    return@launch
                }

                // 🛑 重複チェック（自分自身のmiddleIdを除外して、他のカテゴリと被っていないかチェック）
                val isDuplicate = repository.isMiddleCategoryDuplicate(isIncome, newName, excludeId = middleId)
                if (isDuplicate) {
                    onResult(false, "「${newName}」は既に登録されています")
                    return@launch
                }

                // チェックをパスしたら更新
                repository.updateMiddleCategoryName(middleId, newName)
                if (!isIncome && budgetAmount != null) {
                    repository.insertBudget(BudgetEntity(middleCategoryId = middleId, amount = budgetAmount))
                } else {
                    repository.deleteBudgetByMiddle(middleId)
                }
                onResult(true, "変更を保存しました")
            } catch (e: Exception) {
                onResult(false, "保存に失敗しました")
            }
        }
    }

    // 💡 【重要】中カテゴリの削除（小カテゴリがない場合のみ）
    fun deleteMiddleCategorySafety(middleId: Int, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val subCount = repository.getSubCategoryCountByMiddle(middleId)
            if (subCount > 0) {
                onResult(false, "小カテゴリが登録されているため削除できません")
            } else {
                repository.deleteMiddleCategoryById(middleId) // お持ちの中カテゴリ削除クエリ
                onResult(true, "中カテゴリを削除しました")
            }
        }
    }

    // 💡 【重要】小カテゴリの削除（家計簿データがない場合のみ）
    fun deleteSubCategorySafety(subId: Int, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val kakeiboCount = repository.getKakeiboCountBySubCategory(subId)
            if (kakeiboCount > 0) {
                onResult(false, "このカテゴリを使用した会計履歴があるため削除できません")
            } else {
                repository.deleteSubCategoryById(subId) // お持ちの小カテゴリ削除クエリ
                onResult(true, "小カテゴリを削除しました")
            }
        }
    }
}