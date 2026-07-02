package com.example.kakeibo_compose.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.kakeibo_compose.data.entity.KakeiboEntity
import com.example.kakeibo_compose.data.local.KakeiboDatabase
import com.example.kakeibo_compose.data.repository.KakeiboRepository
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class KakeiboViewModel(application: Application) : AndroidViewModel(application) {

    // 💡 1. 最初にRepositoryをカチッと作成する
    private val repository: KakeiboRepository = KakeiboRepository(
        KakeiboDatabase.getDatabase(application).kakeiboDao()
    )

    // 💡 2. 宣言と同時に、Repositoryから値を直接代入して初期化！（これでKotlinが完璧に型を理解します）
    val allItems = repository.allItems
    val commonExpenseCategories = repository.commonExpenseCategories
    val commonIncomeCategories = repository.commonIncomeCategories

    val totalAsset = allItems.map { items ->
        items.sumOf { if (it.isIncome) it.amount else -it.amount }
    }

    val thisMonthExpense = allItems.map { items ->
        val currentMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
        items.filter { !it.isIncome && it.date.startsWith(currentMonth) }.sumOf { it.amount }
    }

    fun saveItem(isIncome: Boolean, category: String, amount: Int, memo: String) {
        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val entity = KakeiboEntity(
            date = currentDate,
            isIncome = isIncome,
            category = category,
            amount = amount,
            memo = memo
        )
        viewModelScope.launch {
            repository.insert(entity)
        }
    }
}