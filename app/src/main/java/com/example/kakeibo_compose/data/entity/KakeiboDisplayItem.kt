package com.example.kakeibo_compose.data.entity

import androidx.room.ColumnInfo

// 💡 画面に「履歴一覧」として表示するために、3つのテーブルから必要な情報だけを集約したクラスです
data class KakeiboDisplayItem(
    val id: Int,
    val date: String,
    val amount: Int,
    val memo: String,
    @ColumnInfo(name = "sub_category_id") val subCategoryId: Int,
    @ColumnInfo(name = "sub_category_name") val subCategoryName: String,       // 小カテゴリ名（例: 昼飯）
    @ColumnInfo(name = "middle_category_name") val middleCategoryName: String, // 中カテゴリ名（例: 食費）
    @ColumnInfo(name = "is_income") val isIncome: Boolean                      // 収支フラグ（支出か収入か）
)