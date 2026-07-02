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
import com.example.kakeibo_compose.data.entity.KakeiboDisplayItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(kakeiboList: List<KakeiboDisplayItem>) { // 💡 引数を KakeiboDisplayItem に変更！
    var searchQuery by remember { mutableStateOf("") }

    // 💡 メモ、中カテゴリ名、小カテゴリ名からキーワード検索できるフィルタリング機能
    val filteredList = remember(searchQuery, kakeiboList) {
        if (searchQuery.isBlank()) {
            kakeiboList
        } else {
            kakeiboList.filter { item ->
                item.memo.contains(searchQuery, ignoreCase = true) ||
                        item.middleCategoryName.contains(searchQuery, ignoreCase = true) ||
                        item.subCategoryName.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // 🔍 1. 検索バーの設置
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("履歴を検索 (カテゴリ名、メモなど)") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            singleLine = true
        )

        // 2. 履歴リスト表示
        if (filteredList.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    text = if (searchQuery.isBlank()) "履歴がまだありません。" else "該当する履歴が見つかりません。",
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
                        modifier = Modifier.fillMaxWidth(),
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

                                // 💡 「中カテゴリ（小カテゴリ）」の形式できれいに表示
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
}