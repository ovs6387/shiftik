package dev.shiftcalendar.app.ui

import android.view.SoundEffectConstants
import android.view.ViewConfiguration
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.shiftcalendar.app.R
import dev.shiftcalendar.app.data.ShiftRepository
import dev.shiftcalendar.app.ui.adhoc.AdHocListScreen
import dev.shiftcalendar.app.ui.adhoc.AdHocViewModel
import dev.shiftcalendar.app.ui.calendar.CalendarScreen
import dev.shiftcalendar.app.ui.calendar.CalendarViewModel
import dev.shiftcalendar.app.ui.colors.ColorsScreen
import dev.shiftcalendar.app.ui.help.DeveloperScreen
import dev.shiftcalendar.app.ui.help.HelpScreen
import dev.shiftcalendar.app.ui.schedules.ScheduleEditorScreen
import dev.shiftcalendar.app.ui.schedules.SchedulesScreen
import dev.shiftcalendar.app.ui.settings.SettingsScreen
import dev.shiftcalendar.app.ui.shifttypes.ShiftTypesScreen
import dev.shiftcalendar.app.ui.shifttypes.ShiftTypesViewModel
import kotlinx.coroutines.launch

object Routes {
    const val CALENDAR = "calendar"
    const val SCHEDULES = "schedules"
    const val EDITOR = "editor/{id}"
    const val ADHOC = "adhoc"
    const val COLORS = "colors"
    const val SHIFT_TYPES = "shifttypes"
    const val OPTIONS = "options"
    const val HELP = "help"
    const val DEVELOPER = "developer"

    fun editor(id: Int) = "editor/$id"
}

@Composable
fun ShiftCalendarApp(repository: ShiftRepository) {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val app by repository.state.collectAsState()
    val view = LocalView.current
    val clickSound = app.settings.sound

Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(clickSound) {
                if (!clickSound) return@pointerInput
                val slop = ViewConfiguration.get(view.context).scaledTouchSlop
                awaitEachGesture {
                    val down = awaitFirstDown(
                        requireUnconsumed = false,
                        pass = PointerEventPass.Initial,
                    )
                    var moved = 0f
                    while (true) {
                        val ev = awaitPointerEvent(PointerEventPass.Initial)
                        val change = ev.changes.first()
                        if (change.changedToUp()) break
                        moved = maxOf(moved, (change.position - down.position).getDistance())
                        if (moved > slop) {
                            while (true) {
                                val e = awaitPointerEvent(PointerEventPass.Initial)
                                if (e.changes.first().changedToUp()) break
                            }
                            break
                        }
                    }
                    if (moved <= slop) view.playSoundEffect(SoundEffectConstants.CLICK)
                }
            },
    ) {
        ModalNavigationDrawer(
            drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Text(
                    text = stringResource(R.string.app_name),
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.titleLarge,
                )
                DrawerItem(
                    label = stringResource(R.string.cal_title),
                    icon = { Icon(Icons.Filled.DateRange, contentDescription = null) },
                    onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate(Routes.CALENDAR) {
                            popUpTo(Routes.CALENDAR) { inclusive = false }
                            launchSingleTop = true
                        }
                    },
                )
                DrawerItem(
                    label = stringResource(R.string.menu_schedules),
                    icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
                    onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate(Routes.SCHEDULES)
                    },
                )
                DrawerItem(
                    label = stringResource(R.string.menu_adhoc),
                    icon = { Icon(Icons.Filled.EventBusy, contentDescription = null) },
                    onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate(Routes.ADHOC)
                    },
                )
                DrawerItem(
                    label = stringResource(R.string.menu_colors),
                    icon = { Icon(Icons.Filled.ColorLens, contentDescription = null) },
                    onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate(Routes.COLORS)
                    },
                )
                DrawerItem(
                    label = stringResource(R.string.menu_shifts),
                    icon = { Icon(Icons.Filled.Palette, contentDescription = null) },
                    onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate(Routes.SHIFT_TYPES)
                    },
                )
                DrawerItem(
                    label = stringResource(R.string.menu_options),
                    icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                    onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate(Routes.OPTIONS)
                    },
                )
                DrawerItem(
                    label = stringResource(R.string.menu_help),
                    icon = { Icon(Icons.AutoMirrored.Filled.HelpOutline, contentDescription = null) },
                    onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate(Routes.HELP)
                    },
                )
                DrawerItem(
                    label = stringResource(R.string.menu_about),
                    icon = { Icon(Icons.Filled.Info, contentDescription = null) },
                    onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate(Routes.DEVELOPER)
                    },
                )
            }
        },
    ) {
        NavHost(
            navController = navController,
            startDestination = Routes.CALENDAR,
        ) {
            composable(Routes.CALENDAR) {
                val vm: CalendarViewModel = viewModel(factory = CalendarViewModel.factory(repository))
                CalendarScreen(vm = vm, onMenu = { scope.launch { drawerState.open() } })
            }
            composable(Routes.SCHEDULES) {
                SchedulesScreen(
                    repository = repository,
                    onBack = { navController.popBackStack() },
                    onEdit = { navController.navigate(Routes.editor(it)) },
                )
            }
            composable(Routes.EDITOR) { entry ->
                val id = entry.arguments?.getString("id")?.toIntOrNull() ?: 1
                ScheduleEditorScreen(
                    repository = repository,
                    scheduleId = id,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.ADHOC) {
                val vm: AdHocViewModel = viewModel(factory = AdHocViewModel.factory(repository))
                AdHocListScreen(vm = vm, onBack = { navController.popBackStack() })
            }
            composable(Routes.COLORS) {
                ColorsScreen(repository = repository, onBack = { navController.popBackStack() })
            }
            composable(Routes.SHIFT_TYPES) {
                val vm: ShiftTypesViewModel = viewModel(factory = ShiftTypesViewModel.factory(repository))
                ShiftTypesScreen(vm = vm, onBack = { navController.popBackStack() })
            }
            composable(Routes.OPTIONS) {
                SettingsScreen(repository = repository, onBack = { navController.popBackStack() })
            }
            composable(Routes.HELP) {
                HelpScreen(
                    onBack = { navController.popBackStack() },
                    onOpenDeveloper = { navController.navigate(Routes.DEVELOPER) },
                )
            }
            composable(Routes.DEVELOPER) {
                DeveloperScreen(onBack = { navController.popBackStack() })
            }
        }
        }
    }
}

@Composable
private fun DrawerItem(
    label: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
) {
    NavigationDrawerItem(
        label = { Text(label) },
        icon = icon,
        selected = false,
        onClick = onClick,
    )
}