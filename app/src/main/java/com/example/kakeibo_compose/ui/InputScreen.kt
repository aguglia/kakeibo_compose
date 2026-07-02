package com.example.kakeibo_compose.ui

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.kakeibo_compose.viewmodel.KakeiboViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InputScreen(
    isIncome: Boolean,
    totalAsset: Int,
    thisMonthExpense: Int,
    commonCategories: List<String>,
    viewModel: KakeiboViewModel
) {
    val context = LocalContext.current
    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var memo by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    LaunchedEffect(isIncome) {
        amount = ""
        category = ""
        memo = ""
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("総資産", style = MaterialTheme.typography.labelMedium)
                    Text("${totalAsset} 円", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }
            Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("今月の出費", style = MaterialTheme.typography.labelMedium)
                    Text("${thisMonthExpense} 円", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text(text = if (isIncome) "収入登録" else "支出登録", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = amount,
            onValueChange = { amount = it },
            label = { Text("金額を入力 (円)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = category,
                onValueChange = { category = it; expanded = true },
                label = { Text(if (isIncome) "カテゴリ (例: 給与)" else "カテゴリ (例: 食費)") },
                modifier = Modifier.fillMaxWidth().menuAnchor(),
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
            )
            if (commonCategories.isNotEmpty()) {
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    commonCategories.forEach { selectionOption ->
                        DropdownMenuItem(
                            text = { Text(selectionOption) },
                            onClick = { category = selectionOption; expanded = false },
                            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = memo,
            onValueChange = { memo = it },
            label = { Text("理由・メモ (空欄でもOK)") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                val amountInt = amount.toIntOrNull()
                if (amountInt == null || amountInt <= 0) {
                    Toast.makeText(context, "正しい金額を入力してください！", Toast.LENGTH_SHORT).show()
                } else if (category.trim().isEmpty()) {
                    Toast.makeText(context, "カテゴリを入力または選択してください！", Toast.LENGTH_SHORT).show()
                } else {
                    viewModel.saveItem(isIncome, category.trim(), amountInt, memo)
                    Toast.makeText(context, "DBに保存しました！", Toast.LENGTH_SHORT).show()
                    amount = ""
                    category = ""
                    memo = ""
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "家計簿に保存", modifier = Modifier.padding(vertical = 4.dp))
        }
    }
}