package com.example.kakeibo_compose.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "middle_category_table")
data class MiddleCategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String, // "食費", "日用品", "給与" など
    @ColumnInfo(name = "is_income") val isIncome: Boolean, // 💡 支出ならfalse、収入ならtrue
    @ColumnInfo(name = "is_system", defaultValue = "0") val isSystem: Boolean = false // 💡 システムフラグを追加
)