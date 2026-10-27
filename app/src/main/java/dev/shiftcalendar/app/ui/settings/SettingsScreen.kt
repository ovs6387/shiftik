package dev.shiftcalendar.app.ui.settings

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AlarmAdd
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import dev.shiftcalendar.app.R
import dev.shiftcalendar.app.data.ShiftRepository
import dev.shiftcalendar.app.data.ReminderTemplate
import dev.shiftcalendar.app.data.defaultReminderTimes
import dev.shiftcalendar.app.data.nonWorkShiftTypes
import dev.shiftcalendar.app.reminder.ReminderScheduler
import dev.shiftcalendar.app.ui.components.AppTopBar
import dev.shiftcalendar.app.ui.components.ticker
import dev.shiftcalendar.app.ui.theme.AppThemeStyle
import dev.shiftcalendar.app.ui.theme.themeSpecFor
import java.util.Locale

@Composable
fun SettingsScreen(repository: ShiftRepository) {
    val app by repository.state.collectAsState()
    val context = LocalContext.current
    val tick = ticker()
    var confirmReset by remember { mutableStateOf(false) }
    var pickerType by remember { mutableStateOf<Int?>(null) }
    var templatePicker by remember { mutableStateOf<Int?>(null) }
    var showExactAlarmDialog by remember { mutableStateOf(false) }
    var showMirrorNote by remember { mutableStateOf(false) }
    var showOverlayNote by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }

    val exactAlarmLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        val currentSettings = app.settings
        if (ReminderScheduler.canScheduleExactAlarms(context)) {
            repository.updateSettings(
                currentSettings.copy(
                    reminderEnabled = true,
                    reminderTimes = if (currentSettings.reminderTimes.isEmpty()) defaultReminderTimes else currentSettings.reminderTimes,
                )
            )
            if (Build.VERSION.SDK_INT >= 33) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    Scaffold(
        topBar = { AppTopBar(stringResource(R.string.options_title)) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            val settings = app.settings
            Spacer(Modifier.height(4.dp))
            SettingsSection(stringResource(R.string.opt_section_main)) {
                SwitchSetting(
                    icon = Icons.Filled.VolumeUp,
                    title = stringResource(R.string.option_sound),
                    subtitle = stringResource(R.string.opt_sound_sub),
                    checked = settings.sound,
                    onToggle = { repository.updateSettings(settings.copy(sound = !settings.sound)) },
                )
                SwitchSetting(
                    icon = Icons.Filled.DateRange,
                    title = stringResource(R.string.option_monday),
                    subtitle = stringResource(R.string.opt_monday_sub),
                    checked = settings.startOfWeekMonday,
                    onToggle = { repository.updateSettings(settings.copy(startOfWeekMonday = !settings.startOfWeekMonday)) },
                )
                SwitchSetting(
                    icon = Icons.Filled.TextFields,
                    title = stringResource(R.string.option_shift_info),
                    subtitle = stringResource(R.string.opt_shift_info_sub),
                    checked = settings.showShiftInitial,
                    onToggle = { repository.updateSettings(settings.copy(showShiftInitial = !settings.showShiftInitial)) },
                )
                SwitchSetting(
                    icon = Icons.Filled.History,
                    title = stringResource(R.string.option_cycle_past),
                    subtitle = stringResource(R.string.opt_cycle_past_sub),
                    checked = settings.extendPast,
                    last = true,
                    onToggle = { repository.updateSettings(settings.copy(extendPast = !settings.extendPast)) },
                )
            }

            SettingsSection(stringResource(R.string.opt_section_theme)) {
                for (style in AppThemeStyle.entries) {
                    ThemeStyleSetting(
                        icon = Icons.Filled.Palette,
                        label = themeStyleLabel(style),
                        preview = themeSpecFor(style).preview,
                        selected = settings.themeStyle == style.ordinal,
                        onSelect = { repository.updateSettings(settings.copy(themeStyle = style.ordinal)) },
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
                ) {
                    Text(
                        text = stringResource(R.string.option_theme),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                ModeSetting(
                    icon = Icons.Filled.BrightnessAuto,
                    label = stringResource(R.string.theme_system),
                    selected = settings.themeMode == 0,
                    onSelect = { repository.updateSettings(settings.copy(themeMode = 0)) },
                )
                ModeSetting(
                    icon = Icons.Filled.LightMode,
                    label = stringResource(R.string.theme_light),
                    selected = settings.themeMode == 1,
                    onSelect = { repository.updateSettings(settings.copy(themeMode = 1)) },
                )
                ModeSetting(
                    icon = Icons.Filled.DarkMode,
                    label = stringResource(R.string.theme_dark),
                    selected = settings.themeMode == 2,
                    last = true,
                    onSelect = { repository.updateSettings(settings.copy(themeMode = 2)) },
                )
            }

            SettingsSection(stringResource(R.string.opt_section_notify)) {
                SwitchSetting(
                    icon = Icons.Filled.Alarm,
                    title = stringResource(R.string.option_reminder),
                    subtitle = stringResource(R.string.opt_reminder_sub),
                    checked = settings.reminderEnabled,
                    last = !settings.reminderEnabled,
                    onToggle = {
                        val enable = !settings.reminderEnabled
                        if (enable) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                                !ReminderScheduler.canScheduleExactAlarms(context)
                            ) {
                                showExactAlarmDialog = true
                                return@SwitchSetting
                            }
                            if (Build.VERSION.SDK_INT >= 33) {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        } else if (settings.mirrorToSystemClock) {
                            showMirrorNote = true
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
                    SwitchSetting(
                        icon = Icons.Filled.AlarmAdd,
                        title = stringResource(R.string.option_mirror_clock),
                        subtitle = stringResource(R.string.opt_mirror_clock_sub),
                        checked = settings.mirrorToSystemClock,
                        onToggle = {
                            val enable = !settings.mirrorToSystemClock
                            if (!enable) showMirrorNote = true
                            repository.updateSettings(settings.copy(mirrorToSystemClock = enable))
                        },
                    )
                    if (settings.mirrorToSystemClock) {
                        val lifecycleOwner = LocalLifecycleOwner.current
                        var overlayGranted by remember { mutableStateOf(canDrawOverlays(context)) }
                        DisposableEffect(lifecycleOwner) {
                            val observer = LifecycleEventObserver { _, event ->
                                if (event == Lifecycle.Event.ON_RESUME) {
                                    overlayGranted = canDrawOverlays(context)
                                }
                            }
                            lifecycleOwner.lifecycle.addObserver(observer)
                            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showOverlayNote = true }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.option_mirror_overlay),
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                                Text(
                                    text = if (overlayGranted) {
                                        stringResource(R.string.option_mirror_overlay_on)
                                    } else {
                                        stringResource(R.string.option_mirror_overlay_off)
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (overlayGranted) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                )
                            }
                            Icon(
                                Icons.Filled.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                    }
                    val workTypes = app.shiftTypes.filter { it.visible && it.id !in nonWorkShiftTypes }
                    workTypes.forEachIndexed { index, type ->
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
                        if (index != workTypes.lastIndex) HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                    }
                    Text(
                        text = stringResource(R.string.option_reminder_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    )
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                    Text(
                        text = stringResource(R.string.reminder_tpl_section),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 16.dp, top = 14.dp, bottom = 2.dp),
                    )
                    workTypes.forEachIndexed { index, type ->
                        val tpl = settings.reminderTemplates[type.id]
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { templatePicker = type.id }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = type.name.ifBlank { stringResource(R.string.type_n, type.id + 1) },
                                    style = MaterialTheme.typography.titleSmall,
                                )
                                Text(
                                    text = if (tpl != null && (tpl.title.isNotBlank() || tpl.text.isNotBlank()))
                                        stringResource(R.string.reminder_tpl_custom)
                                    else stringResource(R.string.reminder_tpl_reset),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Icon(
                                Icons.Filled.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (index != workTypes.lastIndex) HorizontalDivider(Modifier.padding(horizontal = 16.dp))
                    }
                    Text(
                        text = stringResource(R.string.reminder_tpl_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
            ) {
                Row(modifier = Modifier.padding(16.dp)) {
                    Icon(
                        Icons.Filled.ErrorOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.opt_reset_info),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { confirmReset = true },
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 14.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Filled.RestartAlt,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.option_reset),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.titleSmall,
                    )
                }
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

    val tplType = templatePicker
    if (tplType != null) {
        val settings = app.settings
        val typeName = app.shiftTypes.getOrNull(tplType)?.name
            ?.takeIf { it.isNotBlank() }
            ?: stringResource(R.string.type_n, tplType + 1)
        ReminderTemplateDialog(
            typeName = typeName,
            template = settings.reminderTemplates[tplType],
            onSave = { title, text ->
                val map = settings.reminderTemplates.toMutableMap()
                if (title.isBlank() && text.isBlank()) map.remove(tplType)
                else map[tplType] = ReminderTemplate(title, text)
                repository.updateSettings(settings.copy(reminderTemplates = map))
                templatePicker = null
            },
            onDismiss = { templatePicker = null },
        )
    }

    if (showExactAlarmDialog) {
        AlertDialog(
            onDismissRequest = { showExactAlarmDialog = false },
            title = { Text(stringResource(R.string.reminder_exact_alarm_title)) },
            text = { Text(stringResource(R.string.reminder_exact_alarm_message)) },
            confirmButton = {
                TextButton(onClick = {
                    exactAlarmLauncher.launch(ReminderScheduler.exactAlarmSettingsIntent())
                    showExactAlarmDialog = false
                }) {
                    Text(stringResource(R.string.reminder_exact_alarm_open_settings))
                }
            },
            dismissButton = {
                TextButton(onClick = { showExactAlarmDialog = false }) {
                    Text(stringResource(R.string.button_cancel))
                }
            },
        )
    }

    if (showMirrorNote) {
        AlertDialog(
            onDismissRequest = { showMirrorNote = false },
            title = { Text(stringResource(R.string.mirror_clock_note_title)) },
            text = { Text(stringResource(R.string.mirror_clock_note_message)) },
            confirmButton = {
                TextButton(onClick = { showMirrorNote = false }) {
                    Text(stringResource(R.string.button_ok))
                }
            },
        )
    }

    if (showOverlayNote) {
        val noteBase = stringResource(R.string.mirror_overlay_note_message)
        val noteMiui = if (isMiui()) stringResource(R.string.mirror_overlay_note_miui) else ""
        AlertDialog(
            onDismissRequest = { showOverlayNote = false },
            title = { Text(stringResource(R.string.mirror_overlay_note_title)) },
            text = { Text(if (noteMiui.isEmpty()) noteBase else "$noteBase\n\n$noteMiui") },
            confirmButton = {
                TextButton(onClick = {
                    showOverlayNote = false
                    openOverlaySettings(context)
                }) {
                    Text(stringResource(R.string.mirror_overlay_note_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showOverlayNote = false }) {
                    Text(stringResource(R.string.mirror_overlay_note_cancel))
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
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) { content() }
        }
    }
}

@Composable
private fun SettingIcon(icon: ImageVector) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
    }
}

@Composable
private fun SwitchSetting(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    last: Boolean = false,
    onToggle: () -> Unit,
) {
    val tick = ticker()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                tick()
                onToggle()
            }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SettingIcon(icon)
        Spacer(Modifier.width(14.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(text = title, style = MaterialTheme.typography.titleSmall)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
            )
        }
        Spacer(Modifier.width(8.dp))
        Switch(checked = checked, onCheckedChange = {
            tick()
            onToggle()
        })
    }
    if (!last) HorizontalDivider(Modifier.padding(start = 70.dp))
}

@Composable
private fun ReminderTemplateDialog(
    typeName: String,
    template: ReminderTemplate?,
    onSave: (String, String) -> Unit,
    onDismiss: () -> Unit,
) {
    var title by remember(template) { mutableStateOf(template?.title.orEmpty()) }
    var text by remember(template) { mutableStateOf(template?.text.orEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(typeName) },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.reminder_tpl_title)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text(stringResource(R.string.reminder_tpl_text)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.reminder_tpl_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(title, text)
                onDismiss()
            }) {
                Text(stringResource(R.string.reminder_tpl_save))
            }
        },
        dismissButton = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = {
                    onSave("", "")
                    onDismiss()
                }) {
                    Text(stringResource(R.string.reminder_tpl_reset))
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.button_cancel))
                }
            }
        },
    )
}

@Composable
private fun ThemeStyleSetting(
    icon: ImageVector,
    label: String,
    preview: Color,
    selected: Boolean,
    last: Boolean = false,
    onSelect: () -> Unit,
) {
    val tick = ticker()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                tick()
                onSelect()
            }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SettingIcon(icon)
        Spacer(Modifier.width(14.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.weight(1f),
        )
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(preview),
        )
        Spacer(Modifier.width(10.dp))
        RadioButton(selected = selected, onClick = {
            tick()
            onSelect()
        })
    }
    if (!last) HorizontalDivider(Modifier.padding(start = 70.dp))
}

@Composable
private fun ModeSetting(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    last: Boolean = false,
    onSelect: () -> Unit,
) {
    val tick = ticker()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                tick()
                onSelect()
            }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SettingIcon(icon)
        Spacer(Modifier.width(14.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.weight(1f),
        )
        RadioButton(selected = selected, onClick = {
            tick()
            onSelect()
        })
    }
    if (!last) HorizontalDivider(Modifier.padding(start = 70.dp))
}

@Composable
private fun themeStyleLabel(style: AppThemeStyle): String = stringResource(
    when (style) {
        AppThemeStyle.CLASSIC -> R.string.theme_style_classic
        AppThemeStyle.PERIWINKLE -> R.string.theme_style_periwinkle
    }
)

private fun formatTime(hour: Int, minute: Int): String =
    String.format(Locale.getDefault(), "%02d:%02d", hour, minute)

/** Разрешено ли «Поверх других приложений» — единственное исключение Android из запрета
 *  запускать «Часы» из фона (без него зеркало ставится только при открытии приложения). */
private fun canDrawOverlays(context: android.content.Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context)

/** MIUI (Xiaomi, Redmi, POCO) дополнительно требует своё разрешение
 *  «Открывать новые окна, когда запущено в фоновом режиме» = «Всегда разрешать». */
private fun isMiui(): Boolean = Build.MANUFACTURER.equals("Xiaomi", ignoreCase = true)

private fun openOverlaySettings(context: android.content.Context) {
    // На MIUI экран «Поверх других приложений» открывается только списком всех приложений,
    // а нужное разрешение живёт в «Другие разрешения», поэтому ведём в карточку приложения.
    val intent = if (isMiui()) {
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse("package:${context.packageName}"),
        )
    } else {
        Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}"),
        )
    }.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        // Экран настроек недоступен — ничего страшного, зеркало поставится при открытии приложения.
    }
}

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
