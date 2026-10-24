package dev.shiftcalendar.app.ui.adhoc

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.shiftcalendar.app.R
import dev.shiftcalendar.app.data.AdHoc
import dev.shiftcalendar.app.data.ShiftRepository
import dev.shiftcalendar.app.data.defaultTypeColors
import dev.shiftcalendar.app.ui.components.BackTopBar
import dev.shiftcalendar.app.ui.components.scheduleTitle
import dev.shiftcalendar.app.ui.components.ticker
import dev.shiftcalendar.app.ui.components.typeName
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdHocListScreen(
    vm: AdHocViewModel,
    onBack: () -> Unit,
) {
    val state by vm.state.collectAsState()
    val tick = ticker()

    Scaffold(
        topBar = { BackTopBar(stringResource(R.string.adhoc_title), onBack) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    tick()
                    vm.openAdd()
                },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.adhoc_add)) },
            )
        },
    ) { padding ->
        if (state.app.adHocs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.adhoc_empty),
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(state.app.adHocs) { adHoc ->
                    AdHocCard(
                        adHoc = adHoc,
                        vm = vm,
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }
    }

    if (state.dialogOpen) {
        AdHocDialog(state = state, vm = vm)
    }
}

@Composable
private fun AdHocCard(adHoc: AdHoc, vm: AdHocViewModel, modifier: Modifier = Modifier) {
    val app = vm.state.value.app
    val typeColor = app.colors.colors[adHoc.shift to adHoc.shiftTypeId]
        ?: defaultTypeColors.getOrElse(adHoc.shiftTypeId) { 0xFF8A8A8A.toInt() }
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { vm.openEdit(adHoc) },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .background(Color(typeColor), CircleShape),
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = periodLabel(adHoc),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = "${scheduleTitle(app, adHoc.shift)}  →  " +
                        (app.shiftTypes.getOrNull(adHoc.shiftTypeId)?.name?.ifBlank { typeName(adHoc.shiftTypeId) }
                            ?: typeName(adHoc.shiftTypeId)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
            IconButton(onClick = {
                vm.openEdit(adHoc)
                vm.requestDelete()
            }) {
                Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.button_delete))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdHocDialog(state: AdHocScreenState, vm: AdHocViewModel) {
    val app = state.app

    AlertDialog(
        onDismissRequest = vm::dismiss,
        title = {
            Text(
                if (state.editing == null) stringResource(R.string.adhoc_add)
                else stringResource(R.string.adhoc_edit)
            )
        },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.adhoc_period) + ":",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.size(4.dp))
                OutlinedButton(
                    onClick = vm::requestDatePicker,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    val fmt = DateTimeFormatter.ofPattern("dd.MM.yyyy")
                    val label = when {
                        state.start != null && state.end != null ->
                            state.start!!.format(fmt) + " — " + state.end!!.format(fmt)
                        state.start != null -> state.start!!.format(fmt) + " — …"
                        else -> "—"
                    }
                    Text(label, maxLines = 1)
                }
                Spacer(Modifier.size(8.dp))
                AdHocDropdown(
                    label = stringResource(R.string.adhoc_shift),
                    options = (1..4).map { scheduleTitle(app, it) },
                    selected = state.shift - 1,
                    onSelect = { vm.setShift(it + 1) },
                )
                Spacer(Modifier.size(8.dp))
                AdHocDropdown(
                    label = stringResource(R.string.adhoc_type),
                    options = app.shiftTypes.map { t -> t.name.ifBlank { typeName(t.id) } },
                    selected = state.type,
                    onSelect = vm::setType,
                )
                if (state.editing != null) {
                    Spacer(Modifier.size(8.dp))
                    TextButton(
                        onClick = vm::requestDelete,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            stringResource(R.string.adhoc_delete),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = vm::save,
                enabled = state.start != null && state.end != null,
            ) {
                Text(stringResource(R.string.adhoc_save))
            }
        },
        dismissButton = {
            TextButton(onClick = vm::dismiss) {
                Text(stringResource(R.string.button_cancel))
            }
        },
    )

    if (state.confirmDelete) {
        AlertDialog(
            onDismissRequest = vm::dismiss,
            title = { Text(stringResource(R.string.info_dialog)) },
            text = { Text(stringResource(R.string.adhoc_delete_confirm)) },
            confirmButton = {
                TextButton(onClick = vm::delete) {
                    Text(stringResource(R.string.button_yes))
                }
            },
            dismissButton = {
                TextButton(onClick = vm::dismiss) {
                    Text(stringResource(R.string.button_no))
                }
            },
        )
    }

    if (state.showDatePicker) {
        PeriodPickerDialog(
            start = state.start,
            end = state.end,
            mondayFirst = state.app.settings.startOfWeekMonday,
            onTap = vm::tapDate,
            onConfirm = vm::dismissDatePicker,
            onDismiss = vm::dismissDatePicker,
        )
    }
}

@Composable
private fun PeriodPickerDialog(
    start: LocalDate?,
    end: LocalDate?,
    mondayFirst: Boolean,
    onTap: (LocalDate) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    var month by remember { mutableStateOf(YearMonth.from(start ?: end ?: LocalDate.now())) }
    val ready = start != null && end != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.adhoc_pick_dates)) },
        text = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { month = month.minusMonths(1) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                    Text(
                        text = month.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
                            .replaceFirstChar { it.uppercase() },
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    IconButton(onClick = { month = month.plusMonths(1) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                    }
                }
                Spacer(Modifier.size(4.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    weekDays(mondayFirst).forEach { dow ->
                        Text(
                            text = dow.getDisplayName(TextStyle.SHORT, Locale.getDefault())
                                .replaceFirstChar { it.uppercase() },
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                }
                val startOfWeek = if (mondayFirst) DayOfWeek.MONDAY else DayOfWeek.SUNDAY
                var offset = month.atDay(1).dayOfWeek.value - startOfWeek.value
                if (offset < 0) offset += 7
                val first = month.atDay(1).minusDays(offset.toLong())
                (0 until 42).chunked(7).forEach { week ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        week.forEach { i ->
                            val date = first.plusDays(i.toLong())
                            val inRange = start != null && end != null &&
                                !date.isBefore(start) && !date.isAfter(end)
                            PeriodCell(
                                date = date,
                                isStart = date == start,
                                isEnd = date == end && end != start,
                                inRange = inRange,
                                onClick = { onTap(date) },
                            )
                        }
                    }
                }
                Spacer(Modifier.size(10.dp))
                val fmt = DateTimeFormatter.ofPattern("dd.MM.yyyy")
                Text(
                    text = when {
                        start != null && end != null ->
                            stringResource(R.string.adhoc_range, start.format(fmt), end.format(fmt))
                        start != null -> stringResource(R.string.adhoc_pick_hint)
                        else -> stringResource(R.string.adhoc_pick_hint)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = ready) {
                Text(stringResource(R.string.button_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.button_cancel))
            }
        },
    )
}

private fun weekDays(mondayFirst: Boolean): List<DayOfWeek> =
    if (mondayFirst) {
        listOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
    } else {
        listOf(DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY)
    }

private fun periodLabel(adHoc: AdHoc): String {
    val fmt = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    return if (adHoc.startDate == adHoc.endDate) {
        adHoc.startDate.format(fmt)
    } else {
        "${adHoc.startDate.format(fmt)} — ${adHoc.endDate.format(fmt)}"
    }
}

@Composable
private fun RowScope.PeriodCell(
    date: LocalDate,
    isStart: Boolean,
    isEnd: Boolean,
    inRange: Boolean,
    onClick: () -> Unit,
) {
    val isToday = date == LocalDate.now()
    val bg = when {
        isStart || isEnd -> MaterialTheme.colorScheme.primary
        inRange -> MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
        else -> Color.Transparent
    }
    val textColor = when {
        isStart || isEnd -> MaterialTheme.colorScheme.onPrimary
        isToday -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface
    }
    Box(
        modifier = Modifier
            .weight(1f)
            .aspectRatio(1f)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(bg, CircleShape)
                .then(
                    if (isToday && !isStart && !isEnd && !inRange) {
                        Modifier.border(1.dp, MaterialTheme.colorScheme.primary, CircleShape)
                    } else {
                        Modifier
                    }
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = date.dayOfMonth.toString(),
                color = textColor,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun AdHocDropdown(
    label: String,
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
) {
    var open by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    Box {
        OutlinedButton(
            onClick = { open = true },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = "$label: ${options.getOrNull(selected) ?: ""}",
                maxLines = 1,
            )
        }
        androidx.compose.material3.DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
        ) {
            options.forEachIndexed { i, option ->
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text(option, maxLines = 1) },
                    onClick = {
                        onSelect(i)
                        open = false
                    },
                )
            }
        }
    }
}