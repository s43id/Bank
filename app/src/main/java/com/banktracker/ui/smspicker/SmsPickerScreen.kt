package com.banktracker.ui.smspicker

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.banktracker.data.repository.SmsThread
import com.banktracker.util.JalaliUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmsPickerScreen(onBack: () -> Unit, vm: SmsPickerViewModel = viewModel()) {
    val context = LocalContext.current
    val threads by vm.threads.collectAsState()
    val loading by vm.loading.collectAsState()
    val selected by vm.selected.collectAsState()
    val importedCount by vm.importedCount.collectAsState()

    var showRangeDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { vm.load(context.contentResolver) }

    LaunchedEffect(importedCount) {
        importedCount?.let {
            snackbarHostState.showSnackbar(
                if (it > 0) "$it تراکنش جدید وارد شد" else "تراکنش جدیدی پیدا نشد"
            )
            vm.clearResult()
            onBack()
        }
    }

    if (showRangeDialog) {
        DateRangeDialog(
            onSelect = { sinceMs ->
                showRangeDialog = false
                vm.import(context.contentResolver, sinceMs)
            },
            onDismiss = { showRangeDialog = false }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("انتخاب مکالمات SMS") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "بازگشت")
                    }
                }
            )
        },
        bottomBar = {
            if (selected.isNotEmpty()) {
                Surface(tonalElevation = 3.dp) {
                    Button(
                        onClick = { showRangeDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text("وارد کردن از ${selected.size} مکالمه انتخاب‌شده")
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                loading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                threads.isEmpty() -> {
                    Text(
                        "هیچ پیامکی در اینباکس پیدا نشد",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    Column {
                        Text(
                            "مکالمات بانکی خود را انتخاب کنید",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                        LazyColumn(contentPadding = PaddingValues(bottom = 80.dp)) {
                            items(threads, key = { it.address }) { thread ->
                                ThreadItem(
                                    thread = thread,
                                    isSelected = thread.address in selected,
                                    onClick = { vm.toggle(thread.address) }
                                )
                                HorizontalDivider(modifier = Modifier.padding(start = 72.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ThreadItem(thread: SmsThread, isSelected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = isSelected,
            onCheckedChange = { onClick() },
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    thread.displayName,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    JalaliUtil.timestampToJalaliDate(thread.lastDate),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                thread.lastMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "${thread.messageCount} پیامک",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
fun DateRangeDialog(onSelect: (Long) -> Unit, onDismiss: () -> Unit) {
    val now = System.currentTimeMillis()
    val options = listOf(
        "۱ روز اخیر" to now - 24 * 60 * 60 * 1000L,
        "۳ روز اخیر" to now - 3 * 24 * 60 * 60 * 1000L,
        "۱ هفته اخیر" to now - 7 * 24 * 60 * 60 * 1000L,
        "۱ ماه اخیر" to now - 30 * 24 * 60 * 60 * 1000L,
        "همه پیامک‌ها" to 0L,
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("چه بازه‌ای وارد شود؟") },
        text = {
            Column {
                options.forEach { (label, sinceMs) ->
                    TextButton(
                        onClick = { onSelect(sinceMs) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(label, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )
}
