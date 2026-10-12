package dev.shiftcalendar.app.ui.shifttypes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.shiftcalendar.app.R
import dev.shiftcalendar.app.data.defaultTypeColors
import dev.shiftcalendar.app.ui.components.BackTopBar
import dev.shiftcalendar.app.ui.components.ticker
import dev.shiftcalendar.app.ui.components.typeName
import androidx.compose.foundation.layout.Arrangement

@Composable
fun ShiftTypesScreen(
    vm: ShiftTypesViewModel,
    onBack: () -> Unit,
) {
    val state by vm.state.collectAsState()
    val tick = ticker()

    Scaffold(topBar = { BackTopBar(stringResource(R.string.shifts_title), onBack) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Text(
                text = stringResource(R.string.shifts_hint),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                itemsIndexed(state.shiftTypes) { _, t ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateItem(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                        ),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            OutlinedTextField(
                                value = t.name,
                                onValueChange = { vm.rename(t.id, it) },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                placeholder = { Text(typeName(t.id)) },
                                leadingIcon = {
                                    Box(
                                        modifier = Modifier
                                            .size(18.dp)
                                            .background(
                                                Color(defaultTypeColors.getOrElse(t.id) { 0xFF8A8A8A.toInt() }),
                                                CircleShape,
                                            ),
                                    )
                                },
                            )
                            Spacer(Modifier.width(10.dp))
                            Switch(
                                checked = t.visible,
                                onCheckedChange = {
                                    tick()
                                    vm.toggleVisible(t.id)
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}