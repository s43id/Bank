package com.banktracker.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.banktracker.util.JalaliUtil

@Composable
fun DashboardScreen(
    onOpenSmsPicker: () -> Unit = {},
    vm: DashboardViewModel = viewModel()
) {
    val todayStats by vm.todayStats.collectAsState()
    val yesterdayStats by vm.yesterdayStats.collectAsState()
    val selectedStats by vm.selectedStats.collectAsState()
    val availableDates by vm.availableDates.collectAsState()
    val selectedDate by vm.selectedDate.collectAsState()
    var showDateDialog by remember { mutableStateOf(false) }

    Scaffold { scaffoldPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(scaffoldPadding).padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text("داشبورد", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            }

            item {
                DayCard(stats = todayStats, containerColor = MaterialTheme.colorScheme.primaryContainer)
            }

            item {
                DayCard(stats = yesterdayStats, containerColor = MaterialTheme.colorScheme.secondaryContainer)
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("انتخاب روز دیگر", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                            OutlinedButton(onClick = { showDateDialog = true }) {
                                Text(JalaliUtil.toPrettyDate(selectedDate), fontSize = 13.sp)
                            }
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
                        StatsRow(stats = selectedStats)
                    }
                }
            }

            item {
                Button(
                    onClick = onOpenSmsPicker,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("وارد کردن پیامک‌های بانکی")
                }
            }
        }
    }

    if (showDateDialog) {
        DatePickerDialog(
            dates = availableDates,
            selected = selectedDate,
            onSelect = { vm.selectDate(it); showDateDialog = false },
            onDismiss = { showDateDialog = false }
        )
    }
}


@Composable
fun DayCard(stats: DayStats, containerColor: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(stats.label, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(JalaliUtil.toPrettyDate(stats.date), style = MaterialTheme.typography.bodySmall)
            }
            Spacer(modifier = Modifier.height(12.dp))
            StatsRow(stats = stats)
        }
    }
}

@Composable
fun StatsRow(stats: DayStats) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        StatColumn(
            label = "واریز",
            amount = stats.totalDeposit,
            color = Color(0xFF2E7D32)
        )
        VerticalDivider(modifier = Modifier.height(50.dp))
        StatColumn(
            label = "برداشت",
            amount = stats.totalWithdrawal,
            color = Color(0xFFC62828)
        )
        VerticalDivider(modifier = Modifier.height(50.dp))
        StatColumn(
            label = "خالص",
            amount = stats.net,
            color = if (stats.net >= 0) Color(0xFF1565C0) else Color(0xFFC62828)
        )
    }
}

@Composable
fun StatColumn(label: String, amount: Long, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            JalaliUtil.formatAmount(amount),
            fontWeight = FontWeight.Bold,
            color = color,
            fontSize = 15.sp
        )
        Text("ریال", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun DatePickerDialog(dates: List<String>, selected: String, onSelect: (String) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("انتخاب تاریخ") },
        text = {
            if (dates.isEmpty()) {
                Text("هیچ تراکنشی ثبت نشده است", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                    items(dates) { date ->
                        TextButton(
                            onClick = { onSelect(date) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                JalaliUtil.toPrettyDate(date),
                                color = if (date == selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("بستن") } }
    )
}
