package com.example.kakeibo_compose.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sub_category_table",
    foreignKeys = [
        ForeignKey(
            entity = MiddleCategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["middle_category_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    // 👇 【ここを追加！】中カテゴリIDと小カテゴリ名の組み合わせをユニーク（唯一無二）にします
    indices = [Index(value = ["middle_category_id", "name"], unique = true)]
)
data class SubCategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "middle_category_id") val middleCategoryId: Int,
    val name: String
)