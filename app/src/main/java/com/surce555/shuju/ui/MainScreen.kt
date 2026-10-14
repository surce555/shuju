package com.surce555.shuju.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.surce555.shuju.data.models.DataSource
import com.surce555.shuju.data.models.MarketSnapshot
import com.surce555.shuju.ui.viewmodel.MainViewModel
import com.surce555.shuju.utils.AppLogger

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(vm: MainViewModel = viewModel()) {
    val uiState by vm.uiState.collectAsState()
    val clipboardManager = LocalClipboardManager.current
    var showLogs by remember { mutableStateOf(false) }
    var logText by remember { mutableStateOf("") }
    var showCopied by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        AppLogger.addListener { _ ->
            logText = AppLogger.getLogs()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "八周期行情采集器",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "AI交易数据采集 · crypto-mtf-analyzer",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showLogs = !showLogs }) {
                        Icon(
                            if (showLogs) Icons.Default.List else Icons.Default.Info,
                            contentDescription = "日志",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 12.dp)
        ) {
            DataSourceSelector(
                selected = uiState.selectedSource,
                onSelect = { vm.setDataSource(it) }
            )

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = { vm.fetchData() },
                enabled = !uiState.loading,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                if (uiState.loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("正在采集数据...")
                } else {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("一键采集 8 周期实盘数据", fontWeight = FontWeight.Bold)
                }
            }

            uiState.error?.let { err ->
                Spacer(Modifier.height(4.dp))
                Text(
                    "⚠ $err",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(8.dp))

            if (showLogs) {
                LogPanel(
                    logText = logText,
                    onCopy = { clipboardManager.setText(AnnotatedString(logText)) },
                    onClear = { AppLogger.clear(); logText = "" }
                )
            } else {
                val formattedText = uiState.formattedText
                val snapshot = uiState.snapshot
                if (snapshot != null && formattedText != null) {
                    ResultPanel(
                        formattedText = formattedText,
                        snapshot = snapshot,
                        onCopy = {
                            clipboardManager.setText(AnnotatedString(formattedText))
                            showCopied = true
                        }
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("📊", fontSize = 48.sp)
                            Spacer(Modifier.height(16.dp))
                            Text(
                                "点击上方按钮采集数据",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 16.sp
                            )
                            Text(
                                "数据将自动格式化为 AI 分析文本",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCopied) {
        LaunchedEffect(showCopied) {
            kotlinx.coroutines.delay(2000)
            showCopied = false
        }
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Surface(
                modifier = Modifier.padding(top = 80.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    " ✓ 已复制到剪贴板 ",
                    modifier = Modifier.padding(12.dp, 8.dp),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun DataSourceSelector(selected: DataSource, onSelect: (DataSource) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            "数据源：",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp
        )
        Box {
            OutlinedButton(
                onClick = { expanded = true },
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(selected.displayName, fontSize = 12.sp)
                Icon(
                    Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                DataSource.values().forEach { source ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                source.displayName,
                                color = if (source == selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface
                            )
                        },
                        onClick = {
                            onSelect(source)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ResultPanel(formattedText: String, snapshot: MarketSnapshot, onCopy: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        Button(
            onClick = onCopy,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.primary
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Icon(Icons.Default.ContentCopy, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("一键复制 AI 分析文本", fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(8.dp))

        val btc = snapshot.btc
        val eth = snapshot.eth
        if (btc != null && eth != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CoinCard(
                    modifier = Modifier.weight(1f),
                    symbol = "BTC",
                    price = "%,.2f".format(btc.currentPrice),
                    change = "%.2f".format(btc.change24h),
                    funding = btc.fundingRate?.let { "%.4f".format(it) } ?: "N/A"
                )
                CoinCard(
                    modifier = Modifier.weight(1f),
                    symbol = "ETH",
                    price = "%,.2f".format(eth.currentPrice),
                    change = "%.2f".format(eth.change24h),
                    funding = eth.fundingRate?.let { "%.4f".format(it) } ?: "N/A"
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(8.dp)
        ) {
            val scrollState = rememberScrollState()
            val hScrollState = rememberScrollState()
            Text(
                text = formattedText,
                modifier = Modifier
                    .fillMaxSize()
                    .horizontalScroll(hScrollState)
                    .verticalScroll(scrollState)
                    .padding(12.dp),
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
fun CoinCard(
    modifier: Modifier = Modifier,
    symbol: String,
    price: String,
    change: String,
    funding: String
) {
    val changeDouble = change.toDoubleOrNull() ?: 0.0
    val changeColor = when {
        changeDouble > 0 -> Color(0xFF00C853)
        changeDouble < 0 -> Color(0xFFFF1744)
        else -> MaterialTheme.colorScheme.onSurface
    }
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                symbol,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Text(
                "\$$price",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "${if (changeDouble > 0) "+" else ""}${change}%",
                color = changeColor,
                fontSize = 12.sp
            )
            Text(
                "费率: ${funding}%",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
fun LogPanel(logText: String, onCopy: () -> Unit, onClear: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "调试日志",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Row {
                TextButton(onClick = onCopy) {
                    Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("复制日志", fontSize = 12.sp)
                }
                TextButton(onClick = onClear) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("清空", fontSize = 12.sp)
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFF0A0A14),
            shape = RoundedCornerShape(8.dp)
        ) {
            val scrollState = rememberScrollState()
            LaunchedEffect(logText) {
                scrollState.animateScrollTo(scrollState.maxValue)
            }
            Text(
                text = if (logText.isEmpty()) "暂无日志" else logText,
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(8.dp),
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                color = Color(0xFF00FF88),
                lineHeight = 14.sp
            )
        }
    }
}
