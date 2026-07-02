package com.example.kakeibo_compose.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.kakeibo_compose.data.entity.MiddleCategoryEntity
import com.example.kakeibo_compose.data.entity.SubCategoryEntity
import com.example.kakeibo_compose.data.entity.KakeiboEntity

// 💡 確実に3つのクラスが認識されるように明示します
@Database(
    entities = [
        MiddleCategoryEntity::class,
        SubCategoryEntity::class,
        KakeiboEntity::class
    ],
    version = 2,
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
                    .fallbackToDestructiveMigration(true) // バージョンアップ時にDBを安全に再構築
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}