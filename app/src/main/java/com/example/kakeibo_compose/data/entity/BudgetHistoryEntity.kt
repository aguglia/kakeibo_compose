package com.example.kakeibo_compose.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity

// 💡 「この年月 ＋ この中カテゴリ」の組み合わせで唯一の履歴とします
@Entity(
    tableName = "budget_history_table",
    primaryKeys = ["year_month", "middle_category_id"]
)
data class BudgetHistoryEntity(
    @ColumnInfo(name = "year_month") val yearMonth: String,          // 💡 登録年月 (形式: yyyy-MM)
    @ColumnInfo(name = "middle_category_id") val middleCategoryId: Int, // 💡 【修正】中カテゴリID
    @ColumnInfo(name = "budget_amount") val budgetAmount: Int        // 💡 設定された予算額
)