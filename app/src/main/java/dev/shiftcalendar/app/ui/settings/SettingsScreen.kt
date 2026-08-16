package dev.shiftcalendar.app.ui.settings

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.shiftcalendar.app.R
import dev.shiftcalendar.app.data.ShiftRepository
import dev.shiftcalendar.app.data.defaultReminderTimes
import dev.shiftcalendar.app.data.nonWorkShiftTypes
import dev.shiftcalendar.app.ui.components.BackTopBar
import dev.shiftcalendar.app.ui.components.ticker
import java.util.Locale

@Composable
fun SettingsScreen(
    repository: ShiftRepository,
    onBack: () -> Unit,
) {
    val app by repository.state.collectAsState()
    val tick = ticker()
    var confirmReset by remember { mutableStateOf(false) }
    var pickerType by remember { mutableStateOf<Int?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }

    Scaffold(topBar = { BackTopBar(stringResource(R.string.options_title), onBack) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            val settings = app.settings
            SwitchRow(
                label = stringResource(R.string.option_sound),
                checked = settings.sound,
                onToggle = { repository.updateSettings(settings.copy(sound = !settings.sound)) },
            )
            SwitchRow(
                label = stringResource(R.string.option_monday),
                checked = settings.startOfWeekMonday,
                onToggle = { repository.updateSettings(settings.copy(startOfWeekMonday = !settings.startOfWeekMonday)) },
            )
            SwitchRow(
                label = stringResource(R.string.option_shift_info),
                checked = settings.showShiftInitial,
                onToggle = { repository.updateSettings(settings.copy(showShiftInitial = !settings.showShiftInitial)) },
            )
            SwitchRow(
                label = stringResource(R.string.option_cycle_past),
                checked = settings.extendPast,
                onToggle = { repository.updateSettings(settings.copy(extendPast = !settings.extendPast)) },
            )
            Text(
                text = stringResource(R.string.option_theme),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            )
            ThemeRow(
                label = stringResource(R.string.theme_system),
                selected = settings.themeMode == 0,
                onSelect = { repository.updateSettings(settings.copy(themeMode = 0)) },
            )
            ThemeRow(
                label = stringResource(R.string.theme_light),
                selected = settings.themeMode == 1,
                onSelect = { repository.updateSettings(settings.copy(themeMode = 1)) },
            )
            ThemeRow(
                label = stringResource(R.string.theme_dark),
                selected = settings.themeMode == 2,
                onSelect = { repository.updateSettings(settings.copy(themeMode = 2)) },
            )
            SwitchRow(
                label = stringResource(R.string.option_reminder),
                checked = settings.reminderEnabled,
                onToggle = {
                    val enable = !settings.reminderEnabled
                    if (enable && Build.VERSION.SDK_INT >= 33) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    repository.updateSettings(
                        settings.copy(
                            reminderEnabled = enable,
                            reminderTimes = if (enable && settings.reminderTimes.isEmpty()) defaultReminderTimes else settings.reminderTimes,
                        )
                    )
                },
            )
            if (settings.reminderEnabled) {
                for (type in app.shiftTypes) {
                    if (!type.visible || type.id in nonWorkShiftTypes) continue
                    val time = settings.reminderTimes[type.id]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { pickerType = type.id }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = type.name.ifBlank { stringResource(R.string.type_n, type.id + 1) },
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = time?.let { formatTime(it.first, it.second) }
                                ?: stringResource(R.string.reminder_time_off),
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (time != null) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    HorizontalDivider()
                }
                Text(
                    text = stringResource(R.string.option_reminder_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
                HorizontalDivider()
            }
            OutlinedButton(
                onClick = { confirmReset = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            ) {
                Text(
                    text = stringResource(R.string.option_reset),
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    val picker = pickerType
    if (picker != null) {
        val settings = app.settings
        val typeName = app.shiftTypes.getOrNull(picker)?.name
            ?.takeIf { it.isNotBlank() }
            ?: stringResource(R.string.type_n, picker + 1)
        val existing = settings.reminderTimes[picker]
        var digits by remember {
            mutableStateOf(
                if (existing != null) {
                    val h = existing.first
                    val m = existing.second
                    if (m == 0) h.toString()
                    else "$h${if (m < 10) "0$m" else m}"
                } else ""
            )
        }
        var inputError by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { pickerType = null },
            title = { Text(typeName) },
            text = {
                Column {
                    Text(
                        text = stringResource(R.string.reminder_time_title),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedTextField(
                        value = formatDigits(digits),
                        onValueChange = { input ->
                            digits = input.filter { it.isDigit() }.take(4)
                            inputError = false
                        },
                        singleLine = true,
                        isError = inputError,
                        supportingText = {
                            Text(if (inputError) stringResource(R.string.reminder_time_error)
                            else stringResource(R.string.reminder_time_hint))
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        textStyle = MaterialTheme.typography.headlineMedium.copy(textAlign = TextAlign.Center),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val time = parseTime(digits)
                    if (time == null) {
                        inputError = true
                    } else {
                        tick()
                        val (h, m) = time
                        repository.updateSettings(
                            settings.copy(
                                reminderTimes = settings.reminderTimes + (picker to (h to m)),
                            )
                        )
                        pickerType = null
                    }
                }) {
                    Text(stringResource(R.string.button_ok))
                }
            },
            dismissButton = {
                Row {
                    if (existing != null) {
                        TextButton(onClick = {
                            repository.updateSettings(
                                settings.copy(reminderTimes = settings.reminderTimes - picker)
                            )
                            pickerType = null
                        }) {
                            Text(stringResource(R.string.button_disable))
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = { pickerType = null }) {
                        Text(stringResource(R.string.button_cancel))
                    }
                }
            },
        )
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text(stringResource(R.string.options_title)) },
            text = { Text(stringResource(R.string.option_reset_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    repository.resetAll()
                    confirmReset = false
                }) {
                    Text(
                        text = stringResource(R.string.button_yes),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmReset = false }) {
                    Text(stringResource(R.string.button_no))
                }
            },
        )
    }
}

@Composable
private fun SwitchRow(
    label: String,
    checked: Boolean,
    onToggle: () -> Unit,
) {
    val tick = ticker()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = {
                tick()
                onToggle()
            })
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        Switch(checked = checked, onCheckedChange = {
            tick()
            onToggle()
        })
    }
    HorizontalDivider()
}

@Composable
private fun ThemeRow(
    label: String,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    val tick = ticker()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = {
                tick()
                onSelect()
            })
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        RadioButton(selected = selected, onClick = {
            tick()
            onSelect()
        })
    }
    HorizontalDivider()
}

private fun formatTime(hour: Int, minute: Int): String =
    String.format(Locale.getDefault(), "%02d:%02d", hour, minute)

/** "6" → "6:00", "630" → "6:30", "1830" → "18:30". */
private fun formatDigits(digits: String): String = when {
    digits.isEmpty() -> ""
    digits.length == 1 -> "$digits:00"
    digits.length == 2 -> "$digits:00"
    digits.length == 3 -> "${digits[0]}:${digits.drop(1)}"
    else -> "${digits.take(2)}:${digits.drop(2)}"
}

/** Разбор "6", "630", "1830"; null — время некорректно. */
private fun parseTime(digits: String): Pair<Int, Int>? {
    if (digits.isEmpty()) return null
    val hour: Int
    val minute: Int
    if (digits.length <= 2) {
        hour = digits.toIntOrNull() ?: return null
        minute = 0
    } else {
        hour = digits.take(digits.length - 2).toIntOrNull() ?: return null
        minute = digits.takeLast(2).toIntOrNull() ?: return null
    }
    if (hour !in 0..23 || minute !in 0..59) return null
    return hour to minute
}