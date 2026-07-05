package com.example.kakeibo_compose.data.local

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.kakeibo_compose.data.entity.BudgetEntity
import com.example.kakeibo_compose.data.entity.CategorySelectionItem
import com.example.kakeibo_compose.data.entity.MiddleCategoryEntity
import com.example.kakeibo_compose.data.entity.SubCategoryEntity
import com.example.kakeibo_compose.data.entity.KakeiboEntity
import com.example.kakeibo_compose.data.entity.KakeiboDisplayItem
import com.example.kakeibo_compose.data.entity.MiddleCategoryWithBudget
import com.example.kakeibo_compose.data.entity.TargetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface KakeiboDao {

    @Insert
    suspend fun insertKakeibo(item: KakeiboEntity)

    @Insert
    suspend fun insertMiddleCategory(cat: MiddleCategoryEntity): Long

    @Insert
    suspend fun insertSubCategory(subCat: SubCategoryEntity)

    @Query("DELETE FROM kakeibo_table WHERE id = :id")
    suspend fun deleteKakeiboById(id: Int)

    @Query("UPDATE kakeibo_table SET amount = :amount, memo = :memo, sub_category_id = :subCategoryId WHERE id = :id")
    suspend fun updateKakeiboFull(id: Int, amount: Int, memo: String, subCategoryId: Int)

    // 💡 【修正後】中カテゴリの一覧を、実際の取引（小カテゴリ経由）で「よく使う順（件数順）」で取得するSQL
    @Query("""
        SELECT m.* FROM middle_category_table m
        LEFT JOIN sub_category_table s ON s.middle_category_id = m.id
        LEFT JOIN kakeibo_table k ON k.sub_category_id = s.id
        WHERE m.is_income = :isIncome AND m.is_system = 0
        GROUP BY m.id
        ORDER BY COUNT(k.id) DESC, m.id ASC
    """)
    fun getMiddleCategories(isIncome: Boolean): Flow<List<MiddleCategoryEntity>>

    // 💡 3. 【新規追加】総資産の計算用：システムデータ（初期資産）も含めて「全部」持ってくるクエリ
    @Query("SELECT * FROM kakeibo_table")
    fun getAllRawKakeiboItems(): Flow<List<KakeiboEntity>>

    // 💡 総資産計算用（エイリアスをKakeiboDisplayItemの定義に完全一致！）
    @Query("""
        SELECT k.id, k.date, k.amount, k.memo, m.is_income, 
               k.sub_category_id AS sub_category_id,
               m.name AS middle_category_name, 
               s.name AS sub_category_name 
        FROM kakeibo_table k
        INNER JOIN sub_category_table s ON k.sub_category_id = s.id
        INNER JOIN middle_category_table m ON s.middle_category_id = m.id
        ORDER BY k.date DESC, k.id DESC
    """)
    fun getAllRawDisplayItems(): Flow<List<KakeiboDisplayItem>>

    // 💡 【修正】自分以外のIDで、同じ名前の中カテゴリ（支出/収入別）があるか数える
    @Query("""
        SELECT COUNT(*) FROM middle_category_table 
        WHERE is_income = :isIncome AND name = :name AND id != :excludeId
    """)
    suspend fun getMiddleCategoryCount(isIncome: Boolean, name: String, excludeId: Int): Int

    // 💡 【修正】自分以外のIDで、同じ中カテゴリ内に同じ名前の小カテゴリがあるか数える
    @Query("""
        SELECT COUNT(*) FROM sub_category_table 
        WHERE middle_category_id = :middleCategoryId AND name = :name AND id != :excludeId
    """)
    suspend fun getSubCategoryCount(middleCategoryId: Int, name: String, excludeId: Int): Int

    // 💡 履歴表示用（エイリアスをKakeiboDisplayItemの定義に完全一致！）
    @Query("""
        SELECT k.id, k.date, k.amount, k.memo, m.is_income, 
               k.sub_category_id AS sub_category_id,
               m.name AS middle_category_name, 
               s.name AS sub_category_name 
        FROM kakeibo_table k
        INNER JOIN sub_category_table s ON k.sub_category_id = s.id
        INNER JOIN middle_category_table m ON s.middle_category_id = m.id
        WHERE k.is_system = 0 
        ORDER BY k.date DESC, k.id DESC
    """)
    fun getAllDisplayItems(): Flow<List<KakeiboDisplayItem>>

    // 💡 結合して取得する中身を、小カテゴリEntityそのものから「中カテゴリ名」なども含んだリッチなデータに変更
    @Query("""
        SELECT s.* FROM sub_category_table s
        INNER JOIN middle_category_table m ON s.middle_category_id = m.id
        LEFT JOIN kakeibo_table k ON k.sub_category_id = s.id
        WHERE m.is_income = :isIncome
        GROUP BY s.id
        ORDER BY COUNT(k.id) DESC, s.id ASC
    """)
    fun getCommonSubCategories(isIncome: Boolean): Flow<List<SubCategoryEntity>>

    // --- カテゴリ管理・予算機能用 ---

    // 中カテゴリの名前変更
    @Query("UPDATE middle_category_table SET name = :newName WHERE id = :id")
    suspend fun updateMiddleCategoryName(id: Int, newName: String)

    // 小カテゴリの名前変更
    @Query("UPDATE sub_category_table SET name = :newName WHERE id = :id")
    suspend fun updateSubCategoryName(id: Int, newName: String)

    // 特定の中カテゴリに紐づく小カテゴリを（並び順関係なく）素直に全部取る
    @Query("SELECT * FROM sub_category_table WHERE middle_category_id = :middleCategoryId ORDER BY id ASC")
    fun getSubCategoriesByMiddle(middleCategoryId: Int): Flow<List<SubCategoryEntity>>


    // 予算の保存（あれば上書き、なければ挿入）
    @Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateBudget(budget: BudgetEntity)

    // 💡 全ての中カテゴリの固定予算マスターをそのまま全件取得する
    @Query("SELECT * FROM budget_table")
    fun getAllBudgets(): Flow<List<BudgetEntity>>

    // 💡 収支(isIncome)に合わせて、中カテゴリと小カテゴリを結合し、使用回数（COUNT）が多い順に並び替えるSQL
    @Query("""
        SELECT 
            s.id AS subId, 
            s.name AS subName, 
            m.id AS middleId, 
            m.name AS middleName 
        FROM sub_category_table s
        INNER JOIN middle_category_table m ON s.middle_category_id = m.id
        LEFT JOIN kakeibo_table k ON s.id = k.sub_category_id
        WHERE m.is_Income = :isIncome
        GROUP BY s.id
        ORDER BY COUNT(k.id) DESC, s.id ASC
    """)
    fun getCategoriesSortedByUsage(isIncome: Boolean): Flow<List<CategorySelectionItem>>

    // 💡 予算情報をLEFT JOINで一緒に引っ張ってくるクエリ
    @Query("""
    SELECT m.id, m.name, m.is_income AS isIncome, b.amount AS budgetAmount 
    FROM middle_category_table m
    LEFT JOIN budget_table b ON m.id = b.middle_category_id
    WHERE m.is_income = :isIncome
""")
    fun getMiddleCategoriesWithBudget(isIncome: Boolean): Flow<List<MiddleCategoryWithBudget>>

    // 💡 削除可否チェック用：小カテゴリに紐づく家計簿データの件数を数える
    @Query("SELECT COUNT(*) FROM kakeibo_table WHERE sub_category_id = :subId")
    suspend fun getKakeiboCountBySubCategory(subId: Int): Int

    // 💡 削除可否チェック用：中カテゴリに紐づく小カテゴリの件数を数える
    @Query("SELECT COUNT(*) FROM sub_category_table WHERE middle_category_id = :middleId")
    suspend fun getSubCategoryCountByMiddle(middleId: Int): Int

    // 💡 予算の削除（中カテゴリ編集で予算を空にした時用）
    @Query("DELETE FROM budget_table WHERE middle_category_id = :middleId")
    suspend fun deleteBudgetByMiddle(middleId: Int)

    // 💡 予算の保存（新規追加・更新用：お持ちのInsert(onConflict)メソッドをそのまま使ってもOKです）
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: BudgetEntity)

    // 💡 中カテゴリ自体の削除
    @Query("DELETE FROM middle_category_table WHERE id = :id")
    suspend fun deleteMiddleCategoryById(id: Int)

    // 💡 小カテゴリ自体の削除
    @Query("DELETE FROM sub_category_table WHERE id = :id")
    suspend fun deleteSubCategoryById(id: Int)

    // 今月の特定の中カテゴリの支出合計を取得する（日付が "2026-07-04" のような形式の場合の例）
// ※お使いの日付フォーマット（YYYY-MMなど）に合わせて LIKE の指定は調整してください
    @Query("""
        SELECT SUM(k.amount) 
        FROM kakeibo_table k
        INNER JOIN sub_category_table s ON k.sub_category_id = s.id
        WHERE s.middle_category_id = :middleCategoryId 
          AND k.date LIKE :monthQuery || '%'
    """)
    fun getMiddleCategoryExpenseSum(middleCategoryId: Int, monthQuery: String): Flow<Int?>

    // 指定した中カテゴリの予算額を取得する
    @Query("SELECT amount FROM budget_table WHERE middle_category_id = :middleCategoryId")
    fun getBudgetAmount(middleCategoryId: Int): Flow<Int?>

    // ====================================================
    // 🎯 目標設定機能用のクエリ
    // ====================================================

    @Query("SELECT * FROM target_table ORDER BY is_completed ASC, deadline_date ASC")
    fun getAllTargets(): Flow<List<TargetEntity>>

    @Insert
    suspend fun insertTarget(target: TargetEntity)

    @Update
    suspend fun updateTarget(target: TargetEntity)

    @Query("DELETE FROM target_table WHERE id = :targetId")
    suspend fun deleteTarget(targetId: Int)

    // 💡 分析用：直近1年間の月ごとの「収入合計」と「支出合計」をまとめて集計するクエリ
    // 結果を受け取るためのシンプルなデータ構造（KakeiboMonthSummary）も下に定義します
    @Query("""
        SELECT 
            strftime('%Y-%m', k.date) AS month,
            SUM(CASE WHEN m.is_income = 1 THEN k.amount ELSE 0 END) AS total_income,
            SUM(CASE WHEN m.is_income = 0 THEN k.amount ELSE 0 END) AS total_expense
        FROM kakeibo_table k
        INNER JOIN sub_category_table s ON k.sub_category_id = s.id
        INNER JOIN middle_category_table m ON s.middle_category_id = m.id
        WHERE k.date >= date('now', '-1 year') AND k.is_system = 0 
        GROUP BY month
        ORDER BY month DESC
    """)
    fun getMonthlyAnalysisData(): Flow<List<KakeiboMonthSummary>>
}

data class KakeiboMonthSummary(
    val month: String,
    @ColumnInfo(name = "total_income") val totalIncome: Int,
    @ColumnInfo(name = "total_expense") val totalExpense: Int
)