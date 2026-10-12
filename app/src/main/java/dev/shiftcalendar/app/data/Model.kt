package dev.shiftcalendar.app.data

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Один тип смены (до 9 штук). id 0..8. */
data class ShiftType(
    val id: Int,
    var name: String,
    var visible: Boolean = true,
)

/** Одна строка цикла: N дней подряд типа [shiftTypeId]. */
data class ScheduleRow(
    var days: Int,
    var shiftTypeId: Int,
)

/** Расписание (слот 1..4): название, дата начала и повторяющийся цикл. */
data class Schedule(
    val id: Int,
    var title: String,
    var seedDate: LocalDate,
    var pattern: MutableList<ScheduleRow>,
) {
    fun isPopulated(): Boolean = title.isNotBlank() && pattern.isNotEmpty()

    /** Общая длина цикла в днях; 0, если строк нет. */
    fun cycleLength(): Int = pattern.sumOf { it.days.coerceAtLeast(1) }

    /**
     * Расчёт типа смены для даты (чистый принцип сменного графика).
     * Если [extendPast] — цикл бесконечен в обе стороны от seedDate: до даты
     * начала график продолжается в прошлое по тому же циклу (в обратном порядке).
     * Иначе — только от seedDate в будущее.
     * Возвращает null, только если расписание пустое (или дата раньше начала при extendPast=false).
     */
    fun typeForDate(date: LocalDate, extendPast: Boolean = true): Int? {
        if (!isPopulated()) return null
        val len = cycleLength()
        if (len <= 0) return null
        val offset = ChronoUnit.DAYS.between(seedDate, date)
        if (!extendPast && offset < 0) return null
        var remaining = (offset % len).let { if (it < 0) it + len else it }
        for (row in pattern) {
            val d = row.days.coerceAtLeast(1)
            if (remaining < d) return row.shiftTypeId
            remaining -= d
        }
        return pattern.firstOrNull()?.shiftTypeId
    }
}

/** АдХок-замена: на период дат для конкретного расписания задан конкретный тип. */
data class AdHoc(
    val shift: Int,          // слот 1..4
    val startDate: LocalDate,
    val endDate: LocalDate,
    var shiftTypeId: Int,
)

/** Типы, на которые будильник не ставится (дефолтные «Выходной» и «Отпуск», id фиксированы). */
val nonWorkShiftTypes: Set<Int> = setOf(2, 3)

/** Время будильника по умолчанию: Дневная 06:00, Ночная 17:00. */
val defaultReminderTimes: Map<Int, Pair<Int, Int>> = mapOf(
    0 to (6 to 0),   // Дневная
    1 to (17 to 0),  // Ночная
)

/** Системные настройки. */
data class Settings(
    var startOfWeekMonday: Boolean = true,
    var sound: Boolean = true,
    var showShiftInitial: Boolean = false,
    /** Продолжать сменный цикл за дату начала в прошлое. */
    var extendPast: Boolean = false,
    /** Будильник (уведомление) о смене. */
    var reminderEnabled: Boolean = false,
    /** Своё время будильника для каждого типа смены: id типа -> (час, минута). */
    var reminderTimes: Map<Int, Pair<Int, Int>> = defaultReminderTimes,
    /** Шаблон текста уведомления по типу смены: id типа -> шаблон. Пустые поля = значение по умолчанию. */
    var reminderTemplates: Map<Int, ReminderTemplate> = emptyMap(),
    /** Зеркалировать будильник в системные «Часы» (AlarmClock): по одному на тип — ближайшая смена. */
    var mirrorToSystemClock: Boolean = true,
    /** Тема: 0 — системная, 1 — светлая, 2 — тёмная. */
    var themeMode: Int = 0,
    /** Оформление интерфейса: индекс AppThemeStyle (0 CLASSIC, 1 PERIWINKLE). */
    var themeStyle: Int = 0,
)

/**
 * Редактируемый шаблон уведомления будильника для одного типа смены.
 * Пустая строка поля = значение по умолчанию (поведение 1.0). Поддерживает подстановки:
 * {тип}/{type}, {расписание}/{schedule}, {время}/{time}, {дата}/{date}.
 */
data class ReminderTemplate(
    var title: String = "",
    var text: String = "",
)

/** Хранилище цветов: ключ (слот, тип) -> ARGB-целое. */
class ColorTable {
    val colors = HashMap<Pair<Int, Int>, Int>()
}

/** Запасные цвета для типов 0..8 (используются, пока пользователь не назначит свой). */
val defaultTypeColors: List<Int> = listOf(
    0xFFD4AF37.toInt(), // день — золотой
    0xFF4169E1.toInt(), // ночь — синий
    0xFF6E7B8B.toInt(), // выходной — серо-стальной
    0xFF8B008B.toInt(), // отпуск — фиолетовый
    0xFF228B22.toInt(),
    0xFFCD853F.toInt(),
    0xFF4682B4.toInt(),
    0xFFB22222.toInt(),
    0xFF556B2F.toInt(),
)

/** Полное состояние приложения, собираемое из [ShiftRepository]. */
data class AppState(
    val shiftTypes: List<ShiftType> = defaultShiftTypes(),
    val schedules: List<Schedule> = emptyList(),
    val adHocs: List<AdHoc> = emptyList(),
    val colors: ColorTable = ColorTable(),
    val settings: Settings = Settings(),
) {
    /** Тип дня для (слот, дата) с учётом АдХок и видимости типа. */
    fun dayType(slot: Int, date: LocalDate): Int? {
        val s = schedules.getOrNull(slot - 1) ?: return null
        val base = s.typeForDate(date, settings.extendPast) ?: return null
        // АдХок перекрывает расчёт.
        val adhoc = adHocs.firstOrNull {
            it.shift == slot && !date.isBefore(it.startDate) && !date.isAfter(it.endDate)
        }
        val type = adhoc?.shiftTypeId ?: base
        return shiftTypes.getOrNull(type)?.takeIf { it.visible }?.let { type }
    }
}