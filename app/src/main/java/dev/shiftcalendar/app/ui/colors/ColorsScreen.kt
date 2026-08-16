package dev.shiftcalendar.app.ui.colors

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
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
import dev.shiftcalendar.app.ui.components.BackTopBar
import dev.shiftcalendar.app.ui.components.scheduleTitle
import dev.shiftcalendar.app.ui.components.ticker
import dev.shiftcalendar.app.ui.components.typeName

/** Редактируемая ячейка: (слот, тип). */
private data class Picked(val slot: Int, val type: Int)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColorsScreen(
    repository: ShiftRepository,
    onBack: () -> Unit,
) {
    val app by repository.state.collectAsState()
    val tick = ticker()
    var picked by remember { mutableStateOf<Picked?>(null) }

    Scaffold(topBar = { BackTopBar(stringResource(R.string.colors_title), onBack) }) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(4) { slot ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                        Text(
                            text = scheduleTitle(app, slot + 1),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.size(10.dp))
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            app.shiftTypes.forEach { t ->
                                ColorTile(
                                    color = app.colors.colors[slot + 1 to t.id]
                                        ?: defaultTypeColors.getOrElse(t.id) { 0xFF8A8A8A.toInt() },
                                    label = t.name.ifBlank { typeName(t.id) },
                                    onClick = {
                                        tick()
                                        picked = Picked(slot + 1, t.id)
                                    },
                                )
                            }
                        }
                    }
                }
            }
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
}

/** Плитка цвета в карточке расписания. */
@Composable
private fun ColorTile(
    color: Int,
    label: String,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier.width(54.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .background(Color(color), RoundedCornerShape(12.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                .clickable(onClick = onClick),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            textAlign = TextAlign.Center,
            overflow = TextOverflow.Ellipsis,
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