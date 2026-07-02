package com.example.kakeibo_compose.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "kakeibo_table")
data class KakeiboEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String,       // 日付 (yyyy-MM-dd)

    // 👇 【ここを追加！】Roomに「DBの中では is_income って名前にしてね」と明示します
    @ColumnInfo(name = "is_income") val isIncome: Boolean,

    val category: String,   // カテゴリ名
    val amount: Int,        // 金額
    val memo: String        // メモ
)