package com.banktracker.ui.banks

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.banktracker.data.model.Bank

@Composable
fun BanksScreen(vm: BanksViewModel = viewModel()) {
    val banks by vm.banks.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var editingBank by remember { mutableStateOf<Bank?>(null) }
    var deletingBank by remember { mutableStateOf<Bank?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            Spacer(modifier = Modifier.height(16.dp))
            Text("بانک‌ها", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(
                "سرشماره‌های پیامکی بانک‌ها را اینجا تعریف کنید",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (banks.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("هیچ بانکی تعریف نشده است", textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "روی دکمه + کلیک کنید تا بانک اضافه کنید",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(banks, key = { it.id }) { bank ->
                        BankCard(
                            bank = bank,
                            onEdit = { editingBank = bank },
                            onDelete = { deletingBank = bank }
                        )
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "افزودن بانک")
        }
    }

    if (showAddDialog) {
        BankDialog(
            title = "افزودن بانک",
            onConfirm = { name, number -> vm.addBank(name, number); showAddDialog = false },
            onDismiss = { showAddDialog = false }
        )
    }

    editingBank?.let { bank ->
        BankDialog(
            title = "ویرایش بانک",
            initialName = bank.name,
            initialNumber = bank.senderNumber,
            onConfirm = { name, number ->
                vm.updateBank(bank.copy(name = name.trim(), senderNumber = number.trim()))
                editingBank = null
            },
            onDismiss = { editingBank = null }
        )
    }

    deletingBank?.let { bank ->
        AlertDialog(
            onDismissRequest = { deletingBank = null },
            title = { Text("حذف بانک") },
            text = { Text("آیا مطمئن هستید که می‌خواهید «${bank.name}» را حذف کنید؟") },
            confirmButton = {
                Button(
                    onClick = { vm.deleteBank(bank); deletingBank = null },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("حذف") }
            },
            dismissButton = { TextButton(onClick = { deletingBank = null }) { Text("انصراف") } }
        )
    }
}

@Composable
fun BankCard(bank: Bank, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(bank.name, fontWeight = FontWeight.SemiBold)
                Text(
                    bank.senderNumber,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "ویرایش", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
fun BankDialog(
    title: String,
    initialName: String = "",
    initialNumber: String = "",
    onConfirm: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var number by remember { mutableStateOf(initialNumber) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("نام بانک") },
                    placeholder = { Text("مثلاً: بانک ملت") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = number,
                    onValueChange = { number = it },
                    label = { Text("سرشماره پیامک") },
                    placeholder = { Text("مثلاً: 021-4020") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank() && number.isNotBlank()) onConfirm(name, number) },
                enabled = name.isNotBlank() && number.isNotBlank()
            ) { Text("ذخیره") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )
}
