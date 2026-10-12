package dev.shiftcalendar.app.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.shiftcalendar.app.R
import dev.shiftcalendar.app.data.AppState
import dev.shiftcalendar.app.data.defaultTypeColors
import dev.shiftcalendar.app.ui.components.AppTopBar
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun CalendarScreen(
    vm: CalendarViewModel,
) {
    val state by vm.state.collectAsState()

    Scaffold(
        topBar = { AppTopBar(stringResource(R.string.cal_title)) },
        bottomBar = { TodayBar(onClick = vm::goToday) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .pointerInput(state.monthPopupOpen, state.yearPopupOpen) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        var total = 0f
                        var changed = false
                        val threshold = 40.dp.toPx()
                        while (true) {
                            val ev = awaitPointerEvent()
                            val change = ev.changes.first()
                            if (change.changedToUp()) break
                            if (state.monthPopupOpen || state.yearPopupOpen) break
                            if (!change.isConsumed) total += change.positionChange().x
                            change.consume()
                            if (!changed && total > threshold) { vm.prevMonth(); changed = true }
                            if (!changed && total < -threshold) { vm.nextMonth(); changed = true }
                        }
                    }
                },
        ) {
            MonthSelector(
                state = state,
                monthName = { monthNameRes(it) },
                onPrev = vm::prevMonth,
                onNext = vm::nextMonth,
                onToggleMonth = vm::toggleMonthPopup,
                onToggleYear = vm::toggleYearPopup,
                onSelectMonth = vm::selectMonth,
                onSelectYear = vm::selectYear,
                onDismissPopup = vm::closePopups,
            )
            Spacer(Modifier.height(20.dp))
            WeekdayHeader(app = state.app)
            Spacer(Modifier.height(12.dp))
            CalendarGrid(
                state = state,
                onClickDay = vm::selectDay,
            )
            Spacer(Modifier.height(24.dp))
            LegendRow()
        }
    }

    state.selectedDay?.let { day ->
        DayDetailsSheet(
            day = day,
            app = state.app,
            monthName = { monthNameRes(it) },
            onDismiss = vm::dismissDay,
        )
    }
}

@Composable
private fun MonthSelector(
    state: CalendarScreenState,
    monthName: @Composable (Int) -> String,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onToggleMonth: () -> Unit,
    onToggleYear: () -> Unit,
    onSelectMonth: (Int) -> Unit,
    onSelectYear: (Int) -> Unit,
    onDismissPopup: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onPrev,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerLowest),
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(Modifier.weight(0.5f))
        Box(modifier = Modifier.weight(2.4f)) {
            SelectorButton(
                text = monthName(state.visibleMonth.monthValue),
                open = state.monthPopupOpen,
                onClick = onToggleMonth,
            )
            MonthPopup(
                month = state.visibleMonth.monthValue,
                monthName = monthName,
                open = state.monthPopupOpen,
                onSelect = onSelectMonth,
                onDismiss = onDismissPopup,
            )
        }
        Spacer(Modifier.weight(0.2f))
        Box(modifier = Modifier.weight(1.3f)) {
            SelectorButton(
                text = state.visibleMonth.year.toString(),
                open = state.yearPopupOpen,
                onClick = onToggleYear,
            )
            YearPopup(
                year = state.visibleMonth.year,
                open = state.yearPopupOpen,
                onSelect = onSelectYear,
                onDismiss = onDismissPopup,
            )
        }
        Spacer(Modifier.weight(0.5f))
        IconButton(
            onClick = onNext,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerLowest),
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun SelectorButton(
    text: String,
    open: Boolean,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
            )
            Spacer(Modifier.width(2.dp))
            Icon(
                if (open) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.secondary,
            )
        }
    }
}

@Composable
private fun WeekdayHeader(app: AppState) {
    val ids = if (app.settings.startOfWeekMonday) {
        listOf(R.string.day_mon, R.string.day_tue, R.string.day_wed, R.string.day_thu, R.string.day_fri, R.string.day_sat, R.string.day_sun)
    } else {
        listOf(R.string.day_sun, R.string.day_mon, R.string.day_tue, R.string.day_wed, R.string.day_thu, R.string.day_fri, R.string.day_sat)
    }
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        ids.forEach { id ->
            Text(
                text = stringResource(id),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LegendRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LegendItem(R.string.day_shift, Color(defaultTypeColors[0]))
        LegendItem(R.string.night_shift, Color(defaultTypeColors[1]))
        LegendItem(R.string.day_off, Color(defaultTypeColors[2]))
        LegendItem(R.string.cal_legend_selected, MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun LegendItem(label: Int, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, CircleShape),
        )
        Spacer(Modifier.width(5.dp))
        Text(
            text = stringResource(label),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CalendarGrid(
    state: CalendarScreenState,
    onClickDay: (LocalDate) -> Unit,
) {
    val month = state.visibleMonth
    val startOfWeek = if (state.app.settings.startOfWeekMonday) DayOfWeek.MONDAY else DayOfWeek.SUNDAY
    var offset = month.atDay(1).dayOfWeek.value - startOfWeek.value
    if (offset < 0) offset += 7
    val first = month.atDay(1).minusDays(offset.toLong())
    val cells = (0 until 42).map { first.plusDays(it.toLong()) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        cells.chunked(7).forEach { week ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                week.forEach { date ->
                    DayCell(
                        date = date,
                        app = state.app,
                        isCurrent = date.monthValue == month.monthValue && date.year == month.year,
                        isToday = date == LocalDate.now(),
                        isSelected = date == state.selectedDay,
                        onClick = { onClickDay(date) },
                        modifier = Modifier.weight(1f).aspectRatio(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    app: AppState,
    isCurrent: Boolean,
    isToday: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val highlighted = isSelected || isToday
    val bg = when {
        highlighted -> MaterialTheme.colorScheme.primaryContainer
        isCurrent -> MaterialTheme.colorScheme.surfaceContainerLowest
        else -> Color.Transparent
    }
    val textColor = when {
        !isCurrent -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
        highlighted -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.TopCenter,
    ) {
        Text(
            text = date.dayOfMonth.toString(),
            color = textColor,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (highlighted) FontWeight.Bold else FontWeight.Medium,
            modifier = Modifier.padding(top = 4.dp),
        )
        DayDots(
            date = date,
            app = app,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 6.dp),
        )
    }
}

/** До четырех кружков (слоты 1..4), цвет = тип смены дня. */
@Composable
private fun DayDots(date: LocalDate, app: AppState, modifier: Modifier = Modifier) {
    val slots = (1..4).mapNotNull { slot ->
        app.dayType(slot, date)?.let { type -> slot to type }
    }
    if (slots.isEmpty()) return
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        slots.forEach { (slot, type) ->
            val color = app.colors.colors[slot to type] ?: defaultTypeColors.getOrElse(type) { 0xFF8A8A8A.toInt() }
            if (app.settings.showShiftInitial) {
                val letter = app.shiftTypes.getOrNull(type)?.name?.trim()?.firstOrNull()
                    ?.uppercaseChar()?.toString() ?: (type + 1).toString()
                val letterColor =
                    if (android.graphics.Color.luminance(color) > 0.5f) Color.Black else Color.White
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .background(Color(color), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = letter,
                        color = letterColor,
                        fontSize = 10.sp,
                        lineHeight = 10.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(Color(color), CircleShape),
                )
            }
        }
    }
}

@Composable
private fun TodayBar(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer)
                .clickable(onClick = onClick)
                .padding(horizontal = 32.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Filled.Today,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.today),
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

@Composable
private fun MonthPopup(
    month: Int,
    monthName: @Composable (Int) -> String,
    open: Boolean,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    DropdownMenu(expanded = open, onDismissRequest = onDismiss) {
        (1..12).forEach { m ->
            DropdownMenuItem(
                text = { Text(monthName(m)) },
                onClick = { onSelect(m) },
                trailingIcon = {
                    if (m == month) Icon(Icons.Filled.Check, contentDescription = null)
                },
            )
        }
    }
}

@Composable
private fun YearPopup(
    year: Int,
    open: Boolean,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    DropdownMenu(expanded = open, onDismissRequest = onDismiss) {
        (year - 3..year + 3).forEach { y ->
            DropdownMenuItem(
                text = { Text(y.toString()) },
                onClick = { onSelect(y) },
                trailingIcon = {
                    if (y == year) Icon(Icons.Filled.Check, contentDescription = null)
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DayDetailsSheet(
    day: LocalDate,
    app: AppState,
    monthName: @Composable (Int) -> String,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 24.dp)) {
            val formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy")
            Text(
                text = day.format(formatter),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = dayName(day.dayOfWeek),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
            )
            Spacer(Modifier.height(12.dp))
            var hasData = false
            for (slot in 1..4) {
                val type = app.dayType(slot, day)
                if (type != null && app.shiftTypes.getOrNull(type)?.visible == true) {
                    hasData = true
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val title = app.schedules.getOrNull(slot - 1)?.title?.ifBlank { "—" } ?: "—"
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .background(
                                    Color(app.colors.colors[slot to type] ?: defaultTypeColors.getOrElse(type) { 0xFF8A8A8A.toInt() }),
                                    CircleShape,
                                ),
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "$title  →  ${app.shiftTypes.getOrNull(type)?.name ?: type}",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }
            if (!hasData) {
                Text(
                    text = stringResource(R.string.day_detail_empty),
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
        }
    }
}

@Composable
private fun monthNameRes(month: Int): String {
    val id = when (month) {
        1 -> R.string.month_jan
        2 -> R.string.month_feb
        3 -> R.string.month_mar
        4 -> R.string.month_apr
        5 -> R.string.month_may
        6 -> R.string.month_jun
        7 -> R.string.month_jul
        8 -> R.string.month_aug
        9 -> R.string.month_sep
        10 -> R.string.month_oct
        11 -> R.string.month_nov
        else -> R.string.month_dec
    }
    return stringResource(id)
}

private fun dayName(d: DayOfWeek): String =
    d.getDisplayName(TextStyle.FULL, Locale.getDefault())