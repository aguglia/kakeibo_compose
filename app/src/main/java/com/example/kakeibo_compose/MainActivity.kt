package com.example.kakeibo_compose

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                MainScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    var selectedTab by remember { mutableStateOf(0) }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(modifier = Modifier.height(16.dp))
                Text("メニュー一覧", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                HorizontalDivider()

                NavigationDrawerItem(
                    label = { Text("✍️ 家計簿入力") },
                    selected = selectedTab == 0,
                    onClick = {
                        selectedTab = 0
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    label = { Text("📋 履歴一覧") },
                    selected = selectedTab == 1,
                    onClick = {
                        selectedTab = 1
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    label = { Text("📊 グラフ分析") },
                    selected = selectedTab == 2,
                    onClick = {
                        selectedTab = 2
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("家計簿アプリ", fontWeight = FontWeight.Bold) },
                    // 💡 エラーの元凶だった『Icons.Default.Menu』を完全に排除！
                    // テキストとして「三（漢字の三、またはハンバーガー風の絵文字）」を綺麗に配置しました
                    navigationIcon = {
                        IconButton(onClick = {
                            scope.launch { drawerState.open() }
                        }) {
                            Text(
                                text = "☰",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary,
                        navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            },
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        label = { Text("入力") },
                        icon = { Text("✍️") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        label = { Text("履歴") },
                        icon = { Text("📋") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        label = { Text("分析") },
                        icon = { Text("📊") }
                    )
                }
            }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                when (selectedTab) {
                    0 -> InputScreen()
                    1 -> HistoryScreen()
                    2 -> AnalysisScreen()
                }
            }
        }
    }
}

@Composable
fun InputScreen() {
    val context = LocalContext.current

    var amount by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var memo by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "家計簿入力",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = amount,
            onValueChange = { amount = it },
            label = { Text("金額を入力 (円)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = category,
            onValueChange = { category = it },
            label = { Text("カテゴリ (例: 食費、日用品)") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = memo,
            onValueChange = { memo = it },
            label = { Text("メモ (空欄でもOK)") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (amount.isEmpty()) {
                    Toast.makeText(context, "金額を入力してください！", Toast.LENGTH_SHORT).show()
                } else {
                    val msg = "保存しました！\n金額: ${amount}円\nカテゴリ: ${category}\nメモ: ${memo}"
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
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

@Composable
fun HistoryScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("ここに履歴一覧を表示します", style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun AnalysisScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("ここにグラフ分析を表示します", style = MaterialTheme.typography.bodyLarge)
    }
}