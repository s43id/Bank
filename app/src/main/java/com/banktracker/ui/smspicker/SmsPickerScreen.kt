package com.banktracker.ui.smspicker

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.banktracker.data.repository.SmsItem
import com.banktracker.util.JalaliUtil
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmsPickerScreen(onBack: () -> Unit, vm: SmsPickerViewModel = viewModel()) {
    val context = LocalContext.current
    val state by vm.state.collectAsState()
    val selected by vm.selected.collectAsState()

    LaunchedEffect(Unit) { vm.loadSms(context.contentResolver) }

    var showImportedSnack by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(state) {
        if (state is SmsPickerState.Imported) {
            showImportedSnack = (state as SmsPickerState.Imported).count
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(showImportedSnack) {
        showImportedSnack?.let {
            snackbarHostState.showSnackbar("$it پیامک وارد شد")
            onBack()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("انتخاب پیامک‌های بانکی") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "بازگشت")
                    }
                },
                actions = {
                    if (state is SmsPickerState.Loaded) {
                        val items = (state as SmsPickerState.Loaded).items
                        if (selected.size == items.size) {
                            TextButton(onClick = { vm.clearSelection() }) { Text("هیچکدام") }
                        } else {
                            TextButton(onClick = { vm.selectAll(items) }) { Text("همه") }
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (state is SmsPickerState.Loaded && selected.isNotEmpty()) {
                val items = (state as SmsPickerState.Loaded).items
                Surface(tonalElevation = 3.dp) {
                    Button(
                        onClick = { vm.importSelected(context.contentResolver, items) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text("وارد کردن ${selected.size} پیامک انتخاب‌شده")
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (val s = state) {
                is SmsPickerState.Idle, is SmsPickerState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is SmsPickerState.Loaded -> {
                    if (s.items.isEmpty()) {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("هیچ پیامک بانکی پیدا نشد")
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "ابتدا بانک‌های خود را با سرشماره در تب «بانک‌ها» اضافه کنید",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(contentPadding = PaddingValues(bottom = 80.dp)) {
                            itemsIndexed(s.items) { idx, sms ->
                                SmsPickerItem(
                                    sms = sms,
                                    isSelected = idx.toLong() in selected,
                                    onClick = { vm.toggleSelection(idx.toLong()) }
                                )
                                HorizontalDivider()
                            }
                        }
                    }
                }
                is SmsPickerState.Imported -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
            }
        }
    }
}

@Composable
fun SmsPickerItem(sms: SmsItem, isSelected: Boolean, onClick: () -> Unit) {
    val timeStr = remember(sms.date) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(sms.date))
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isSelected) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        } else {
            RadioButton(selected = false, onClick = onClick, modifier = Modifier.size(24.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(sms.bankName, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                Text(
                    "${JalaliUtil.timestampToJalaliDate(sms.date)}  $timeStr",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                sms.preview,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
