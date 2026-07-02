package com.example.kakeibo_compose.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.kakeibo_compose.data.entity.KakeiboEntity // 👈 ここを entity パッケージからのインポートに書き換えます！
import com.example.kakeibo_compose.data.local.KakeiboDao

@Database(entities = [KakeiboEntity::class], version = 1, exportSchema = false)
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
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}