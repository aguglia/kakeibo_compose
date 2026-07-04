package com.example.kakeibo_compose.ui

import android.widget.Toast
import androidx.compose.foundation.layout.*
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
import com.example.kakeibo_compose.data.entity.CategorySelectionItem
import com.example.kakeibo_compose.data.entity.MiddleCategoryEntity
import com.example.kakeibo_compose.viewmodel.KakeiboViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InputScreen(
    isIncome: Boolean,
    totalAsset: Int,
    thisMonthExpense: Int,
    viewModel: KakeiboViewModel
) {
    val context = LocalContext.current

    // 入力フォームの状態
    var amountText by remember { mutableStateOf("") }
    var memoText by remember { mutableStateOf("") }

    // 選択された「中 > 小」カテゴリの状態
    var selectedCategory by remember { mutableStateOf<CategorySelectionItem?>(null) }

    // DBからリストを取得
    val categoryList by viewModel.getCategorySelectionList(isIncome).collectAsState(initial = emptyList())
    // 💡 ダイアログで「既存の中カテゴリ」を選ぶために、中カテゴリの一覧も取得しておく
    val middleCategories by viewModel.getMiddleCategories(isIncome).collectAsState(initial = emptyList())

    // UIの開閉状態
    var expanded by remember { mutableStateOf(false) }
    var showAddSubDialog by remember { mutableStateOf(false) } // 💡 小カテゴリ追加ダイアログの表示状態

    // タブが切り替わったら選択をリセット
    LaunchedEffect(isIncome) {
        selectedCategory = null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 💰 上部インフォメーションボード
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("現在の総資産: $totalAsset 円", style = MaterialTheme.typography.titleMedium)
                if (!isIncome) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("今月の支出合計: $thisMonthExpense 円", style = MaterialTheme.typography.bodyMedium, color = Color.Red)
                }
            }
        }

        Text(
            text = if (isIncome) "💰 収入の登録" else "💸 支出の登録",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        // ----------------------------------------------------
        // 📁 統合されたカテゴリ選択 ＋ 小カテゴリ追加ボタン
        // ----------------------------------------------------
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
                modifier = Modifier.weight(1f)
            ) {
                OutlinedTextField(
                    value = selectedCategory?.displayName ?: "カテゴリを選択してください",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("カテゴリ（必須）") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                    // 前回の警告対応済みのコード
                    modifier = Modifier
                        .menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth()
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    categoryList.forEach { item ->
                        DropdownMenuItem(
                            text = { Text(item.displayName) },
                            onClick = {
                                selectedCategory = item
                                expanded = false
                            }
                        )
                    }
                    if (categoryList.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text("カテゴリがありません") },
                            onClick = { expanded = false },
                            enabled = false
                        )
                    }
                }
            }

            // ➕ 小カテゴリ追加ボタン（復活！）
            Button(
                onClick = {
                    if (middleCategories.isEmpty()) {
                        Toast.makeText(context, "先に設定画面から中カテゴリを作成してください", Toast.LENGTH_SHORT).show()
                    } else {
                        showAddSubDialog = true
                    }
                },
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.size(56.dp)
            ) {
                Text("＋", style = MaterialTheme.typography.titleLarge)
            }
        }

        // 🔢 金額入力
        OutlinedTextField(
            value = amountText,
            onValueChange = { amountText = it },
            label = { Text("金額 (円)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        // ✍️ メモ入力
        OutlinedTextField(
            value = memoText,
            onValueChange = { memoText = it },
            label = { Text("メモ（任意）") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.weight(1f))

        // 💾 登録実行ボタン
        Button(
            onClick = {
                val amount = amountText.toIntOrNull()
                if (selectedCategory == null) {
                    Toast.makeText(context, "カテゴリを必ず選択してください", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                if (amount == null || amount <= 0) {
                    Toast.makeText(context, "正しい金額を入力してください", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                // 保存実行
                viewModel.saveItem(
                    subCategoryId = selectedCategory!!.subId,
                    amount = amount,
                    memo = memoText
                )

                // フォームリセット
                amountText = ""
                memoText = ""
                selectedCategory = null
                Toast.makeText(context, "登録しました！", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("この内容で登録する", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
        }
    }

    // ====================================================
    // 🏷️ ポップアップ：小カテゴリ追加ダイアログ
    // ====================================================
    if (showAddSubDialog) {
        var newSubName by remember { mutableStateOf("") }
        var dialogDropdownExpanded by remember { mutableStateOf(false) }

        // 💡 ユーザーが既にカテゴリを選んでいた場合、その「中カテゴリ」を初期選択状態にしてあげる親切設計
        var dialogSelectedMiddle by remember {
            mutableStateOf(middleCategories.find { it.id == selectedCategory?.middleId })
        }

        AlertDialog(
            onDismissRequest = { showAddSubDialog = false },
            title = { Text("小カテゴリの新規追加") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // ダイアログ内の「中カテゴリ選択」
                    ExposedDropdownMenuBox(
                        expanded = dialogDropdownExpanded,
                        onExpandedChange = { dialogDropdownExpanded = !dialogDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = dialogSelectedMiddle?.name ?: "中カテゴリを選択",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("所属する中カテゴリ") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dialogDropdownExpanded) },
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                            modifier = Modifier
                                .menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = dialogDropdownExpanded,
                            onDismissRequest = { dialogDropdownExpanded = false }
                        ) {
                            middleCategories.forEach { category ->
                                DropdownMenuItem(
                                    text = { Text(category.name) },
                                    onClick = {
                                        dialogSelectedMiddle = category
                                        dialogDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // 新しい小カテゴリ名の入力
                    OutlinedTextField(
                        value = newSubName,
                        onValueChange = { newSubName = it },
                        label = { Text("新しい小カテゴリ名") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (dialogSelectedMiddle == null) {
                            Toast.makeText(context, "中カテゴリを選択してください", Toast.LENGTH_SHORT).show()
                            return@TextButton
                        }
                        if (newSubName.isBlank()) {
                            Toast.makeText(context, "小カテゴリ名を入力してください", Toast.LENGTH_SHORT).show()
                            return@TextButton
                        }
                        // 小カテゴリを追加する（ViewModel側の関数）
                        viewModel.addSubCategory(dialogSelectedMiddle!!.id, newSubName) { success, message ->
                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                            if (success) showAddSubDialog = false
                        }
                    }
                ) { Text("追加") }
            },
            dismissButton = {
                TextButton(onClick = { showAddSubDialog = false }) { Text("キャンセル") }
            }
        )
    }
}