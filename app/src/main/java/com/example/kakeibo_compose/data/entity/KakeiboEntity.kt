package com.example.kakeibo_compose.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "kakeibo_table",
    foreignKeys = [
        ForeignKey(
            entity = SubCategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["sub_category_id"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    // 👇 【ここを追加！】Roomからの警告（Warning）を消し、テーブル結合を爆速にするためのインデックスです
    indices = [Index(value = ["sub_category_id"])]
)
data class KakeiboEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String,
    @ColumnInfo(name = "sub_category_id") val subCategoryId: Int,
    val amount: Int,
    val memo: String,
    @ColumnInfo(name = "is_system", defaultValue = "0") val isSystem: Boolean = false
)