package com.example.kakeibo_compose.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.kakeibo_compose.data.entity.TargetEntity
import com.example.kakeibo_compose.viewmodel.KakeiboViewModel
import com.example.kakeibo_compose.viewmodel.TargetAnalysisResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TargetScreen(viewModel: KakeiboViewModel) {
    val targets by viewModel.allTargets.collectAsState(initial = emptyList())

    // 入力フォームの状態
    var title by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var deadlineText by remember { mutableStateOf("") } // 簡易的に yyyy-MM-dd で入力

    // 詳細ダイアログ表示用の状態
    var selectedTargetForDetail by remember { mutableStateOf<TargetEntity?>(null) }

    // 💡 【ここを追加！】削除確認ダイアログの表示フラグ
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    // 💡 カレンダー（DatePicker）の表示管理フラグと状態
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    // カレンダーで日付が選ばれたら yyyy-MM-dd に変換して deadlineText に入れる
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val selectedDateMillis = datePickerState.selectedDateMillis
                        if (selectedDateMillis != null) {
                            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                            deadlineText = sdf.format(Date(selectedDateMillis))
                        }
                        showDatePicker = false
                    }
                ) { Text("確定") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("キャンセル") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
// ----------------------------------------------------------------------

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("🎯 目標の管理・分析", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("新しい目標を設定する", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("目的（例：温泉旅行、MacBook購入）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = { Text("目標金額 (円)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )

                    // 💡 タップイベントを確実にTextFieldから横取りする魔法のオブジェクト
                    val dateInteractionSource = remember { MutableInteractionSource() }
                    val isDatePressed by dateInteractionSource.collectIsPressedAsState()

                    // タップを検知したらカレンダーを開くフラグを立てる
                    LaunchedEffect(isDatePressed) {
                        if (isDatePressed) {
                            showDatePicker = true
                        }
                    }

                    OutlinedTextField(
                        value = deadlineText,
                        onValueChange = {},
                        readOnly = true, // キーボード入力を完全無効化
                        label = { Text("期限") },
                        placeholder = { Text("タップして選択") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        interactionSource = dateInteractionSource, // 👈 これで確実無比にタップを検知！
                        enabled = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                }

                Button(
                    onClick = {
                        val amount = amountText.toIntOrNull() ?: 0
                        // 💡 簡単なバリデーション：未来の日付かどうかも優しくチェック
                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        val isValidDate = try { sdf.parse(deadlineText)?.after(Date()) == true } catch(e: Exception) { false }

                        if (title.isNotBlank() && amount > 0 && deadlineText.isNotBlank()) {
                            viewModel.addTarget(title, amount, deadlineText)
                            title = ""
                            amountText = ""
                            deadlineText = ""
                        }
                    },
                    modifier = Modifier.align(Alignment.End),
                    enabled = title.isNotBlank() && amountText.isNotBlank() && deadlineText.isNotBlank() && amountText.toIntOrNull() != null
                ) {
                    Text("目標を追加")
                }
            }
        }

        HorizontalDivider()

        Text("現在の目標一覧 (タップで詳細分析)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        // 📋 目標リスト表示
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().weight(1f)
        ) {
            items(targets) { target ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedTargetForDetail = target }, // 💡 タップで詳細分析へ！
                    colors = CardDefaults.cardColors(
                        containerColor = if (target.isCompleted) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.surface
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = target.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (target.isCompleted) Color(0xFF2E7D32) else Color.Unspecified
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("目標額: ¥${target.targetAmount}  /  期限: ${target.deadlineDate}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }

                        // 達成チェックボックス
                        Checkbox(
                            checked = target.isCompleted,
                            onCheckedChange = { isChecked ->
                                viewModel.updateTarget(target.copy(isCompleted = isChecked))
                            }
                        )
                    }
                }
            }
        }
    }

    // ====================================================
    // 📊 ポップアップ：自動収支分析・アドバイス詳細ダイアログ
    // ====================================================
    selectedTargetForDetail?.let { target ->
        // 💡 変数名を includeMinAsset に変更（初期値は true = 考慮する）
        var includeMinAsset by remember { mutableStateOf(true) }

        // 💡 ViewModel の引数に渡します
        val analysisResult by viewModel.getTargetAnalysis(target, includeMinAsset).collectAsState(initial = null)

        AlertDialog(
            onDismissRequest = { selectedTargetForDetail = null },
            title = { Text("📊 目標のシミュレーション分析") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("【目標】${target.title}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    HorizontalDivider()

                    // 💡 チェックボックスのUI（資産防衛ライン連動版）
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Checkbox(
                            checked = includeMinAsset,
                            onCheckedChange = { includeMinAsset = it }
                        )
                        Text(
                            text = "設定した資産防衛ラインを目標に加算する",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    HorizontalDivider()

                    analysisResult?.let { result ->
                        // 残り期間と必要な貯蓄額
                        Text("⏳ 期限までの残り期間: 約 ${String.format("%.1f", result.monthsLeft)} ヶ月", style = MaterialTheme.typography.bodyMedium)
                        Text("💰 不足している金額: ¥${result.amountNeeded} 円", style = MaterialTheme.typography.bodyMedium)
                        Text("📈 過去1年の月平均貯金額: ¥${result.averageMonthlySaving} 円", style = MaterialTheme.typography.bodyMedium)
                        Text("🎯 達成に必要な月間貯金額: ¥${result.requiredMonthlySaving} 円 / 月", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)

                        Spacer(modifier = Modifier.height(8.dp))

                        // システムからのアドバイスメッセージボード
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (result.isOnTrack) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = result.adviceMessage,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (result.isOnTrack) Color(0xFF2E7D32) else Color(0xFFC62828),
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    } ?: run {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedTargetForDetail = null }) { Text("閉じる") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        // 💡 即削除せず、確認ダイアログのフラグを立てる！
                        showDeleteConfirmDialog = true
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                ) {
                    Text("この目標を削除")
                }
            }
        )
    }

    // ====================================================
    // 🚨 【ここを追加！】誤タップ防止：削除最終確認ダイアログ
    // ====================================================
    if (showDeleteConfirmDialog && selectedTargetForDetail != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("🗑️ 目標の削除") },
            text = { Text("「${selectedTargetForDetail!!.title}」を削除しますか？\nこの操作は取り消せません。") },
            confirmButton = {
                Button(
                    onClick = {
                        // 💡 本当に削除する時の処理
                        viewModel.deleteTarget(selectedTargetForDetail!!.id)
                        showDeleteConfirmDialog = false
                        selectedTargetForDetail = null // 詳細ダイアログも一緒に閉じる
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("削除する")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("キャンセル")
                }
            }
        )
    }
}
