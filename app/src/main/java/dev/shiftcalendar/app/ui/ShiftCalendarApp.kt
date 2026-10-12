package dev.shiftcalendar.app.ui

import android.view.SoundEffectConstants
import android.view.ViewConfiguration
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
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
import dev.shiftcalendar.app.ui.more.MoreScreen
import dev.shiftcalendar.app.ui.schedules.ScheduleEditorScreen
import dev.shiftcalendar.app.ui.schedules.SchedulesScreen
import dev.shiftcalendar.app.ui.settings.SettingsScreen
import dev.shiftcalendar.app.ui.shifttypes.ShiftTypesScreen
import dev.shiftcalendar.app.ui.shifttypes.ShiftTypesViewModel

object Routes {
    const val CALENDAR = "calendar"
    const val SCHEDULES = "schedules"
    const val EDITOR = "editor/{id}"
    const val ADHOC = "adhoc"
    const val COLORS = "colors"
    const val SHIFT_TYPES = "shifttypes"
    const val OPTIONS = "options"
    const val MORE = "more"
    const val HELP = "help"
    const val DEVELOPER = "developer"

    fun editor(id: Int) = "editor/$id"
}

private data class NavItem(val route: String, val label: Int, val icon: ImageVector)

private val bottomItems = listOf(
    NavItem(Routes.CALENDAR, R.string.nav_calendar, Icons.Filled.CalendarMonth),
    NavItem(Routes.SCHEDULES, R.string.nav_schedules, Icons.AutoMirrored.Filled.List),
    NavItem(Routes.COLORS, R.string.menu_colors, Icons.Filled.Palette),
    NavItem(Routes.OPTIONS, R.string.menu_options, Icons.Filled.Settings),
    NavItem(Routes.MORE, R.string.nav_more, Icons.Filled.MoreHoriz),
)

private val topLevelRoutes = bottomItems.map { it.route }.toSet()

@Composable
fun ShiftCalendarApp(repository: ShiftRepository) {
    val navController = rememberNavController()
    val app by repository.state.collectAsState()
    val view = LocalView.current
    val clickSound = app.settings.sound
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = currentRoute in topLevelRoutes

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
        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                if (showBottomBar) {
                    AppBottomBar(
                        currentRoute = currentRoute,
                        onSelect = { route ->
                            navController.navigate(route) {
                                popUpTo(Routes.CALENDAR) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                    )
                }
            },
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = Routes.CALENDAR,
                modifier = Modifier.padding(bottom = padding.calculateBottomPadding()),
            ) {
                composable(Routes.CALENDAR) {
                    val vm: CalendarViewModel = viewModel(factory = CalendarViewModel.factory(repository))
                    CalendarScreen(vm = vm)
                }
                composable(Routes.SCHEDULES) {
                    SchedulesScreen(
                        repository = repository,
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
                    ColorsScreen(repository = repository)
                }
                composable(Routes.SHIFT_TYPES) {
                    val vm: ShiftTypesViewModel = viewModel(factory = ShiftTypesViewModel.factory(repository))
                    ShiftTypesScreen(vm = vm, onBack = { navController.popBackStack() })
                }
                composable(Routes.OPTIONS) {
                    SettingsScreen(repository = repository)
                }
                composable(Routes.MORE) {
                    MoreScreen(
                        onOpenAdHoc = { navController.navigate(Routes.ADHOC) },
                        onOpenShiftTypes = { navController.navigate(Routes.SHIFT_TYPES) },
                        onOpenHelp = { navController.navigate(Routes.HELP) },
                        onOpenAbout = { navController.navigate(Routes.DEVELOPER) },
                    )
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
private fun AppBottomBar(currentRoute: String?, onSelect: (String) -> Unit) {
    NavigationBar {
        bottomItems.forEach { item ->
            NavigationBarItem(
                selected = currentRoute == item.route,
                onClick = { onSelect(item.route) },
                icon = { Icon(item.icon, contentDescription = null) },
                label = {
                    Text(
                        text = stringResource(item.label),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        letterSpacing = 0.sp,
                        maxLines = 1,
                        softWrap = false,
                    )
                },
            )
        }
    }
}
