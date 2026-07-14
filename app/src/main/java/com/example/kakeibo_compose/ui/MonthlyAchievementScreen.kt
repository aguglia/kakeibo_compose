package com.example.kakeibo_compose.ui

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
import com.example.kakeibo_compose.data.local.MonthlyAchievementItem
import com.example.kakeibo_compose.viewmodel.KakeiboViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthlyAchievementScreen(viewModel: KakeiboViewModel) {
    // 💡 期間切り替えフラグ (true: 月間, false: 年間)
    var isMonthlyMode by remember { mutableStateOf(true) }
    // 💡 収支切り替えフラグ (true: 収入, false: 支出)
    var isIncomeMode by remember { mutableStateOf(false) }

    // 現在選択されている年月 (初期値は当月)
    var calendar by remember { mutableStateOf(Calendar.getInstance()) }
    val sdf = remember { SimpleDateFormat("yyyy-MM", Locale.getDefault()) }
    val sdfYear = remember { SimpleDateFormat("yyyy", Locale.getDefault()) } // 💡 年用フォーマッタ追加
    val displaySdf = remember { SimpleDateFormat("yyyy年MM月", Locale.getDefault()) }
    val displayYearSdf = remember { SimpleDateFormat("yyyy年", Locale.getDefault()) } // 💡 年表示用

    val currentYearMonth = sdf.format(calendar.time)
    val currentYear = sdfYear.format(calendar.time)

    // 💡 モードに合わせて ViewModel からデータを切り替える（isIncome を渡す）
    val achievementList by remember(isMonthlyMode, isIncomeMode, currentYearMonth, currentYear) {
        if (isMonthlyMode) {
            viewModel.getMonthlyAchievement(currentYearMonth, isIncomeMode)
        } else {
            viewModel.getYearlyAchievement(currentYear, isIncomeMode)
        }
    }.collectAsState(initial = emptyList())

    // 総合計の計算
    val totalBudget = achievementList.sumOf { it.budgetAmount }
    val totalActual = achievementList.sumOf { it.actualExpenseAmount }

    // 💡 収入モードなら「実績 - 目標（黒字/赤字逆転）」、支出モードなら「予算 - 実績」にする
    val totalRemaining = if (isIncomeMode) totalActual - totalBudget else totalBudget - totalActual

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "📊 月間・年間実績確認",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        // ==========================================
        // 🎛️ 切り替え用コントローラー（標準のRowとButtonで安全に構築）
        // ==========================================
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 期間：月間 / 年間
            Button(
                onClick = { isMonthlyMode = !isMonthlyMode },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isMonthlyMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (isMonthlyMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Text(if (isMonthlyMode) "月間モード" else "年間モード")
            }

            // 収支：支出 / 収入
            Button(
                onClick = { isIncomeMode = !isIncomeMode },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isIncomeMode) Color(0xFF1976D2) else Color(0xFFD32F2F), // 収入=青、支出=赤で差別化
                    contentColor = Color.White
                )
            ) {
                Text(if (isIncomeMode) "収入一覧" else "支出一覧")
            }
        }

        // 📅 年月切り替えコントローラー（安全なテキスト矢印版）
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                    alpha = 0.4f
                )
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = {
                    val newCal = calendar.clone() as Calendar
                    if (isMonthlyMode) {
                        newCal.add(Calendar.MONTH, -1) // 💡 月間なら1ヶ月引く
                    } else {
                        newCal.add(Calendar.YEAR, -1)  // 💡 年間なら1年引く
                    }
                    calendar = newCal
                }) {
                    Text("◀", style = MaterialTheme.typography.titleMedium)
                }

                Text(
                    text = if (isMonthlyMode) displaySdf.format(calendar.time) else displayYearSdf.format(
                        calendar.time
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                TextButton(onClick = {
                    val newCal = calendar.clone() as Calendar
                    if (isMonthlyMode) {
                        newCal.add(Calendar.MONTH, 1)  // 💡 月間なら1ヶ月足す
                    } else {
                        newCal.add(Calendar.YEAR, 1)   // 💡 年間なら1年足す
                    }
                    calendar = newCal
                }) {
                    Text("▶", style = MaterialTheme.typography.titleMedium)
                }
            }
        }

        // 💡 サマリー用に、現在の表示モードとは「逆」のデータも裏で同時に取得して合計を出す
        val oppositeList by remember(isMonthlyMode, isIncomeMode, currentYearMonth, currentYear) {
            if (isMonthlyMode) {
                viewModel.getMonthlyAchievement(currentYearMonth, !isIncomeMode)
            } else {
                viewModel.getYearlyAchievement(currentYear, !isIncomeMode)
            }
        }.collectAsState(initial = emptyList())

        // 🪙 支出と収入、それぞれの「総予算」と「総実績」を確定させる
        val totalBudget = if (isIncomeMode) oppositeList.sumOf { it.budgetAmount } else achievementList.sumOf { it.budgetAmount }
        val totalActualExpense = if (isIncomeMode) oppositeList.sumOf { it.actualExpenseAmount } else achievementList.sumOf { it.actualExpenseAmount }
        val totalActualIncome = if (isIncomeMode) achievementList.sumOf { it.actualExpenseAmount } else oppositeList.sumOf { it.actualExpenseAmount }

        // 💡 予算との比較：予算残高（総予算 - 実際の総支出）
        val budgetRemaining = totalBudget - totalActualExpense

        // 💰 全体サマリーカード（画像「image_97a765.png」の完全上位互換版）
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (budgetRemaining >= 0) Color(0xFFE8F5E9) else Color(0xFFFFEBEE) // 予算内に収まっていれば緑、オーバーなら赤
            )
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = if (isMonthlyMode) "全体の収支状況" else "全体の収支状況（年間累計）",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                // 🔵 実際の総収入（常に並記して全体の底上げを確認）
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("実際の総収入:", style = MaterialTheme.typography.bodyMedium)
                    Text("¥${String.format("%,d", totalActualIncome)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color(0xFF1976D2))
                }

                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))

                // 📦 予算管理セクション
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("総予算:", style = MaterialTheme.typography.bodyMedium)
                    Text("¥${String.format("%,d", totalBudget)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("実際の総支出:", style = MaterialTheme.typography.bodyMedium)
                    Text("¥${String.format("%,d", totalActualExpense)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }

                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))

                // 👑 予算との比較結果（ここが一番目立つボトムライン）
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    val labelText = if (budgetRemaining >= 0) "予算残高 (黒字):" else "予算超過 (赤字):"
                    Text(labelText, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    Text(
                        text = "¥${String.format("%,d", budgetRemaining)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (budgetRemaining >= 0) Color(0xFF2E7D32) else Color(0xFFC62828)
                    )
                }
            }
        }

        Text(
            text = "カテゴリ別の内訳",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        // 📋 カテゴリ別の実績リスト（収支連動・スマート表示版）
        if (achievementList.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isIncomeMode) "この期間の目標履歴または収入データがありません" else "この期間の予算履歴または支出データがありません",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp), // 少し余白を広げてスッキリ
                modifier = Modifier.fillMaxWidth().weight(1f)
            ) {
                items(achievementList) { item ->
                    // 💡 収入なら「実績 - 目標（プラスが黒字）」、支出なら「予算 - 実績（プラスが残高）」
                    val diff =
                        if (isIncomeMode) item.actualExpenseAmount - item.budgetAmount else item.budgetAmount - item.actualExpenseAmount
                    val progress =
                        if (item.budgetAmount > 0) item.actualExpenseAmount.toFloat() / item.budgetAmount.toFloat() else 0f

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp) // ほんのり立体感
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {

                            // 🔝 上段：カテゴリ名 ＆ 差額ステータス
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = item.middleCategoryName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold
                                )

                                // 💡 収支モード×進捗状態によって、文言と色を完璧にコントロール
                                val (statusText, statusColor) = if (isIncomeMode) {
                                    if (diff >= 0) "目標達成 (+¥${
                                        String.format(
                                            "%,d",
                                            diff
                                        )
                                    })" to Color(0xFF2E7D32) // 緑
                                    else "あと ¥${
                                        String.format(
                                            "%,d",
                                            -diff
                                        )
                                    }" to Color(0xFFC62828) // 赤
                                } else {
                                    if (diff >= 0) "残り ¥${String.format("%,d", diff)}" to Color(
                                        0xFF2E7D32
                                    ) // 緑
                                    else "超過 ¥${
                                        String.format(
                                            "%,d",
                                            -diff
                                        )
                                    }" to Color(0xFFC62828) // 赤
                                }

                                Text(
                                    text = statusText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = statusColor
                                )
                            }

                            // 📊 中段：視覚的な進捗バー（アニメーションしやすいよう StrokeCap も丸く）
                            // 💡 収入は「達成に向けて伸びる爽快な青」、支出は「残り具合で変わる信号機カラー」
                            val progressBarColor = if (isIncomeMode) {
                                Color(0xFF2196F3) // 収入：クリアなブルー
                            } else {
                                if (progress <= 0.7f) Color(0xFF4CAF50)      // 安全：緑
                                else if (progress <= 1.0f) Color(0xFFFF9800) // 注意：オレンジ
                                else Color(0xFFF44336)                       // 超過：赤
                            }

                            LinearProgressIndicator(
                                progress = { progress.coerceAtMost(1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp),
                                color = progressBarColor,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )

                            // 🪙 下段：実績額 ＆ 目標・予算額の並記
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (isIncomeMode) "実際の収入: ¥${
                                        String.format(
                                            "%,d",
                                            item.actualExpenseAmount
                                        )
                                    }" else "実績: ¥${
                                        String.format(
                                            "%,d",
                                            item.actualExpenseAmount
                                        )
                                    }",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = if (isIncomeMode) "目標: ¥${
                                        String.format(
                                            "%,d",
                                            item.budgetAmount
                                        )
                                    }" else "予算: ¥${String.format("%,d", item.budgetAmount)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}