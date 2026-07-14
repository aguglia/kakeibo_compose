package com.example.kakeibo_compose.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.kakeibo_compose.viewmodel.KakeiboViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    var selectedTab by remember { mutableStateOf(0) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val viewModel: KakeiboViewModel = viewModel()

    val kakeiboList by viewModel.allItems.collectAsState(initial = emptyList())
    val totalAsset by viewModel.totalAsset.collectAsState(initial = 0)
    val thisMonthExpense by viewModel.thisMonthExpense.collectAsState(initial = 0)
    val hasInitialAsset by viewModel.hasInitialAsset.collectAsState(initial = true)

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(modifier = Modifier.height(16.dp))
                Text("メニュー一覧", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                HorizontalDivider()

                NavigationDrawerItem(
                    label = { Text("✍️ 支出登録") },
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0; scope.launch { drawerState.close() } },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    label = { Text("💰 収入登録") },
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1; scope.launch { drawerState.close() } },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    label = { Text("📋 履歴一覧") },
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2; scope.launch { drawerState.close() } },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    label = { Text("📁 カテゴリ・予算管理") },
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3; scope.launch { drawerState.close() } },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                NavigationDrawerItem(
                    label = { Text("📊 月間・年間実績確認") },
                    selected = selectedTab == 6,
                    onClick = { selectedTab = 6; scope.launch { drawerState.close() } },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                // 💡 【ここを追加！】目標管理メニューを追加
                NavigationDrawerItem(
                    label = { Text("🎯 目標の管理・分析") },
                    selected = selectedTab == 5, // 💡 新しいタブ番号「5」を割り当て
                    onClick = { selectedTab = 5; scope.launch { drawerState.close() } },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                // 💡 【ここを追加！】ドロワーの最下部に設定画面への移動ボタンを配置
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                NavigationDrawerItem(
                    label = { Text("⚙️ アプリ設定（目標・防衛ライン）") },
                    selected = selectedTab == 4, // 💡 設定画面のタブ番号を「4」に指定
                    onClick = { selectedTab = 4; scope.launch { drawerState.close() } },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
            }
        }
    ) {
        if (!hasInitialAsset) {
            var inputAmount by remember { mutableStateOf("") }

            AlertDialog(
                onDismissRequest = { },
                title = { Text("👋 はじめに") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("現在の総資産（貯金額など）を入力してください。\nこの金額をスタートとして家計簿を開始します。")
                        OutlinedTextField(
                            value = inputAmount,
                            onValueChange = { inputAmount = it },
                            label = { Text("初期資産 (円)") },
                            singleLine = true,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val amount = inputAmount.toIntOrNull() ?: 0
                            if (amount >= 0) {
                                viewModel.saveInitialAsset(amount)
                            }
                        },
                        enabled = inputAmount.isNotBlank() && inputAmount.toIntOrNull() != null
                    ) {
                        Text("登録して始める")
                    }
                }
            )
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        // 💡 今開いている画面に合わせて上部のタイトルを優しく可変させます
                        val barTitle = when (selectedTab) {
                            4 -> "アプリ設定"
                            5 -> "目標設定と分析"
                            6 -> "月間・年間実績確認"
                            else -> "家計簿アプリ"
                        }
                        Text(barTitle, fontWeight = FontWeight.Bold)
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Text(text = "☰", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
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
                // 💡 ボトムナビゲーションの下部3つと同期（設定画面を開いている時はどれも選択されない親切設計）
                NavigationBar {
                    NavigationBarItem(selected = selectedTab == 0, onClick = { selectedTab = 0 }, label = { Text("支出") }, icon = { Text("💸") })
                    NavigationBarItem(selected = selectedTab == 1, onClick = { selectedTab = 1 }, label = { Text("収入") }, icon = { Text("💰") })
                    NavigationBarItem(selected = selectedTab == 2, onClick = { selectedTab = 2 }, label = { Text("履歴") }, icon = { Text("📋") })
                }
            }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                // 💡 selectedTab = 4 が叩かれたら、新しく作った SettingScreen をここに描画します！
                when (selectedTab) {
                    0 -> InputScreen(isIncome = false, totalAsset = totalAsset, thisMonthExpense = thisMonthExpense, viewModel = viewModel)
                    1 -> InputScreen(isIncome = true, totalAsset = totalAsset, thisMonthExpense = thisMonthExpense, viewModel = viewModel)
                    2 -> HistoryScreen(kakeiboList = kakeiboList, viewModel = viewModel)
                    3 -> CategoryManagementScreen(viewModel = viewModel)
                    4 -> SettingScreen(viewModel = viewModel)
                    5 -> TargetScreen(viewModel = viewModel)
                    6 -> MonthlyAchievementScreen(viewModel = viewModel)
                }
            }
        }
    }
}