package com.example.kakeibo_compose

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

// ==========================================
// 1. Entity（収支表のテーブル設計図）
// ==========================================
@Entity(tableName = "kakeibo_table")
data class KakeiboEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0, // 通番（自動で+1される）
    @ColumnInfo(name = "date") val date: String,       // 日付（例: "2026-07-01"）
    @ColumnInfo(name = "is_income") val isIncome: Boolean, // 収支区分（true:収入, false:支出）
    @ColumnInfo(name = "category") val category: String,   // カテゴリ
    @ColumnInfo(name = "amount") val amount: Int,         // 金額
    @ColumnInfo(name = "memo") val memo: String           // 理由（メモ）
)

// ==========================================
// 2. DAO（データの注文書・操作方法）
// ==========================================
@Dao
interface KakeiboDao {
    @Insert
    suspend fun insert(item: KakeiboEntity)

    @Query("SELECT * FROM kakeibo_table ORDER BY date DESC")
    fun getAllItems(): Flow<List<KakeiboEntity>>

    @Query("SELECT * FROM kakeibo_table WHERE date LIKE :month || '%'")
    fun getItemsByMonth(month: String): Flow<List<KakeiboEntity>>

    // 💡 過去の「支出(is_income = 0)」から、多い順でカテゴリを取得
    @Query("SELECT category FROM kakeibo_table WHERE is_income = 0 GROUP BY category ORDER BY COUNT(category) DESC")
    fun getCommonExpenseCategories(): Flow<List<String>>

    // 💡 過去の「収入(is_income = 1)」から、多い順でカテゴリを取得
    @Query("SELECT category FROM kakeibo_table WHERE is_income = 1 GROUP BY category ORDER BY COUNT(category) DESC")
    fun getCommonIncomeCategories(): Flow<List<String>>
}

// ==========================================
// 3. Database（データベースの大元・管理人）
// ==========================================
@Database(entities = [KakeiboEntity::class], version = 1, exportSchema = false)
abstract class KakeiboDatabase : RoomDatabase() {
    abstract fun kakeiboDao(): KakeiboDao

    companion object {
        @Volatile
        private var INSTANCE: KakeiboDatabase? = null

        // アプリ全体で1つのDBインスタンスを安全に使い回すための定番の関数
        fun getDatabase(context: Context): KakeiboDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    KakeiboDatabase::class.java,
                    "kakeibo_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}