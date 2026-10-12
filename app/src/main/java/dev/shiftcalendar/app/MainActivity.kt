package dev.shiftcalendar.app

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.lifecycleScope
import dev.shiftcalendar.app.data.ShiftRepository
import dev.shiftcalendar.app.reminder.ReminderScheduler
import dev.shiftcalendar.app.ui.ShiftCalendarApp
import dev.shiftcalendar.app.ui.theme.ShiftCalendarTheme
import dev.shiftcalendar.app.ui.theme.appThemeStyleOf
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var repository: ShiftRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.navigationBarColor = Color.TRANSPARENT
        repository = ShiftRepository(this)
        // Любое изменение данных/настроек перепланирует будильники о смене.
        lifecycleScope.launch {
            repository.state.collect { ReminderScheduler.scheduleAll(applicationContext, it) }
        }
        setContent {
            val app by repository.state.collectAsState()
            val darkTheme = when (app.settings.themeMode) {
                1 -> false
                2 -> true
                else -> isSystemInDarkTheme()
            }
            var showExactAlarmDialog by remember {
                mutableStateOf(
                    app.settings.reminderEnabled &&
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                        !ReminderScheduler.canScheduleExactAlarms(this@MainActivity)
                )
            }
            ShiftCalendarTheme(
                darkTheme = darkTheme,
                style = appThemeStyleOf(app.settings.themeStyle),
            ) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ShiftCalendarApp(repository)
                }
                if (showExactAlarmDialog) {
                    AlertDialog(
                        onDismissRequest = { showExactAlarmDialog = false },
                        title = { Text(stringResource(R.string.reminder_exact_alarm_title)) },
                        text = { Text(stringResource(R.string.reminder_exact_alarm_message)) },
                        confirmButton = {
                            TextButton(onClick = {
                                startActivity(ReminderScheduler.exactAlarmSettingsIntent())
                                showExactAlarmDialog = false
                            }) {
                                Text(stringResource(R.string.reminder_exact_alarm_open_settings))
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showExactAlarmDialog = false }) {
                                Text(stringResource(R.string.button_cancel))
                            }
                        }
                    )
                }
            }
        }
    }
}