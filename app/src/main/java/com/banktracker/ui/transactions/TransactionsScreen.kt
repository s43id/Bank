package com.banktracker.ui.transactions

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.banktracker.data.model.Transaction
import com.banktracker.util.JalaliUtil
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TransactionsScreen(vm: TransactionsViewModel = viewModel()) {
    val transactions by vm.transactions.collectAsState()
    val availableDates by vm.availableDates.collectAsState()
    val selectedDate by vm.selectedDate.collectAsState()
    val scanning by vm.scanning.collectAsState()
    val scanResult by vm.scanResult.collectAsState()
    val context = LocalContext.current
    var showDateDialog by remember { mutableStateOf(false) }

    scanResult?.let { count ->
        AlertDialog(
            onDismissRequest = { vm.clearScanResult() },
            title = { Text("اسکن پیامک‌ها") },
            text = {
                Text(
                    if (count > 0) "$count تراکنش جدید از صندوق پیامک اضافه شد"
                    else "تراکنش جدیدی یافت نشد"
                )
            },
            confirmButton = {
                TextButton(onClick = { vm.clearScanResult() }) { Text("باشه") }
            }
        )
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("تراکنش‌ها", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Button(
                onClick = { vm.scanInbox(context.contentResolver) },
                enabled = !scanning
            ) {
                if (scanning) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), color = Color.White, strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("اسکن...")
                    }
                } else {
                    Text("اسکن پیامک‌ها")
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("تاریخ:", style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = { showDateDialog = true }) {
                Text(JalaliUtil.toPrettyDate(selectedDate), color = MaterialTheme.colorScheme.primary)
            }
        }

        HorizontalDivider()
        Spacer(modifier = Modifier.height(8.dp))

        if (transactions.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "هیچ تراکنشی برای این روز ثبت نشده است",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(transactions, key = { it.id }) { tx ->
                    TransactionCard(tx)
                }
            }
        }
    }

    if (showDateDialog) {
        AlertDialog(
            onDismissRequest = { showDateDialog = false },
            title = { Text("انتخاب تاریخ") },
            text = {
                if (availableDates.isEmpty()) {
                    Text("هیچ تراکنشی ثبت نشده است", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                        items(availableDates) { date ->
                            TextButton(
                                onClick = { vm.selectDate(date); showDateDialog = false },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    JalaliUtil.toPrettyDate(date),
                                    color = if (date == selectedDate) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = { showDateDialog = false }) { Text("بستن") } }
        )
    }
}

@Composable
fun TransactionCard(tx: Transaction) {
    val isDeposit = tx.type == "DEPOSIT"
    val isUnknown = tx.type == "UNKNOWN"
    val amountColor = when {
        isDeposit -> Color(0xFF2E7D32)
        isUnknown -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> Color(0xFFC62828)
    }
    val typeLabel = when (tx.type) {
        "DEPOSIT" -> "واریز"
        "WITHDRAWAL" -> "برداشت"
        else -> "نامشخص"
    }
    val badgeColor = when (tx.type) {
        "DEPOSIT" -> Color(0xFFD4EDDA)
        "WITHDRAWAL" -> Color(0xFFFFDAD6)
        else -> Color(0xFFF0F0F0)
    }
    val prefix = if (isDeposit) "+" else if (isUnknown) "" else "-"

    val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault())
    val timeStr = timeFmt.format(Date(tx.timestamp))

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(tx.bankName, fontWeight = FontWeight.SemiBold)
                    Text(timeStr, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "$prefix${JalaliUtil.formatAmount(tx.amount)} ریال",
                        color = amountColor,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(color = badgeColor, shape = MaterialTheme.shapes.small) {
                        Text(
                            typeLabel,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            color = amountColor,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }

            if (tx.rawMessage.length <= 120) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    tx.rawMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3
                )
            }
        }
    }
}
