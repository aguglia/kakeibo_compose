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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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

    var showAddDialog by remember { mutableStateOf(false) }
    var editingMiddleCategory by remember { mutableStateOf<MiddleCategoryEntity?>(null) }
    var editingSubCategory by remember { mutableStateOf<Pair<SubCategoryEntity, Int>?>(null) }
    var expandedMiddleCategoryId by remember { mutableStateOf<Int?>(null) }

    // 💡 削除確認ダイアログの表示を管理する状態
    var showMiddleDeleteConfirm by remember { mutableStateOf<MiddleCategoryEntity?>(null) }
    var showSubDeleteConfirm by remember { mutableStateOf<SubCategoryEntity?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        PrimaryTabRow(selectedTabIndex = if (isIncomeTab) 1 else 0) {
            Tab(selected = !isIncomeTab, onClick = { isIncomeTab = false; expandedMiddleCategoryId = null }, text = { Text("💸 支出カテゴリ") })
            Tab(selected = isIncomeTab, onClick = { isIncomeTab = true; expandedMiddleCategoryId = null }, text = { Text("💰 収入カテゴリ") })
        }

        Box(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            Button(onClick = { showAddDialog = true }) {
                Text(if (isIncomeTab) "＋ 収入カテゴリを追加" else "＋ 支出カテゴリを追加")
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
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

    // 新規追加ダイアログ
    if (showAddDialog) {
        var newCategoryName by remember { mutableStateOf("") }
        var newBudgetText by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text(if (isIncomeTab) "収入カテゴリの追加" else "支出カテゴリの追加") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = newCategoryName, onValueChange = { newCategoryName = it }, label = { Text("カテゴリ名") }, singleLine = true)
                    if (!isIncomeTab) {
                        OutlinedTextField(value = newBudgetText, onValueChange = { newBudgetText = it }, label = { Text("毎月の固定予算 (円)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                    }
                }
            },
            confirmButton = {
                val isInputValid = newCategoryName.isNotBlank() && (isIncomeTab || newBudgetText.toIntOrNull() != null)
                TextButton(
                    onClick = {
                        viewModel.addMiddleCategoryWithBudget(newCategoryName, isIncomeTab, newBudgetText.toIntOrNull()) { success, msg ->
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            if (success) showAddDialog = false
                        }
                    },
                    enabled = isInputValid
                ) { Text("追加") }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("キャンセル") }
            }
        )
    }

    // 中カテゴリの編集ダイアログ
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
                    OutlinedTextField(value = inputName, onValueChange = { inputName = it }, label = { Text("中カテゴリ名") }, singleLine = true)
                    if (!isIncomeTab) {
                        OutlinedTextField(value = budgetText, onValueChange = { budgetText = it }, label = { Text("毎月の固定予算 (円)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                    }
                }
            },
            confirmButton = {
                val isInputValid = inputName.isNotBlank() && (isIncomeTab || budgetText.toIntOrNull() != null)
                TextButton(
                    onClick = {
                        viewModel.updateMiddleCategoryWithBudget(middleEntity.id, inputName, isIncomeTab, budgetText.toIntOrNull()) { success, msg ->
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            if (success) editingMiddleCategory = null
                        }
                    },
                    enabled = isInputValid
                ) { Text("保存") }
            },
            dismissButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(
                        onClick = {
                            // 💡 すぐ削除せず、まず確認ダイアログ用のフラグを立てる
                            showMiddleDeleteConfirm = middleEntity
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("削除")
                    }

                    TextButton(onClick = { editingMiddleCategory = null }) {
                        Text("キャンセル")
                    }
                }
            }
        )
    }

    // 小カテゴリの編集ダイアログ
    editingSubCategory?.let { (subEntity, parentMiddleId) ->
        var inputSubName by remember(subEntity.id) { mutableStateOf(subEntity.name) }

        AlertDialog(
            onDismissRequest = { editingSubCategory = null },
            title = { Text("小カテゴリの編集") },
            text = {
                OutlinedTextField(value = inputSubName, onValueChange = { inputSubName = it }, label = { Text("小カテゴリ名") }, singleLine = true)
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.updateSubCategoryName(subEntity.id, parentMiddleId, inputSubName) { success, msg ->
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            if (success) editingSubCategory = null
                        }
                    },
                    enabled = inputSubName.isNotBlank()
                ) { Text("変更") }
            },
            dismissButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(
                        onClick = {
                            // 💡 すぐ削除せず、まず確認ダイアログ用のフラグを立てる
                            showSubDeleteConfirm = subEntity
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("削除")
                    }

                    TextButton(onClick = { editingSubCategory = null }) {
                        Text("キャンセル")
                    }
                }
            }
        )
    }

    // 🔴 3. 【新設】中カテゴリの削除確認クッションダイアログ
    showMiddleDeleteConfirm?.let { middleEntity ->
        AlertDialog(
            onDismissRequest = { showMiddleDeleteConfirm = null },
            title = { Text("カテゴリの削除") },
            text = { Text("「${middleEntity.name}」を完全に削除しますか？\n（※小カテゴリが残っている場合は削除できません）") },
            confirmButton = {
                TextButton(
                    onClick = {
                        // 確定した時だけ本番の削除処理を走らせる
                        viewModel.deleteMiddleCategorySafety(middleEntity.id) { success, msg ->
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            if (success) {
                                showMiddleDeleteConfirm = null
                                editingMiddleCategory = null // 元の編集画面も一緒に閉じる
                            }
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("削除する")
                }
            },
            dismissButton = {
                TextButton(onClick = { showMiddleDeleteConfirm = null }) {
                    Text("キャンセル")
                }
            }
        )
    }

    // 🔴 4. 【新設】小カテゴリの削除確認クッションダイアログ
    showSubDeleteConfirm?.let { subEntity ->
        AlertDialog(
            onDismissRequest = { showSubDeleteConfirm = null },
            title = { Text("小カテゴリの削除") },
            text = { Text("「${subEntity.name}」を完全に削除しますか？\n（※このカテゴリを使用した会計履歴がある場合は削除できません）") },
            confirmButton = {
                TextButton(
                    onClick = {
                        // 確定した時だけ本番の削除処理を走らせる
                        viewModel.deleteSubCategorySafety(subEntity.id) { success, msg ->
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            if (success) {
                                showSubDeleteConfirm = null
                                editingSubCategory = null // 元の編集画面も一緒に閉じる
                            }
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("削除する")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSubDeleteConfirm = null }) {
                    Text("キャンセル")
                }
            }
        )
    }
}

@Composable
fun SubCategoryListSection(
    middleId: Int,
    viewModel: KakeiboViewModel,
    onSubClick: (SubCategoryEntity, Int) -> Unit
) {
    val subCategories by viewModel.getSubCategoriesByMiddle(middleId).collectAsState(initial = emptyList())

    Column(
        modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 16.dp, bottom = 16.dp),
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
                    Text("📝 編集・削除", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                }
            }
        }
    }
}