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
    // 現在選択されている年月 (初期値は当月)
    var calendar by remember { mutableStateOf(Calendar.getInstance()) }
    val sdf = remember { SimpleDateFormat("yyyy-MM", Locale.getDefault()) }
    val displaySdf = remember { SimpleDateFormat("yyyy年MM月", Locale.getDefault()) }

    val currentYearMonth = sdf.format(calendar.time)

    // 💡 ViewModel から指定した年月の実績データを集計して取得
    val achievementList by viewModel.getMonthlyAchievement(currentYearMonth).collectAsState(initial = emptyList())

    // 総合計の計算
    val totalBudget = achievementList.sumOf { it.budgetAmount }
    val totalActual = achievementList.sumOf { it.actualExpenseAmount }
    val totalRemaining = totalBudget - totalActual

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

        // 📅 年月切り替えコントローラー
        // 📅 年月切り替えコントローラー（安全なテキスト矢印版）
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 💡 アイコンの代わりにシンプルなテキストで「前月」を表現
                TextButton(onClick = {
                    val newCal = calendar.clone() as Calendar
                    newCal.add(Calendar.MONTH, -1)
                    calendar = newCal
                }) {
                    Text("◀", style = MaterialTheme.typography.titleMedium)
                }

                Text(
                    text = displaySdf.format(calendar.time),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                // 💡 アイコンの代わりにシンプルなテキストで「翌月」を表現
                TextButton(onClick = {
                    val newCal = calendar.clone() as Calendar
                    newCal.add(Calendar.MONTH, 1)
                    calendar = newCal
                }) {
                    Text("▶", style = MaterialTheme.typography.titleMedium)
                }
            }
        }

        // 💰 その月の全体サマリーカード
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (totalRemaining >= 0) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("全体の収支状況", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("総予算:", style = MaterialTheme.typography.bodyMedium)
                    Text("¥$totalBudget", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("実際の支出:", style = MaterialTheme.typography.bodyMedium)
                    Text("¥$totalActual", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }

                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(if (totalRemaining >= 0) "予算残高 (黒字):" else "予算超過 (赤字):", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    Text(
                        text = "¥$totalRemaining",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (totalRemaining >= 0) Color(0xFF2E7D32) else Color(0xFFC62828)
                    )
                }
            }
        }

        Text(
            text = "カテゴリ別の内訳",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        // 📋 カテゴリ別の実績リスト
        if (achievementList.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Text("この月の予算履歴または支出データがありません", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().weight(1f)
            ) {
                items(achievementList) { item ->
                    val remaining = item.budgetAmount - item.actualExpenseAmount
                    val progress = if (item.budgetAmount > 0) item.actualExpenseAmount.toFloat() / item.budgetAmount.toFloat() else 0f

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(item.middleCategoryName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                                Text(
                                    text = if (remaining >= 0) "残り ¥$remaining" else "超過 ¥${-remaining}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (remaining >= 0) Color(0xFF2E7D32) else Color(0xFFC62828)
                                )
                            }

                            // 📊 視覚的な進捗バー（予算にどれだけ近づいているか）
                            LinearProgressIndicator(
                                progress = { progress.coerceAtMost(1f) },
                                modifier = Modifier.fillMaxWidth().height(8.dp),
                                color = if (progress <= 0.7f) Color(0xFF4CAF50) else if (progress <= 1.0f) Color(0xFFFF9800) else Color(0xFFF44336),
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("実績: ¥${item.actualExpenseAmount}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                Text("予算: ¥${item.budgetAmount}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            }
                        }
                    }
                }
            }
        }
    }
}