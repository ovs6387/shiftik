package dev.shiftcalendar.app.ui.schedules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.shiftcalendar.app.R
import dev.shiftcalendar.app.data.Schedule
import dev.shiftcalendar.app.data.ScheduleRow
import dev.shiftcalendar.app.data.ShiftRepository
import dev.shiftcalendar.app.ui.components.BackTopBar
import dev.shiftcalendar.app.ui.components.TypeDropdown
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleEditorScreen(
    repository: ShiftRepository,
    scheduleId: Int,
    onBack: () -> Unit,
) {
    val app by repository.state.collectAsState()
    val initial = app.schedules.getOrNull(scheduleId - 1) ?: Schedule(scheduleId, "", LocalDate.now(), mutableListOf())

    var title by remember(scheduleId) { mutableStateOf(initial.title) }
    var seed by remember(scheduleId) { mutableStateOf(initial.seedDate) }
    var rows by remember(scheduleId) {
        mutableStateOf(initial.pattern.map { ScheduleRow(it.days, it.shiftTypeId) })
    }
    var dirty by remember(scheduleId) { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var confirm by remember { mutableStateOf<RectAction?>(null) }
    var errorRes by remember { mutableStateOf<Int?>(null) }

    fun leave() {
        if (dirty) confirm = RectAction.Cancel
        else onBack()
    }

    fun runAction(action: RectAction) {
        when (action) {
            RectAction.Purge -> {
                repository.purgeSchedule(scheduleId)
                onBack()
            }
            RectAction.Cancel -> onBack()
            RectAction.Save -> {
                val reason = validate(title, rows)
                if (reason != null) errorRes = reason
                else {
                    repository.saveSchedule(
                        Schedule(scheduleId, title.trim(), seed, rows.toMutableList())
                    )
                    onBack()
                }
            }
        }
    }

    Scaffold(topBar = {
        BackTopBar(stringResource(R.string.shift_config_title) + "  " + scheduleId, onBack = ::leave)
    }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = {
                    title = it.take(15)
                    dirty = true
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.hint_title)) },
                singleLine = true,
            )
            Spacer(Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.seed_date),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                )
                OutlinedButton(onClick = { showDatePicker = true }) {
                    Text(seed.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")))
                }
            }
            Spacer(Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.shift_row),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary,
            )
            Spacer(Modifier.height(4.dp))
            rows.forEachIndexed { i, row ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = if (row.days <= 0) "" else row.days.toString(),
                        onValueChange = {
                            val newDays = it.filter(Char::isDigit).take(3).toIntOrNull() ?: 0
                            rows = rows.mapIndexed { idx, r -> if (idx == i) r.copy(days = newDays) else r }
                            dirty = true
                        },
                        modifier = Modifier.weight(1f),
                        label = { Text(stringResource(R.string.hint_days)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                    Spacer(Modifier.width(8.dp))
                    TypeDropdown(
                        current = row.shiftTypeId,
                        types = app.shiftTypes,
                        onSelect = {
                            rows = rows.mapIndexed { idx, r -> if (idx == i) r.copy(shiftTypeId = it) else r }
                            dirty = true
                        },
                        modifier = Modifier.weight(2f),
                    )
                    IconButton(onClick = {
                        rows = rows.filterIndexed { idx, _ -> idx != i }
                        dirty = true
                    }) {
                        Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.button_delete))
                    }
                }
            }
            TextButton(
                onClick = {
                    rows = rows + ScheduleRow(1, 0)
                    dirty = true
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.add_row))
            }
            Spacer(Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Button(
                    onClick = { confirm = RectAction.Save },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.button_save), fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = { confirm = RectAction.Purge },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.button_purge))
                }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { leave() },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.button_cancel))
            }
            Spacer(Modifier.height(16.dp))
        }
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = seed.toEpochDay() * 86_400_000L,
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let {
                        seed = LocalDate.ofEpochDay(it / 86_400_000L)
                        dirty = true
                    }
                    showDatePicker = false
                }) {
                    Text(stringResource(R.string.button_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.button_cancel))
                }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }

    confirm?.let { action ->
        AlertDialog(
            onDismissRequest = { confirm = null },
            title = { Text(stringResource(R.string.info_dialog)) },
            text = {
                Text(
                    stringResource(
                        when (action) {
                            RectAction.Save -> R.string.button_save_confirm
                            RectAction.Purge -> R.string.button_purge_confirm
                            RectAction.Cancel -> R.string.button_cancel_confirm
                        }
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = { runAction(action) }) {
                    Text(stringResource(R.string.button_yes))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirm = null }) {
                    Text(stringResource(R.string.button_no))
                }
            },
        )
    }

    errorRes?.let { resId ->
        AlertDialog(
            onDismissRequest = { errorRes = null },
            title = { Text(stringResource(R.string.error_dialog)) },
            text = { Text(stringResource(resId)) },
            confirmButton = {
                TextButton(onClick = { errorRes = null }) {
                    Text(stringResource(R.string.button_ok))
                }
            },
        )
    }
}

private enum class RectAction { Save, Purge, Cancel }

private fun validate(title: String, rows: List<ScheduleRow>): Int? {
    if (title.isBlank()) return R.string.error_dialog_title
    if (rows.isEmpty()) return R.string.error_dialog_items
    if (rows.any { it.days < 1 }) return R.string.error_dialog_days
    return null
}