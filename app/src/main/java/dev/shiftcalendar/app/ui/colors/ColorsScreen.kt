package dev.shiftcalendar.app.ui.colors

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.shiftcalendar.app.R
import dev.shiftcalendar.app.data.ShiftRepository
import dev.shiftcalendar.app.data.defaultTypeColors
import dev.shiftcalendar.app.ui.components.AppTopBar
import dev.shiftcalendar.app.ui.components.scheduleTitle
import dev.shiftcalendar.app.ui.components.ticker
import dev.shiftcalendar.app.ui.components.typeName

/** Редактируемая ячейка: (слот, тип). */
private data class Picked(val slot: Int, val type: Int)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColorsScreen(repository: ShiftRepository) {
    val app by repository.state.collectAsState()
    val tick = ticker()
    var picked by remember { mutableStateOf<Picked?>(null) }
    var confirmReset by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { AppTopBar(stringResource(R.string.colors_title)) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(4) { slot ->
                ColorsGroupCard(
                    title = scheduleTitle(app, slot + 1),
                    primary = slot == 0,
                    app = app,
                    slot = slot + 1,
                    onPick = { type ->
                        tick()
                        picked = Picked(slot + 1, type)
                    },
                )
            }
            item {
                InfoBlock(
                    title = stringResource(R.string.colors_info_title),
                    text = stringResource(R.string.colors_info_text),
                )
            }
            item {
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
                            Icons.Filled.Refresh,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.colors_reset),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
            item { Spacer(Modifier.size(8.dp)) }
        }
    }

    picked?.let { p ->
        ColorPickerDialog(
            title = "${scheduleTitle(app, p.slot)} — ${app.shiftTypes.getOrNull(p.type)?.name?.ifBlank { typeName(p.type) } ?: typeName(p.type)}",
            current = app.colors.colors[p.slot to p.type] ?: defaultTypeColors.getOrElse(p.type) { 0xFF8A8A8A.toInt() },
            onPick = { repository.setColor(p.slot, p.type, it) },
            onDismiss = { picked = null },
        )
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text(stringResource(R.string.colors_reset)) },
            text = { Text(stringResource(R.string.colors_reset_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    repository.resetColors()
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColorsGroupCard(
    title: String,
    primary: Boolean,
    app: dev.shiftcalendar.app.data.AppState,
    slot: Int,
    onPick: (Int) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .background(
                            if (primary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                            CircleShape,
                        ),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                if (primary) {
                    Text(
                        text = stringResource(R.string.colors_badge_default),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.secondaryContainer,
                                RoundedCornerShape(50),
                            )
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
            }
            Spacer(Modifier.size(14.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                app.shiftTypes.chunked(3).forEach { rowTypes ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        rowTypes.forEach { t ->
                            ColorTile(
                                color = app.colors.colors[slot to t.id]
                                    ?: defaultTypeColors.getOrElse(t.id) { 0xFF8A8A8A.toInt() },
                                label = t.name.ifBlank { typeName(t.id) },
                                onClick = { onPick(t.id) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                        repeat(3 - rowTypes.size) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoBlock(title: String, text: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Row(modifier = Modifier.padding(16.dp)) {
            Icon(
                Icons.Filled.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                Spacer(Modifier.size(4.dp))
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }
    }
}

/** Плитка цвета в карточке расписания (3 в ряд, равные). */
@Composable
private fun ColorTile(
    color: Int,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(39.dp)
                .background(Color(color), RoundedCornerShape(13.dp))
                .clickable(onClick = onClick),
        )
        Spacer(Modifier.size(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            textAlign = TextAlign.Center,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColorPickerDialog(
    title: String,
    current: Int,
    onPick: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val tick = ticker()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                palette.forEach { color ->
                    val selected = color == current
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(Color(color), CircleShape)
                            .border(
                                width = if (selected) 3.dp else 1.dp,
                                color = if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outline,
                                shape = CircleShape,
                            )
                            .clickable {
                                tick()
                                onPick(color)
                                onDismiss()
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (selected) {
                            val checkColor =
                                if (android.graphics.Color.luminance(color) > 0.5f) Color.Black else Color.White
                            Icon(
                                Icons.Filled.Check,
                                contentDescription = null,
                                tint = checkColor,
                                modifier = Modifier.size(26.dp),
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.button_ok))
            }
        },
    )
}

/** Стандартная палитра (нейтральные тона, без привязки к оригиналу). */
private val palette = listOf(
    0xFF000000.toInt(),
    0xFF444444.toInt(),
    0xFF777777.toInt(),
    0xFFAAAAAA.toInt(),
    0xFFFFFFFF.toInt(),
    0xFFB71C1C.toInt(),
    0xFFE53935.toInt(),
    0xFFEF6C00.toInt(),
    0xFFFB8C00.toInt(),
    0xFFF9A825.toInt(),
    0xFFFDD835.toInt(),
    0xFF9E9D24.toInt(),
    0xFFC0CA33.toInt(),
    0xFF2E7D32.toInt(),
    0xFF43A047.toInt(),
    0xFF00897B.toInt(),
    0xFF26A69A.toInt(),
    0xFF00ACC1.toInt(),
    0xFF039BE5.toInt(),
    0xFF1E88E5.toInt(),
    0xFF3949AB.toInt(),
    0xFF5E35B1.toInt(),
    0xFF8E24AA.toInt(),
    0xFFD81B60.toInt(),
    0xFF6D4C41.toInt(),
    0xFF546E7A.toInt(),
)
