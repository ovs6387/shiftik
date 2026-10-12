package dev.shiftcalendar.app.ui.schedules

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.shiftcalendar.app.R
import dev.shiftcalendar.app.data.ShiftRepository
import dev.shiftcalendar.app.data.ScheduleRow
import dev.shiftcalendar.app.ui.components.AppTopBar
import androidx.compose.foundation.layout.WindowInsets
import dev.shiftcalendar.app.ui.components.scheduleTitle
import java.time.format.DateTimeFormatter

@Composable
fun SchedulesScreen(
    repository: ShiftRepository,
    onEdit: (Int) -> Unit,
) {
    val app by repository.state.collectAsState()
    var purgeId by remember { mutableStateOf<Int?>(null) }

    Scaffold(
        topBar = { AppTopBar(stringResource(R.string.shift_list_title)) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            itemsIndexed(app.schedules) { index, schedule ->
                val slot = index + 1
                ScheduleCard(
                    slot = slot,
                    title = scheduleTitle(app, slot),
                    seedDate = schedule.seedDate,
                    pattern = schedule.pattern,
                    onEdit = { onEdit(slot) },
                    onPurge = { purgeId = slot },
                    modifier = Modifier.animateItem(),
                )
            }
        }
    }

    purgeId?.let { id ->
        AlertDialog(
            onDismissRequest = { purgeId = null },
            title = { Text(stringResource(R.string.info_dialog)) },
            text = { Text(stringResource(R.string.button_purge_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    repository.purgeSchedule(id)
                    purgeId = null
                }) {
                    Text(stringResource(R.string.button_yes))
                }
            },
            dismissButton = {
                TextButton(onClick = { purgeId = null }) {
                    Text(stringResource(R.string.button_no))
                }
            },
        )
    }
}

@Composable
private fun ScheduleCard(
    slot: Int,
    title: String,
    seedDate: java.time.LocalDate,
    pattern: List<ScheduleRow>,
    onEdit: () -> Unit,
    onPurge: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(slot.toString(), color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
            if (pattern.isNotEmpty()) {
                Spacer(Modifier.size(6.dp))
                Text(
                    text = stringResource(R.string.seed_date) + ": " +
                        seedDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
                Spacer(Modifier.size(2.dp))
                Text(
                    text = patternPreview(pattern),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
            } else {
                Spacer(Modifier.size(6.dp))
                Text(
                    text = stringResource(R.string.schedule_empty),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                OutlinedButton(onClick = onEdit) {
                    Text(stringResource(R.string.button_edit))
                }
                Spacer(Modifier.width(8.dp))
                Button(onClick = onPurge) {
                    Text(stringResource(R.string.button_purge))
                }
            }
        }
    }
}

@Composable
private fun patternPreview(pattern: List<ScheduleRow>): String {
    val text = StringBuilder()
    pattern.forEachIndexed { i, row ->
        if (i > 0) text.append(" · ")
        text.append(stringResource(R.string.pattern_row, row.days, row.shiftTypeId + 1))
    }
    return text.toString()
}