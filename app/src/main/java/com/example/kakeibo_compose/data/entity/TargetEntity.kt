package com.example.kakeibo_compose.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "target_table")
data class TargetEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,                                      // 💡 目的（例: 旅行、貯金）
    @ColumnInfo(name = "target_amount") val targetAmount: Int, // 💡 目標金額
    @ColumnInfo(name = "deadline_date") val deadlineDate: String, // 💡 いつまで（yyyy-MM-dd）
    @ColumnInfo(name = "is_completed") val isCompleted: Boolean = false // 💡 達成フラグ
)