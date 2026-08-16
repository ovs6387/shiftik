package dev.shiftcalendar.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import dev.shiftcalendar.app.R
import dev.shiftcalendar.app.data.AppState
import dev.shiftcalendar.app.data.ShiftType

/** Стандартная верхняя панель экранов-разделов: назад + название. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackTopBar(title: String, onBack: () -> Unit) {
    TopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                )
            }
        },
    )
}

/** Легкий тактильный отклик: возвращает лямбду для вызова в обработчиках нажатий. */
@Composable
fun ticker(): () -> Unit {
    val haptic = LocalHapticFeedback.current
    return { haptic.performHapticFeedback(HapticFeedbackType.LongPress) }
}

/** Название расписания по номеру слота (1..4), с запасным вариантом. */
@Composable
fun scheduleTitle(app: AppState, slot: Int): String =
    app.schedules.getOrNull(slot - 1)?.title?.ifBlank { stringResource(R.string.slot_name, slot) }
        ?: stringResource(R.string.slot_name, slot)

/** Запасное имя типа смены. */
@Composable
fun typeName(type: Int): String = stringResource(R.string.type_n, type + 1)

/** Выпадающий список типов смен. */
@Composable
fun TypeDropdown(
    current: Int,
    types: List<ShiftType>,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedButton(
            onClick = { open = true },
            modifier = Modifier.fillMaxWidth(),
        ) {
            val name = types.getOrNull(current)?.name
            Text(
                text = if (name.isNullOrBlank()) typeName(current) else name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            types.forEach { t ->
                DropdownMenuItem(
                    text = {
                        Text(if (t.name.isBlank()) typeName(t.id) else t.name)
                    },
                    onClick = {
                        onSelect(t.id)
                        open = false
                    },
                )
            }
        }
    }
}