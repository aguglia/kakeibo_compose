package com.example.kakeibo_compose.data.entity

// 💡 中カテゴリ情報に、予算（budget_tableのamount）をくっつけた画面表示用のクラス
data class MiddleCategoryWithBudget(
    val id: Int,
    val name: String,
    val isIncome: Boolean,
    val budgetAmount: Int? // 予算がない場合、または収入カテゴリの場合はnull
)