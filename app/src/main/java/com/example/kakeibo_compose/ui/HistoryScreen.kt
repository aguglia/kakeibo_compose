package com.example.kakeibo_compose.ui

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.kakeibo_compose.data.entity.KakeiboDisplayItem
import com.example.kakeibo_compose.viewmodel.KakeiboViewModel // 💡 ViewModelをインポート

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    kakeiboList: List<KakeiboDisplayItem>,
    viewModel: KakeiboViewModel
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }

    // 💡 選択されているタブの状態 (0: 全体, 1: 支出, 2: 収入)
    var selectedTabIdx by remember { mutableStateOf(0) }

    // 編集用と削除確認用のステート
    var editingItem by remember { mutableStateOf<KakeiboDisplayItem?>(null) }
    var showDeleteConfirm by remember { mutableStateOf<KakeiboDisplayItem?>(null) }

    // 💡 検索クエリ ＋ タブ の両方でリストをフィルタリングする
    val filteredList = remember(searchQuery, selectedTabIdx, kakeiboList) {
        kakeiboList.filter { item ->
            // 1. タブによるフィルター
            val matchesTab = when (selectedTabIdx) {
                1 -> !item.isIncome // 支出タブ (isIncome が false のもの)
                2 -> item.isIncome  // 収入タブ (isIncome が true のもの)
                else -> true        // 全体タブ
            }

            // 2. 検索キーワードによるフィルター
            val matchesQuery = if (searchQuery.isBlank()) {
                true
            } else {
                item.memo.contains(searchQuery, ignoreCase = true) ||
                        item.middleCategoryName.contains(searchQuery, ignoreCase = true) ||
                        item.subCategoryName.contains(searchQuery, ignoreCase = true)
            }

            matchesTab && matchesQuery
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // 💡 上部のフィルタータブ (CategoryManagementScreen のスタイルを参考に実装)
        PrimaryTabRow(selectedTabIndex = selectedTabIdx) {
            Tab(selected = selectedTabIdx == 0, onClick = { selectedTabIdx = 0 }, text = { Text("📊 全体") })
            Tab(selected = selectedTabIdx == 1, onClick = { selectedTabIdx = 1 }, text = { Text("💸 支出") })
            Tab(selected = selectedTabIdx == 2, onClick = { selectedTabIdx = 2 }, text = { Text("💰 収入") })
        }

        // 🔍 検索バー
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("履歴を検索 (カテゴリ名、メモなど)") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            singleLine = true
        )

        // 履歴リスト表示
        if (filteredList.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    text = if (searchQuery.isBlank()) "該当する履歴がありません。" else "該当する履歴が見つかりません。",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredList, key = { it.id }) { item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { editingItem = item },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = item.date, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = "${item.middleCategoryName} (${item.subCategoryName})",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )

                                if (item.memo.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(text = item.memo, style = MaterialTheme.typography.bodySmall, color = Color.DarkGray)
                                }
                            }
                            Text(
                                text = if (item.isIncome) "+${item.amount} 円" else "-${item.amount} 円",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (item.isIncome) Color(0xFF2E7D32) else Color(0xFFC62828)
                            )
                        }
                    }
                }
            }
        }
    }

    // 📝 編集ダイアログ (前回の修正版：CategorySelectionItem の subId 対応)
    if (editingItem != null) {
        val item = editingItem!!
        var amountText by remember { mutableStateOf(item.amount.toString()) }
        var memoText by remember { mutableStateOf(item.memo) }
        var dropdownExpanded by remember { mutableStateOf(false) }

        val categoryList by viewModel.getCategorySelectionList(item.isIncome).collectAsState(initial = emptyList())
        var selectedCategory by remember(item.id, categoryList) {
            mutableStateOf(categoryList.find { it.subId == item.subCategoryId })
        }

        AlertDialog(
            onDismissRequest = { editingItem = null },
            title = { Text("履歴の編集") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "${item.date} の履歴を編集中",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )

                    ExposedDropdownMenuBox(
                        expanded = dropdownExpanded,
                        onExpandedChange = { dropdownExpanded = !dropdownExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = selectedCategory?.displayName ?: "${item.middleCategoryName} (${item.subCategoryName})",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("カテゴリ") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                            modifier = Modifier
                                .menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth()
                        )

                        ExposedDropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false }
                        ) {
                            categoryList.forEach { categoryItem ->
                                DropdownMenuItem(
                                    text = { Text(categoryItem.displayName) },
                                    onClick = {
                                        selectedCategory = categoryItem
                                        dropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = { Text("金額 (円)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = memoText,
                        onValueChange = { memoText = it },
                        label = { Text("メモ") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                val amount = amountText.toIntOrNull()
                val finalSubCategoryId = selectedCategory?.subId ?: item.subCategoryId

                TextButton(
                    onClick = {
                        if (amount != null && amount > 0) {
                            viewModel.updateKakeibo(
                                id = item.id,
                                amount = amount,
                                memo = memoText,
                                subCategoryId = finalSubCategoryId
                            )
                            Toast.makeText(context, "変更を保存しました", Toast.LENGTH_SHORT).show()
                            editingItem = null
                        } else {
                            Toast.makeText(context, "正しい金額を入力してください", Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled = amountText.isNotBlank()
                ) { Text("保存") }
            },
            dismissButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(
                        onClick = { showDeleteConfirm = item },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) { Text("削除") }

                    TextButton(onClick = { editingItem = null }) { Text("キャンセル") }
                }
            }
        )
    }

    // 🗑️ 削除確認ダイアログ
    showDeleteConfirm?.let { item ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text("履歴の削除") },
            text = {
                Text("この会計履歴を完全に削除しますか？\n\n金額: ${if (item.isIncome) "+" else "-"}${item.amount}円\n内容: ${item.middleCategoryName}")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteKakeibo(item.id)
                        Toast.makeText(context, "履歴を削除しました", Toast.LENGTH_SHORT).show()
                        showDeleteConfirm = null
                        editingItem = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("削除する") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = null }) { Text("キャンセル") }
            }
        )
    }
}