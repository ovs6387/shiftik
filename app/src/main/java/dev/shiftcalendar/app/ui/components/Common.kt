package dev.shiftcalendar.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.shiftcalendar.app.R
import dev.shiftcalendar.app.data.AppState
import dev.shiftcalendar.app.data.ShiftType

/** Логотип приложения: светлая плитка с Bento-знаком. */
@Composable
fun BrandLogo(size: Dp = 36.dp, corner: Dp = 10.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(corner))
            .background(Color(0xFFF8FAFC)),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.ic_launcher_bento),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

/** Верхняя панель основного раздела: логотип + бренд + название экрана. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(title: String) {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BrandLogo()
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        maxLines = 1,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        },
    )
}

/** Стандартная верхняя панель экранов-разделов: назад + название. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackTopBar(title: String, onBack: () -> Unit) {
    TopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
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