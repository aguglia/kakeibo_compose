package com.example.kakeibo_compose.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.* // 💡 これにより getValue / setValue が正しくインポートされます
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

    // 💡 collectAsState のインポートエラーを解消し、型安全にデータを取得します
    val kakeiboList by viewModel.allItems.collectAsState(initial = emptyList())
    val totalAsset by viewModel.totalAsset.collectAsState(initial = 0)
    val thisMonthExpense by viewModel.thisMonthExpense.collectAsState(initial = 0)

    // 💡 型が List<SubCategoryEntity> に進化したデータを受け取ります
    val expenseCategories by viewModel.commonExpenseSubCategories.collectAsState(initial = emptyList())
    val incomeCategories by viewModel.commonIncomeSubCategories.collectAsState(initial = emptyList())

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(modifier = Modifier.height(16.dp))
                Text("メニュー一覧", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                HorizontalDivider()

                NavigationDrawerItem(
                    label = { Text("✍️ 支出登録（初期画面）") },
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
                    label = { Text("⚙️ カテゴリ・予算管理") },
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3; scope.launch { drawerState.close() } },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("家計簿アプリ", fontWeight = FontWeight.Bold) },
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
                NavigationBar {
                    NavigationBarItem(selected = selectedTab == 0, onClick = { selectedTab = 0 }, label = { Text("支出") }, icon = { Text("💸") })
                    NavigationBarItem(selected = selectedTab == 1, onClick = { selectedTab = 1 }, label = { Text("収入") }, icon = { Text("💰") })
                    NavigationBarItem(selected = selectedTab == 2, onClick = { selectedTab = 2 }, label = { Text("履歴") }, icon = { Text("📋") })
                }
            }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                // 💡 ※ InputScreen 側はまだ修正前の古い状態なので一時的に赤文字になる可能性がありますが、
                // 次のステップで InputScreen を完全固定選択＋ポップアップ追加仕様に大改造して一撃で解消します！
                when (selectedTab) {
                    0 -> InputScreen(isIncome = false, totalAsset = totalAsset, thisMonthExpense = thisMonthExpense, commonCategories = expenseCategories, viewModel = viewModel)
                    1 -> InputScreen(isIncome = true, totalAsset = totalAsset, thisMonthExpense = thisMonthExpense, commonCategories = incomeCategories, viewModel = viewModel)
                    2 -> HistoryScreen(kakeiboList = kakeiboList)
                    3 -> CategoryManagementScreen(viewModel = viewModel)
                }
            }
        }
    }
}