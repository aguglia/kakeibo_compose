package com.example.kakeibo_compose.ui

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState // 💡 これが絶対に必要です！
import androidx.compose.runtime.getValue        // 💡 これが by のエラーを消す特効薬です！
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.kakeibo_compose.data.entity.MiddleCategoryEntity
import com.example.kakeibo_compose.data.entity.SubCategoryEntity
import com.example.kakeibo_compose.viewmodel.KakeiboViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryManagementScreen(viewModel: KakeiboViewModel) {
    val context = LocalContext.current
    var isIncomeTab by remember { mutableStateOf(false) }

    val middleCategories by viewModel.getMiddleCategories(isIncomeTab).collectAsState(initial = emptyList())
    val budgetList by viewModel.allBudgets.collectAsState(initial = emptyList())

    var editingMiddleCategory by remember { mutableStateOf<MiddleCategoryEntity?>(null) }
    var editingSubCategory by remember { mutableStateOf<Pair<SubCategoryEntity, Int>?>(null) }

    var expandedMiddleCategoryId by remember { mutableStateOf<Int?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = if (isIncomeTab) 1 else 0) {
            Tab(selected = !isIncomeTab, onClick = { isIncomeTab = false; expandedMiddleCategoryId = null }, text = { Text("💸 支出カテゴリ") })
            Tab(selected = isIncomeTab, onClick = { isIncomeTab = true; expandedMiddleCategoryId = null }, text = { Text("💰 収入カテゴリ") })
        }

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(middleCategories, key = { it.id }) { middle ->
                val budget = budgetList.find { it.middleCategoryId == middle.id }
                val isExpanded = expandedMiddleCategoryId == middle.id

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { expandedMiddleCategoryId = if (isExpanded) null else middle.id }
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(text = middle.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                if (!isIncomeTab) {
                                    Text(
                                        text = if (budget != null) "毎月の予算: ${budget.amount} 円" else "毎月の予算: 未設定",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (budget != null) MaterialTheme.colorScheme.primary else Color.Gray
                                    )
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TextButton(onClick = { editingMiddleCategory = middle }) {
                                    Text("編集・予算")
                                }
                                Text(if (isExpanded) "▲" else "▼", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            }
                        }

                        AnimatedVisibility(visible = isExpanded) {
                            SubCategoryListSection(
                                middleId = middle.id,
                                viewModel = viewModel,
                                onSubClick = { sub, parentId ->
                                    editingSubCategory = Pair(sub, parentId)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (editingMiddleCategory != null) {
        val middleEntity = editingMiddleCategory!!
        var inputName by remember { mutableStateOf(middleEntity.name) }
        val currentBudget = budgetList.find { it.middleCategoryId == middleEntity.id }
        var budgetText by remember { mutableStateOf(currentBudget?.amount?.toString() ?: "") }

        AlertDialog(
            onDismissRequest = { editingMiddleCategory = null },
            title = { Text("中カテゴリの編集") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = inputName,
                        onValueChange = { inputName = it },
                        label = { Text("中カテゴリ名") },
                        singleLine = true
                    )
                    if (!isIncomeTab) {
                        OutlinedTextField(
                            value = budgetText,
                            onValueChange = { budgetText = it },
                            label = { Text("毎月の固定予算 (円)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.updateMiddleCategoryName(middleEntity.id, isIncomeTab, inputName) { success, msg ->
                        if (!success) {
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        } else {
                            if (!isIncomeTab) {
                                val amount = budgetText.toIntOrNull()
                                if (amount != null && amount >= 0) {
                                    viewModel.saveBudget(middleEntity.id, amount)
                                }
                            }
                            Toast.makeText(context, "変更を保存しました", Toast.LENGTH_SHORT).show()
                            editingMiddleCategory = null
                        }
                    }
                }) { Text("保存") }
            },
            dismissButton = {
                TextButton(onClick = { editingMiddleCategory = null }) { Text("キャンセル") }
            }
        )
    }

    editingSubCategory?.let { (subEntity, parentMiddleId) ->
        var inputSubName by remember(subEntity.id) { mutableStateOf(subEntity.name) }

        AlertDialog(
            onDismissRequest = { editingSubCategory = null },
            title = { Text("小カテゴリ名の変更") },
            text = {
                OutlinedTextField(
                    value = inputSubName,
                    onValueChange = { inputSubName = it },
                    label = { Text("小カテゴリ名") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.updateSubCategoryName(subEntity.id, parentMiddleId, inputSubName) { success, msg ->
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        if (success) editingSubCategory = null
                    }
                }) { Text("変更") }
            },
            dismissButton = {
                TextButton(onClick = { editingSubCategory = null }) { Text("キャンセル") }
            }
        )
    }
}

// ====================================================
// 小カテゴリ表示専用のコンポーザブル部品（完全に外側に独立）
// ====================================================
@Composable
fun SubCategoryListSection(
    middleId: Int,
    viewModel: KakeiboViewModel,
    onSubClick: (SubCategoryEntity, Int) -> Unit
) {
    // 💡 上部で明示的にインポートしたため、ここで確実に by と collectAsState が型を解決できるようになります！
    val subCategories by viewModel.getSubCategoriesByMiddle(middleId).collectAsState(initial = emptyList())

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        HorizontalDivider(modifier = Modifier.padding(bottom = 8.dp))
        if (subCategories.isEmpty()) {
            Text("小カテゴリがありません。入力画面から追加してください。", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        } else {
            subCategories.forEach { sub ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSubClick(sub, middleId) }
                        .padding(vertical = 8.dp, horizontal = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "• ${sub.name}", style = MaterialTheme.typography.bodyLarge)
                    Text("📝 変更", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                }
            }
        }
    }
}