package com.example.kakeibo_compose.ui

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.kakeibo_compose.viewmodel.KakeiboViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingScreen(viewModel: KakeiboViewModel) {
    val context = LocalContext.current

    // DataStoreから現在の設定額をリアルタイム取得
    val currentMinAsset by viewModel.minimumAsset.collectAsState(initial = 100000)

    // 入力フォーム用の状態（初期値は現在の設定額）
    var inputText by remember(currentMinAsset) { mutableStateOf(currentMinAsset.toString()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(
            text = "⚙️ アプリ設定",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "🛡️ 資産防衛ライン（最低維持資産）",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "目標設定や生活防衛のために、常に手元に残しておきたい最低限の金額を設定します。この金額を下回るとアラートが表示されます。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    label = { Text("最低維持金額 (円)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        val amount = inputText.toIntOrNull()
                        if (amount != null && amount >= 0) {
                            viewModel.saveMinimumAsset(amount)
                            Toast.makeText(context, "設定を保存しました", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "正しい金額を入力してください", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.align(androidx.compose.ui.Alignment.End),
                    enabled = inputText.isNotBlank() && inputText.toIntOrNull() != null
                ) {
                    Text("設定を保存")
                }
            }
        }
    }
}