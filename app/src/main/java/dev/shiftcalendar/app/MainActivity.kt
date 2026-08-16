package dev.shiftcalendar.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import dev.shiftcalendar.app.data.ShiftRepository
import dev.shiftcalendar.app.reminder.ReminderScheduler
import dev.shiftcalendar.app.ui.ShiftCalendarApp
import dev.shiftcalendar.app.ui.theme.ShiftCalendarTheme
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
            ShiftCalendarTheme(darkTheme = darkTheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ShiftCalendarApp(repository)
                }
            }
        }
    }
}