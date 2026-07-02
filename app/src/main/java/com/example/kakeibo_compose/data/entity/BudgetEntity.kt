package com.example.kakeibo_compose.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "budget_table",
    foreignKeys = [
        ForeignKey(
            entity = MiddleCategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["middle_category_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    // 💡 1つの中カテゴリに対して予算設定は1つだけ（ユニーク）
    indices = [Index(value = ["middle_category_id"], unique = true)]
)
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "middle_category_id") val middleCategoryId: Int, // 中カテゴリID
    val amount: Int // 毎月の固定予算金額
)