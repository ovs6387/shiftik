package dev.shiftcalendar.app.data

import android.content.Context
import android.content.SharedPreferences
import dev.shiftcalendar.app.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate

fun defaultShiftTypes(): List<ShiftType> = (0..8).map { i ->
    ShiftType(id = i, name = defaultNames.getOrElse(i) { "" }, visible = i <= 3)
}

private val defaultNames = listOf("Дневная", "Ночная", "Выходной", "Отпуск", "", "", "", "", "")

/**
 * Единый источник данных: SharedPreferences + StateFlow.
 * Формат хранения собственный (ключи вида "schedule_N_title" и т.п.).
 */
class ShiftRepository(context: Context) {

    private val appContext = context.applicationContext
    private val resNames = listOf(
        appContext.getString(R.string.day_shift),
        appContext.getString(R.string.night_shift),
        appContext.getString(R.string.day_off),
        appContext.getString(R.string.holiday),
    )

    private val prefs: SharedPreferences =
        appContext.getSharedPreferences("shift_calendar", Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(load())
    val state: StateFlow<AppState> = _state.asStateFlow()

    // ---------------- загрузка ----------------

    private fun load(): AppState {
        val shiftTypes = (0..8).map { i ->
            val stored = prefs.getString("type_${i}_name", null)
            ShiftType(
                id = i,
                name = stored ?: resNames.getOrElse(i) { "" },
                visible = prefs.getBoolean(
                    "type_${i}_visible",
                    i <= 3,
                ),
            )
        }
        val schedules = (1..4).map { id ->
            val title = prefs.getString("schedule_${id}_title", null) ?: ""
            val seed = LocalDate.ofEpochDay(prefs.getLong("schedule_${id}_seed", LocalDate.now().toEpochDay()))
            val rows = mutableListOf<ScheduleRow>()
            var k = 1
            while (true) {
                val days = prefs.getInt("schedule_${id}_d_$k", -1)
                val type = prefs.getInt("schedule_${id}_t_$k", -1)
                if (days < 0 || type < 0) break
                rows.add(ScheduleRow(days, type))
                k++
            }
            Schedule(id, title, seed, rows)
        }
        val adHocs = mutableListOf<AdHoc>()
        var m = 1
        while (true) {
            val raw = prefs.getString("adhoc_$m", null) ?: break
            val parts = raw.split('|')
            if (parts.size == 4) {
                val shift = parts[0].toIntOrNull() ?: 0
                val start = parts[1].toLongOrNull() ?: 0L
                val end = parts[2].toLongOrNull() ?: start
                val type = parts[3].toIntOrNull() ?: 0
                if (shift in 1..4) adHocs.add(AdHoc(shift, LocalDate.ofEpochDay(start), LocalDate.ofEpochDay(end), type))
            } else if (parts.size == 3) {
                val shift = parts[0].toIntOrNull() ?: 0
                val epoch = parts[1].toLongOrNull() ?: 0L
                val type = parts[2].toIntOrNull() ?: 0
                if (shift in 1..4) {
                    val d = LocalDate.ofEpochDay(epoch)
                    adHocs.add(AdHoc(shift, d, d, type))
                }
            }
            m++
        }
        val colors = ColorTable()
        for (slot in 1..4) {
            for (t in 0..8) {
                val c = prefs.getInt("color_${slot}_$t", -1)
                if (c != -1) colors.colors[slot to t] = c
            }
        }
        val settings = Settings(
            startOfWeekMonday = prefs.getBoolean("opt_monday", true),
            sound = prefs.getBoolean("opt_sound", true),
            showShiftInitial = prefs.getBoolean("opt_shift_info", false),
            extendPast = prefs.getBoolean("opt_cycle_past", false),
            reminderEnabled = prefs.getBoolean("opt_reminder", false),
            reminderTimes = loadReminderTimes(),
            reminderTemplates = loadReminderTemplates(),
            mirrorToSystemClock = prefs.getBoolean("opt_mirror_clock", true),
            themeMode = prefs.getInt("opt_theme", 0),
            themeStyle = loadThemeStyle(),
        )
        return AppState(
            shiftTypes = shiftTypes,
            schedules = schedules,
            adHocs = adHocs.sortedBy { it.startDate },
            colors = colors,
            settings = settings,
        )
    }

    /** Время будильников по типам из строки "0:360,1:1020". Миграция со старого единого времени. */
    private fun loadReminderTimes(): Map<Int, Pair<Int, Int>> {
        val raw = prefs.getString("opt_reminder_times", null)
        if (!raw.isNullOrBlank()) {
            val times = mutableMapOf<Int, Pair<Int, Int>>()
            for (seg in raw.split(",")) {
                val parts = seg.split(":").map { it.toIntOrNull() }
                val type = parts.getOrNull(0) ?: continue
                val minutes = parts.getOrNull(1) ?: continue
                if (type in 0..8 && minutes in 0 until 24 * 60) {
                    times[type] = (minutes / 60) to (minutes % 60)
                }
            }
            return times
        }
        // Старый формат: одно время на все смены.
        val oldHour = prefs.getInt("opt_reminder_hour", 6)
        val oldMinute = prefs.getInt("opt_reminder_minute", 0)
        if (oldHour in 0..23 && oldMinute in 0..59) {
            return defaultReminderTimes.mapValues { oldHour to oldMinute }
        }
        return defaultReminderTimes
    }

    /** Шаблоны уведомлений по типам из строки "0|title|text\u001F1|title|text" (эскейпинг \\ \p \u). */
    private fun loadReminderTemplates(): Map<Int, ReminderTemplate> {
        val raw = prefs.getString("opt_reminder_templates", null) ?: return emptyMap()
        val result = mutableMapOf<Int, ReminderTemplate>()
        for (record in raw.split('\u001F')) {
            if (record.isBlank()) continue
            val parts = splitEscaped(record, '|')
            if (parts.size < 3) continue
            val type = parts[0].toIntOrNull() ?: continue
            if (type !in 0..8) continue
            result[type] = ReminderTemplate(unescape(parts[1]), unescape(parts[2]))
        }
        return result
    }

    private fun escape(s: String): String = s
        .replace("\\", "\\\\")
        .replace("|", "\\p")
        .replace("\u001F", "\\u")

    private fun unescape(s: String): String {
        val sb = StringBuilder()
        var i = 0
        while (i < s.length) {
            val c = s[i]
            if (c == '\\' && i + 1 < s.length) {
                when (s[i + 1]) {
                    '\\' -> sb.append('\\')
                    'p' -> sb.append('|')
                    'u' -> sb.append('\u001F')
                    else -> sb.append(s[i + 1])
                }
                i += 2
            } else {
                sb.append(c); i++
            }
        }
        return sb.toString()
    }

    /** Разбиение по [delim], игнорируя экранированные (с обратным слэшем) символы. */
    private fun splitEscaped(s: String, delim: Char): List<String> {
        val out = mutableListOf<String>()
        val sb = StringBuilder()
        var i = 0
        while (i < s.length) {
            val c = s[i]
            if (c == '\\' && i + 1 < s.length) {
                sb.append(c).append(s[i + 1]); i += 2
            } else if (c == delim) {
                out.add(sb.toString()); sb.clear(); i++
            } else {
                sb.append(c); i++
            }
        }
        out.add(sb.toString())
        return out
    }

    /**
     * Оформление интерфейса. До отказа от темы SYSTEM индексы были на 1 больше
     * (0 SYSTEM, 1 CLASSIC, ...) — одноразовая миграция. После сокращения набора тем
     * до двух (CLASSIC/PERIWINKLE) значения 2 и 3 (бывшие TERRA/ALEXANDRIA) → CLASSIC.
     * По умолчанию CLASSIC.
     */
    private fun loadThemeStyle(): Int {
        val raw = prefs.getInt("opt_theme_style", -1)
        if (raw < 0) {
            prefs.edit().putBoolean("opt_theme_style_migrated", true).apply()
            return 0
        }
        if (!prefs.getBoolean("opt_theme_style_migrated", false)) {
            val migrated = (raw - 1).coerceAtLeast(0)
            prefs.edit()
                .putInt("opt_theme_style", migrated)
                .putBoolean("opt_theme_style_migrated", true)
                .apply()
            return migrated
        }
        return raw.coerceIn(0, 1)
    }

    // ---------------- сохранение ----------------

    fun updateShiftType(type: ShiftType) {
        val st = _state.value
        val next = st.copy(shiftTypes = st.shiftTypes.map { if (it.id == type.id) type else it })
        prefs.edit()
            .putString("type_${type.id}_name", type.name)
            .putBoolean("type_${type.id}_visible", type.visible)
            .apply()
        _state.value = next
    }

    fun saveSchedule(schedule: Schedule) {
        val st = _state.value
        val next = st.copy(schedules = st.schedules.map { if (it.id == schedule.id) schedule else it })
        val e = prefs.edit()
        e.putString("schedule_${schedule.id}_title", schedule.title)
        e.putLong("schedule_${schedule.id}_seed", schedule.seedDate.toEpochDay())
        var k = 1
        for (row in schedule.pattern) {
            e.putInt("schedule_${schedule.id}_d_$k", row.days)
            e.putInt("schedule_${schedule.id}_t_$k", row.shiftTypeId)
            k++
        }
        while (prefs.contains("schedule_${schedule.id}_d_$k") || prefs.contains("schedule_${schedule.id}_t_$k")) {
            e.remove("schedule_${schedule.id}_d_$k")
            e.remove("schedule_${schedule.id}_t_$k")
            k++
        }
        e.apply()
        _state.value = next
    }

    fun purgeSchedule(id: Int) {
        val st = _state.value
        val next = st.copy(
            schedules = st.schedules.map {
                if (it.id == id) Schedule(id, "", LocalDate.now(), mutableListOf()) else it
            }
        )
        val e = prefs.edit()
        e.remove("schedule_${id}_title")
        e.remove("schedule_${id}_seed")
        var k = 1
        while (prefs.contains("schedule_${id}_d_$k") || prefs.contains("schedule_${id}_t_$k")) {
            e.remove("schedule_${id}_d_$k")
            e.remove("schedule_${id}_t_$k")
            k++
        }
        e.apply()
        _state.value = next
    }

    fun addAdHoc(shift: Int, startDate: LocalDate, endDate: LocalDate, shiftTypeId: Int) {
        val st = _state.value
        val next = st.copy(adHocs = (st.adHocs + AdHoc(shift, startDate, endDate, shiftTypeId)).sortedBy { it.startDate })
        persistAdHocs(next.adHocs)
        _state.value = next
    }

    fun deleteAdHoc(adHoc: AdHoc) {
        val st = _state.value
        val next = st.copy(adHocs = st.adHocs.filterNot { it == adHoc })
        persistAdHocs(next.adHocs)
        _state.value = next
    }

    private fun persistAdHocs(list: List<AdHoc>) {
        val e = prefs.edit()
        for (i in 1..1000) e.remove("adhoc_$i")
        list.forEachIndexed { i, adHoc ->
            e.putString(
                "adhoc_${i + 1}",
                "${adHoc.shift}|${adHoc.startDate.toEpochDay()}|${adHoc.endDate.toEpochDay()}|${adHoc.shiftTypeId}"
            )
        }
        e.apply()
    }

    fun setColor(slot: Int, type: Int, argb: Int) {
        val st = _state.value
        val table = ColorTable()
        table.colors.putAll(st.colors.colors)
        table.colors[slot to type] = argb
        prefs.edit().putInt("color_${slot}_$type", argb).apply()
        _state.value = st.copy(colors = table)
    }

    /** Сброс всех пользовательских цветов к значениям по умолчанию. */
    fun resetColors() {
        val e = prefs.edit()
        for (slot in 1..4) for (t in 0..8) e.remove("color_${slot}_$t")
        e.apply()
        _state.value = _state.value.copy(colors = ColorTable())
    }

    fun updateSettings(settings: Settings) {
        val st = _state.value
        prefs.edit()
            .putBoolean("opt_monday", settings.startOfWeekMonday)
            .putBoolean("opt_sound", settings.sound)
            .putBoolean("opt_shift_info", settings.showShiftInitial)
            .putBoolean("opt_cycle_past", settings.extendPast)
            .putBoolean("opt_reminder", settings.reminderEnabled)
            .putBoolean("opt_mirror_clock", settings.mirrorToSystemClock)
            .putInt("opt_theme", settings.themeMode)
            .putInt("opt_theme_style", settings.themeStyle)
            .putString(
                "opt_reminder_times",
                settings.reminderTimes.entries
                    .sortedBy { it.key }
                    .joinToString(",") { "${it.key}:${it.value.first * 60 + it.value.second}" },
            )
            .putString(
                "opt_reminder_templates",
                settings.reminderTemplates.entries
                    .sortedBy { it.key }
                    .joinToString("\u001F") { "${it.key}|${escape(it.value.title)}|${escape(it.value.text)}" },
            )
            .apply()
        _state.value = st.copy(settings = settings)
    }

    fun resetAll() {
        prefs.edit().clear().apply()
        _state.value = load()
    }
}