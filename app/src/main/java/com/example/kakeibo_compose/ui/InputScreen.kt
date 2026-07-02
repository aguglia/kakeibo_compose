package com.example.kakeibo_compose.ui

import android.widget.Toast
import androidx.compose.foundation.clickable
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
import com.example.kakeibo_compose.data.entity.MiddleCategoryEntity
import com.example.kakeibo_compose.data.entity.SubCategoryEntity
import com.example.kakeibo_compose.viewmodel.KakeiboViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InputScreen(
    isIncome: Boolean,
    totalAsset: Int,
    thisMonthExpense: Int,
    commonCategories: List<SubCategoryEntity>, // MainScreenからの引数（型エラー解消用）
    viewModel: KakeiboViewModel
) {
    val context = LocalContext.current

    // 入力フォームの状態
    var amountText by remember { mutableStateOf("") }
    var memoText by remember { mutableStateOf("") }

    // 選択されたカテゴリの状態
    var selectedMiddleCategory by remember { mutableStateOf<MiddleCategoryEntity?>(null) }
    var selectedSubCategory by remember { mutableStateOf<SubCategoryEntity?>(null) }

    // DBからリアルタイムにその収支タイプの中カテゴリ一覧を取得
    val middleCategories by viewModel.getMiddleCategories(isIncome).collectAsState(initial = emptyList())

    // 現在選択中の中カテゴリに紐づく小カテゴリ一覧をフィルタリングして取得
    // ※今回はシンプルに全小カテゴリ（よく使う順）から、選択中の中カテゴリIDが一致するものに絞り込みます
    val filteredSubCategories = remember(selectedMiddleCategory, commonCategories) {
        if (selectedMiddleCategory == null) emptyList()
        else commonCategories.filter { it.middleCategoryId == selectedMiddleCategory!!.id }
    }

    // ドロップダウンの開閉状態
    var middleDropdownExpanded by remember { mutableStateOf(false) }
    var subDropdownExpanded by remember { mutableStateOf(false) }

    // ポップアップ（ダイアログ）の開閉状態
    var showAddMiddleDialog by remember { mutableStateOf(false) }
    var showAddSubDialog by remember { mutableStateOf(false) }

    // タブ（支出/収入）が切り替わったら選択をリセット
    LaunchedEffect(isIncome) {
        selectedMiddleCategory = null
        selectedSubCategory = null
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
        // 📁 1. 中カテゴリ選択 ＋ 追加ボタン
        // ----------------------------------------------------
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                OutlinedTextField(
                    value = selectedMiddleCategory?.name ?: "選択してください",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("中カテゴリ（必須）") },
                    modifier = Modifier.fillMaxWidth().clickable { middleDropdownExpanded = true },
                    enabled = false, // クリックイベントをBox側で拾うため
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledBorderColor = MaterialTheme.colorScheme.outline
                    )
                )
                // 本物のクリック領域
                Box(modifier = Modifier.matchParentSize().clickable { middleDropdownExpanded = true })

                DropdownMenu(
                    expanded = middleDropdownExpanded,
                    onDismissRequest = { middleDropdownExpanded = false }
                ) {
                    middleCategories.forEach { category ->
                        DropdownMenuItem(
                            text = { Text(category.name) },
                            onClick = {
                                selectedMiddleCategory = category
                                selectedSubCategory = null // 中が変わったら小はリセット
                                middleDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            // ➕ 中カテゴリ追加ボタン
            Button(
                onClick = { showAddMiddleDialog = true },
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.size(56.dp)
            ) {
                Text("＋", style = MaterialTheme.typography.titleLarge)
            }
        }

        // ----------------------------------------------------
        // 🏷️ 2. 小カテゴリ選択 ＋ 追加ボタン
        // ----------------------------------------------------
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                OutlinedTextField(
                    value = selectedSubCategory?.name ?: "選択してください",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("小カテゴリ（必須）") },
                    modifier = Modifier.fillMaxWidth().clickable {
                        if (selectedMiddleCategory != null) subDropdownExpanded = true
                    },
                    enabled = false,
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = if (selectedMiddleCategory != null) MaterialTheme.colorScheme.onSurface else Color.Gray,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledBorderColor = MaterialTheme.colorScheme.outline
                    )
                )
                Box(modifier = Modifier.matchParentSize().clickable {
                    if (selectedMiddleCategory == null) {
                        Toast.makeText(context, "先に中カテゴリを選択してください", Toast.LENGTH_SHORT).show()
                    } else {
                        subDropdownExpanded = true
                    }
                })

                DropdownMenu(
                    expanded = subDropdownExpanded,
                    onDismissRequest = { subDropdownExpanded = false }
                ) {
                    filteredSubCategories.forEach { subCategory ->
                        DropdownMenuItem(
                            text = { Text(subCategory.name) },
                            onClick = {
                                selectedSubCategory = subCategory
                                subDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            // ➕ 小カテゴリ追加ボタン
            Button(
                onClick = {
                    if (selectedMiddleCategory == null) {
                        Toast.makeText(context, "先に中カテゴリを選択してください", Toast.LENGTH_SHORT).show()
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
                if (selectedSubCategory == null) {
                    Toast.makeText(context, "カテゴリを必ず選択してください", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                if (amount == null || amount <= 0) {
                    Toast.makeText(context, "正しい金額を入力してください", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                // IDを渡して保存！
                viewModel.saveItem(
                    subCategoryId = selectedSubCategory!!.id,
                    amount = amount,
                    memo = memoText
                )

                // フォームリセット
                amountText = ""
                memoText = ""
                selectedMiddleCategory = null
                selectedSubCategory = null
                Toast.makeText(context, "登録しました！", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("この内容で登録する", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
        }
    }

    // ====================================================
    // 🏢 ポップアップ：中カテゴリ追加ダイアログ
    // ====================================================
    if (showAddMiddleDialog) {
        var newMiddleName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddMiddleDialog = false },
            title = { Text("中カテゴリの新規追加") },
            text = {
                OutlinedTextField(
                    value = newMiddleName,
                    onValueChange = { newMiddleName = it },
                    label = { Text("中カテゴリ名") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.addMiddleCategory(newMiddleName, isIncome) { success, message ->
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                        if (success) showAddMiddleDialog = false
                    }
                }) { Text("追加") }
            },
            dismissButton = {
                TextButton(onClick = { showAddMiddleDialog = false }) { Text("キャンセル") }
            }
        )
    }

    // ====================================================
    // 🏷️ ポップアップ：小カテゴリ追加ダイアログ
    // ====================================================
    if (showAddSubDialog && selectedMiddleCategory != null) {
        var newSubName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddSubDialog = false },
            title = { Text("「${selectedMiddleCategory!!.name}」に小カテゴリを追加") },
            text = {
                OutlinedTextField(
                    value = newSubName,
                    onValueChange = { newSubName = it },
                    label = { Text("小カテゴリ名") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.addSubCategory(selectedMiddleCategory!!.id, newSubName) { success, message ->
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                        if (success) showAddSubDialog = false
                    }
                }) { Text("追加") }
            },
            dismissButton = {
                TextButton(onClick = { showAddSubDialog = false }) { Text("キャンセル") }
            }
        )
    }
}