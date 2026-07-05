package com.example.kakeibo_compose.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.combine
import com.example.kakeibo_compose.data.entity.BudgetEntity
import com.example.kakeibo_compose.data.entity.CategorySelectionItem
import com.example.kakeibo_compose.data.entity.MiddleCategoryEntity
import com.example.kakeibo_compose.data.entity.SubCategoryEntity
import com.example.kakeibo_compose.data.entity.KakeiboEntity
import com.example.kakeibo_compose.data.entity.MiddleCategoryWithBudget
import com.example.kakeibo_compose.data.entity.TargetEntity
import com.example.kakeibo_compose.data.local.KakeiboDatabase
import com.example.kakeibo_compose.data.local.SettingPreferences
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

    // 💡 SettingPreferences を初期化
    private val settingPreferences = SettingPreferences(application)

    // 💡 UI側でリアルタイム監視できる「最低維持資産」のFlow
    val minimumAsset = settingPreferences.minimumAsset

    // 💡 MainScreenやHistoryScreenが呼び出しているプロパティ名を最新のRepositoryとガチッと結合！
    val allItems = repository.allDisplayItems
    val commonExpenseSubCategories = repository.commonExpenseSubCategories
    val commonIncomeSubCategories = repository.commonIncomeSubCategories

    // 💡 入力画面のドロップダウン用：中カテゴリのリストを直接取得する関数
    fun getMiddleCategories(isIncome: Boolean): Flow<List<MiddleCategoryEntity>> {
        return repository.getMiddleCategories(isIncome)
    }

    // 💡 計算元をシステムデータ込の全件（allRawDisplayItems）にするだけ！
    // 初期費用は isIncome = true (収入) で入っているので、自動的にプラスとして計算されます。
    val totalAsset = repository.allRawDisplayItems.map { items ->
        items.sumOf { if (it.isIncome) it.amount else -it.amount }
    }

    // 💡 初回起動時の保存も、ただシステムフラグを true にして保存するだけ！
    fun saveInitialAsset(amount: Int) {
        viewModelScope.launch {
            val currentDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
            repository.insertKakeibo(
                KakeiboEntity(
                    date = currentDate,
                    subCategoryId = 1, // 自動投入されたシステム用小カテゴリのID
                    amount = amount,
                    memo = "初期資産",
                    isSystem = true // 💡 ここをtrueにする
                )
            )
        }
    }

    // 💡 初期登録済みかどうかの判定も、全件の中に1件でもシステムデータがあるかで一発判定
    val hasInitialAsset = repository.allRawDisplayItems.map { items ->
        items.any { it.middleCategoryName == "初期費用" } // あるいはシステム用フラグで判定
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

    // 中カテゴリIDと現在の月（例: "2026-07"）を渡すと、残予算（予算 - 支出）を返すFlow
    fun getRemainingBudget(middleCategoryId: Int, monthQuery: String): Flow<Int?> {
        val budgetFlow = repository.getBudgetAmount(middleCategoryId)
        val expenseFlow = repository.getMiddleCategoryExpenseSum(middleCategoryId, monthQuery)

        return combine(budgetFlow, expenseFlow) { budget, expense ->
            // 💡 予算が設定されていない（0円）場合は、予算管理外として null を返すか、マイナス表示にするか選べます
            if (budget == 0) null else budget - expense
        }
    }

    // 💡 【ここがキモ！】総資産と最低維持資産をガチッと結合して、危険域（10万未満）かどうかを判定するFlow
    // true = 防衛ラインを割り込んでいる（危険） / false = 安全圏
    val isAssetInDanger = totalAsset.combine(minimumAsset) { total, min ->
        total < min
    }

    // 💡 設定画面から呼ばれる、最低維持資産の保存関数
    fun saveMinimumAsset(amount: Int) {
        viewModelScope.launch {
            settingPreferences.saveMinimumAsset(amount)
        }
    }

    // ====================================================
    // 🎯 目標設定 ＆ 分析アドバイスロジック
    // ====================================================

    // 全ての目標を取得するFlow
    val allTargets = repository.allTargets

    // 目標の登録
    fun addTarget(title: String, amount: Int, deadline: String) {
        viewModelScope.launch {
            repository.insertTarget(TargetEntity(title = title, targetAmount = amount, deadlineDate = deadline))
        }
    }

    // 目標の更新（達成フラグの切り替えなど）
    fun updateTarget(target: TargetEntity) {
        viewModelScope.launch {
            repository.updateTarget(target)
        }
    }

    // 目標の削除
    fun deleteTarget(targetId: Int) {
        viewModelScope.launch {
            repository.deleteTarget(targetId)
        }
    }

    /**
     * 📊 【最終防衛ライン上乗せ版】設定された最低維持資産を目標金額にプラスしてシミュレーションする
     * @param includeMinAsset 設定された最低維持資産（防衛ライン）を考慮に入れるかどうか
     */
    fun getTargetAnalysis(target: TargetEntity, includeMinAsset: Boolean): Flow<TargetAnalysisResult> {
        // 💡 過去の収支データ、総資産、そしてDataStoreにある「最低維持資産（minimumAsset）」を結合！
        return combine(repository.monthlyAnalysisData, totalAsset, minimumAsset) { monthlyData, currentAsset, minAssetSetting ->

            // 1. 過去の実績から純粋な月平均の貯金額を計算（初期資産は除外済み）
            val validMonths = monthlyData.filter { it.totalIncome > 0 || it.totalExpense > 0 }
            val averageMonthlySaving = if (validMonths.isNotEmpty()) {
                val totalIncomeSum = validMonths.sumOf { it.totalIncome }
                val totalExpenseSum = validMonths.sumOf { it.totalExpense }
                (totalIncomeSum - totalExpenseSum) / validMonths.size
            } else {
                0
            }

            // 2. 残り月数の計算
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            val deadlineItem = try { sdf.parse(target.deadlineDate) } catch (e: Exception) { null }
            val today = java.util.Date()

            val monthsLeft = if (deadlineItem != null && deadlineItem.after(today)) {
                val diffMs = deadlineItem.time - today.time
                val days = diffMs / (1000 * 60 * 60 * 24)
                (days / 30.43).coerceAtLeast(0.1)
            } else {
                0.1
            }

            // 3. 💡 防衛ラインを考慮する場合、目標金額に「最低維持資産」をドカンとプラスする！
            val baseTargetAmount = target.targetAmount
            val finalTargetAmount = if (includeMinAsset) {
                baseTargetAmount + minAssetSetting
            } else {
                baseTargetAmount
            }

            // 4. あと必要な金額（防衛ライン上乗せ後の目標額ベース）
            val amountNeeded = (finalTargetAmount - currentAsset).coerceAtLeast(0)

            // 5. 今のペースで間に合うかどうかの判定
            val estimatedFutureAsset = currentAsset + (averageMonthlySaving * monthsLeft).toInt()
            val isOnTrack = estimatedFutureAsset >= finalTargetAmount

            // 6. 必要な月間貯金額
            val requiredMonthlySaving = (amountNeeded / monthsLeft).toInt()

            // 7. メッセージの自動生成
            val prefix = if (includeMinAsset) "【防衛ライン考慮】" else "【目標額のみ】"
            val adviceMessage = when {
                target.isCompleted -> "🎉 この目標は既に達成されています！素晴らしい！"
                currentAsset >= finalTargetAmount -> if (includeMinAsset) {
                    "✨ 資産防衛ライン（¥$minAssetSetting）をガッチリ守った上で、既に目標を突破しています！完璧な状態です！"
                } else {
                    "✨ 既に目標金額を突破しています！今すぐ達成ボタンを押せます！"
                }
                averageMonthlySaving <= 0 -> "🚨 $prefix 現在の月間収支が赤字、またはプラマイゼロです。このままだと絶対に間に合いません！"
                isOnTrack -> "✅ $prefix 順調です！今の貯金ペース（月平均 +$averageMonthlySaving 円）を維持できれば、期限までに安全に達成可能です。"
                else -> {
                    val deficit = requiredMonthlySaving - averageMonthlySaving
                    "⚠️ $prefix 【警告】このペースだと目標（資産防衛ライン含む）に ¥$deficit 円足りず、間に合いません！毎月の支出をあと ¥$deficit 円セーブする必要があります。"
                }
            }

            TargetAnalysisResult(
                averageMonthlySaving = averageMonthlySaving,
                monthsLeft = monthsLeft,
                amountNeeded = amountNeeded,
                isOnTrack = isOnTrack,
                requiredMonthlySaving = requiredMonthlySaving,
                adviceMessage = adviceMessage
            )
        }
    }
}

data class TargetAnalysisResult(
    val averageMonthlySaving: Int,      // 月平均の貯金額
    val monthsLeft: Double,             // 期限までの残り月数
    val amountNeeded: Int,              // あと必要な金額
    val isOnTrack: Boolean,             // 今のペースで間に合うか
    val requiredMonthlySaving: Int,     // 間に合わせるために必要な毎月の貯金額
    val adviceMessage: String           // 画面に表示するアドバイステキスト
)