package com.example.kakeibo_compose.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.kakeibo_compose.data.entity.KakeiboEntity// ※既存のEntityの場所に合わせて適宜インポート
import kotlinx.coroutines.flow.Flow

@Dao
interface KakeiboDao {
    @Insert
    suspend fun insert(item: KakeiboEntity)

    @Query("SELECT * FROM kakeibo_table ORDER BY date DESC")
    fun getAllItems(): Flow<List<KakeiboEntity>>

    @Query("SELECT category FROM kakeibo_table WHERE is_income = 0 GROUP BY category ORDER BY COUNT(category) DESC")
    fun getCommonExpenseCategories(): Flow<List<String>>

    @Query("SELECT category FROM kakeibo_table WHERE is_income = 1 GROUP BY category ORDER BY COUNT(category) DESC")
    fun getCommonIncomeCategories(): Flow<List<String>>
}