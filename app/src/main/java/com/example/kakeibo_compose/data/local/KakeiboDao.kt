package com.example.kakeibo_compose.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.kakeibo_compose.data.entity.MiddleCategoryEntity
import com.example.kakeibo_compose.data.entity.SubCategoryEntity
import com.example.kakeibo_compose.data.entity.KakeiboEntity
import com.example.kakeibo_compose.data.entity.KakeiboDisplayItem
import kotlinx.coroutines.flow.Flow

@Dao
interface KakeiboDao {

    @Insert
    suspend fun insertKakeibo(item: KakeiboEntity)

    @Insert
    suspend fun insertMiddleCategory(cat: MiddleCategoryEntity): Long

    @Insert
    suspend fun insertSubCategory(subCat: SubCategoryEntity)

    // 💡 【修正後】中カテゴリの一覧を、実際の取引（小カテゴリ経由）で「よく使う順（件数順）」で取得するSQL
    @Query("""
        SELECT m.* FROM middle_category_table m
        LEFT JOIN sub_category_table s ON s.middle_category_id = m.id
        LEFT JOIN kakeibo_table k ON k.sub_category_id = s.id
        WHERE m.is_income = :isIncome
        GROUP BY m.id
        ORDER BY COUNT(k.id) DESC, m.id ASC
    """)
    fun getMiddleCategories(isIncome: Boolean): Flow<List<MiddleCategoryEntity>>

    // 💡 【重複チェック用】選択された中カテゴリの中に、すでに同じ名前の小カテゴリがあるか件数を数える
    @Query("SELECT COUNT(*) FROM sub_category_table WHERE middle_category_id = :middleCategoryId AND name = :name")
    suspend fun getSubCategoryCount(middleCategoryId: Int, name: String): Int

    // 💡 【重複チェック用】すでに同じ名前の中カテゴリ（支出/収入別）があるか件数を数える
    @Query("SELECT COUNT(*) FROM middle_category_table WHERE is_income = :isIncome AND name = :name")
    suspend fun getMiddleCategoryCount(isIncome: Boolean, name: String): Int

    @Query("""
        SELECT 
            k.id AS id,
            k.date AS date,
            k.amount AS amount,
            k.memo AS memo,
            s.name AS sub_category_name,
            m.name AS middle_category_name,
            m.is_income AS is_income
        FROM kakeibo_table k
        INNER JOIN sub_category_table s ON k.sub_category_id = s.id
        INNER JOIN middle_category_table m ON s.middle_category_id = m.id
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
}