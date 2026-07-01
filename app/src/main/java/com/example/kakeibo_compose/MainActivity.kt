package com.example.kakeibo_compose

import android.app.Application
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ==========================================
// 1. ViewModel（支出・収入の候補を個別に管理）
// ==========================================
class KakeiboViewModel(application: Application) : AndroidViewModel(application) {
    private val kakeiboDao = KakeiboDatabase.getDatabase(application).kakeiboDao()

    val allItems = kakeiboDao.getAllItems()

    // 💡 支出用・収入用のカテゴリランキングをそれぞれ引っ張る
    val commonExpenseCategories = kakeiboDao.getCommonExpenseCategories()
    val commonIncomeCategories = kakeiboDao.getCommonIncomeCategories()

    val totalAsset = allItems.map { items ->
        items.sumOf { if (it.isIncome) it.amount else -it.amount }
    }

    val thisMonthExpense = allItems.map { items ->
        val currentMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
        items.filter { !it.isIncome && it.date.startsWith(currentMonth) }.sumOf { it.amount }
    }

    fun saveItem(isIncome: Boolean, category: String, amount: Int, memo: String) {
        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val entity = KakeiboEntity(
            date = currentDate,
            isIncome = isIncome,
            category = category,
            amount = amount,
            memo = memo
        )
        viewModelScope.launch {
            kakeiboDao.insert(entity)
        }
    }
}

// ==========================================
// 2. Activity
// ==========================================
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

// ==========================================
// 3. メイン画面の土台
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    var selectedTab by remember { mutableStateOf(0) }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val viewModel: KakeiboViewModel = viewModel()

    val kakeiboList by viewModel.allItems.collectAsState(initial = emptyList())
    val totalAsset by viewModel.totalAsset.collectAsState(0)
    val thisMonthExpense by viewModel.thisMonthExpense.collectAsState(0)

    // 💡 それぞれの状態を別々に監視する
    val expenseCategories by viewModel.commonExpenseCategories.collectAsState(initial = emptyList())
    val incomeCategories by viewModel.commonIncomeCategories.collectAsState(initial = emptyList())

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
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        label = { Text("支出") },
                        icon = { Text("💸") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        label = { Text("収入") },
                        icon = { Text("💰") }
                    )
                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        label = { Text("履歴") },
                        icon = { Text("📋") }
                    )
                }
            }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                when (selectedTab) {
                    // 💡 タブ（支出か収入か）に応じて、渡すカテゴリ候補リストを切り替える！
                    0 -> InputScreen(isIncome = false, totalAsset = totalAsset, thisMonthExpense = thisMonthExpense, commonCategories = expenseCategories, viewModel = viewModel)
                    1 -> InputScreen(isIncome = true, totalAsset = totalAsset, thisMonthExpense = thisMonthExpense, commonCategories = incomeCategories, viewModel = viewModel)
                    2 -> HistoryScreen(kakeiboList = kakeiboList)
                }
            }
        }
    }
}

// ==========================================
// 4. 入力画面（ロジックはそのまま！）
// ==========================================
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
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
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

        Text(
            text = if (isIncome) "収入登録" else "支出登録",
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

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = category,
                onValueChange = {
                    category = it
                    expanded = true
                },
                label = { Text(if (isIncome) "カテゴリ (例: 給与)" else "カテゴリ (例: 食費)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
            )

            if (commonCategories.isNotEmpty()) {
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    commonCategories.forEach { selectionOption ->
                        DropdownMenuItem(
                            text = { Text(selectionOption) },
                            onClick = {
                                category = selectionOption
                                expanded = false
                            },
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

// ==========================================
// 5. 履歴一覧画面
// ==========================================
@Composable
fun HistoryScreen(kakeiboList: List<KakeiboEntity>) {
    if (kakeiboList.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("履歴がまだありません。データを登録してね！", style = MaterialTheme.typography.bodyLarge)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(kakeiboList) { item ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = item.date, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = item.category, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            if (item.memo.isNotEmpty()) {
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