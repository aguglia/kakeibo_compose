package com.example.kakeibo_compose.data.entity

// 💡 「中 > 小」のリスト表示と、保存に必要なIDを保持するクラス
data class CategorySelectionItem(
    val subId: Int,
    val subName: String,
    val middleId: Int,
    val middleName: String
) {
    // 画面に表示する用の文字列（例: "食費 > 昼食"）を簡単に取得できるようにするプロパティ
    val displayName: String
        get() = "$middleName > $subName"
}