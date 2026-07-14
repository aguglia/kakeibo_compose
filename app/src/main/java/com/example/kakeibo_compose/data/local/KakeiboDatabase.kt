package com.example.kakeibo_compose.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.kakeibo_compose.data.entity.BudgetEntity
import com.example.kakeibo_compose.data.entity.KakeiboEntity
import com.example.kakeibo_compose.data.entity.MiddleCategoryEntity
import com.example.kakeibo_compose.data.entity.SubCategoryEntity
import com.example.kakeibo_compose.data.entity.TargetEntity
import com.example.kakeibo_compose.data.entity.BudgetHistoryEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        MiddleCategoryEntity::class,
        SubCategoryEntity::class,
        KakeiboEntity::class,
        BudgetEntity::class,
        TargetEntity::class,
        BudgetHistoryEntity::class
    ],
    version = 6, // 💡 バージョンを「6」にアップ！
    exportSchema = false
)
abstract class KakeiboDatabase : RoomDatabase() {
    abstract fun kakeiboDao(): KakeiboDao

    companion object {
        @Volatile
        private var INSTANCE: KakeiboDatabase? = null

        fun getDatabase(context: Context): KakeiboDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    KakeiboDatabase::class.java,
                    "kakeibo_database"
                )
                    .fallbackToDestructiveMigration(true)
                    .addCallback(DatabaseCallback()) // 💡 コールバックを設定
                    .build()
                INSTANCE = instance
                instance
            }
        }

        // 💡 データベースが作成された時に、システム専用カテゴリを自動投入するコールバック
        private class DatabaseCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                CoroutineScope(Dispatchers.IO).launch {
                    // 1. システム用の中カテゴリ「初期費用（収入フラグtrue）」を挿入
                    db.execSQL("INSERT INTO middle_category_table (is_income, name, is_system) VALUES (1, '初期費用', 1);")

                    // 2. いま入れた中カテゴリのIDを取得（通常は 1 になります）
                    val cursor = db.query("SELECT id FROM middle_category_table WHERE is_system = 1 LIMIT 1")
                    if (cursor.moveToFirst()) {
                        val middleId = cursor.getInt(0)
                        // 3. それに紐づくシステム用の小カテゴリ「初期費用」を挿入
                        db.execSQL("INSERT INTO sub_category_table (middle_category_id, name) VALUES ($middleId, '初期費用');")
                    }
                    cursor.close()
                }
            }
        }
    }
}