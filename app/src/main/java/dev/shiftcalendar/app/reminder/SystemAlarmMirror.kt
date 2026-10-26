package dev.shiftcalendar.app.reminder

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.provider.AlarmClock
import dev.shiftcalendar.app.data.AppState
import dev.shiftcalendar.app.data.nonWorkShiftTypes
import java.time.LocalDate
import java.time.ZoneId

/**
 * Зеркалирование будильника о смене в системное приложение «Часы».
 *
 * Ставится ровно один будильник на каждый рабочий тип смены — ближайшая предстоящая смена
 * с заданным временем (одиночный будильник, без повтора по дням недели). Переустановка
 * происходит только при изменении времени/текста/даты зеркалируемой смены, чтобы «Часы»
 * не засорялись дублями (платформа не позволяет удалять чужие будущие будильники).
 */
object SystemAlarmMirror {

    private const val PREFS_NAME = "shift_calendar"
    private const val PREFS_KEY = "opt_mirror_clock_state"
    private const val HORIZON_DAYS = 62L

    /** Что было поставлено в «Часы» для типа смены. */
    private data class MirrorState(
        val hour: Int,
        val minute: Int,
        val message: String,
        val epochDay: Long,
    )

    /** Ставит одиночный будильник в системных «Часах» на [hour]:[minute] без экрана. */
    fun setAlarm(context: Context, hour: Int, minute: Int, message: String) {
        val intent = Intent(AlarmClock.ACTION_SET_ALARM)
            // NO_USER_ACTION: не выводить системные «Часы» на передний план при установке.
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION)
            .putExtra(AlarmClock.EXTRA_HOUR, hour)
            .putExtra(AlarmClock.EXTRA_MINUTES, minute)
            .putExtra(AlarmClock.EXTRA_MESSAGE, message)
            .putExtra(AlarmClock.EXTRA_SKIP_UI, true)
        context.startActivity(intent)
    }

    /**
     * Пересоздаёт зеркала для всех рабочих типов: по одному на тип — ближайшая смена.
     * SET_ALARM отправляется, только если (время, текст, дата) изменились.
     * При выключенном будильнике/зеркале новые не ставятся, сохранённое состояние очищается.
     */
    fun mirrorAll(context: Context, state: AppState) {
        val settings = state.settings
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val stored = loadState(prefs)
        val next = mutableMapOf<Int, MirrorState>()

        if (settings.reminderEnabled && settings.mirrorToSystemClock) {
            for ((type, time) in settings.reminderTimes) {
                if (type in nonWorkShiftTypes) continue
                val date = nearestShift(state, type, time) ?: continue
                // ACTION_SET_ALARM не умеет ставить будильник на конкретную дату: система
                // берёт ближайшее наступление этого времени (поэтому «на 27.10 17:00»
                // звонило сегодня в 17:00). Зеркалим только смену сегодняшнего дня —
                // на будущие дни зеркало поставит ежедневное обновление в 00:01.
                if (date != LocalDate.now()) continue
                val message = mirrorMessage(context, state, type, date)
                val prev = stored[type]
                if (prev == null ||
                    prev.hour != time.first ||
                    prev.minute != time.second ||
                    prev.message != message ||
                    prev.epochDay != date.toEpochDay()
                ) {
                    setAlarm(context, time.first, time.second, message)
                }
                next[type] = MirrorState(time.first, time.second, message, date.toEpochDay())
            }
        }

        saveState(prefs, next)
    }

    /** Ближайшая дата смены [type] от сегодня; сегодня — только если время ещё не наступило. */
    private fun nearestShift(state: AppState, type: Int, time: Pair<Int, Int>): LocalDate? {
        val today = LocalDate.now()
        for (d in 0 until HORIZON_DAYS) {
            val date = today.plusDays(d)
            var found = false
            for (slot in 1..4) {
                if (state.dayType(slot, date) == type) { found = true; break }
            }
            if (!found) continue
            if (d == 0L) {
                val at = date.atTime(time.first, time.second)
                    .atZone(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli()
                if (at <= System.currentTimeMillis()) continue
            }
            return date
        }
        return null
    }

    /** Текст системного будильника: заголовок и текст уведомления, напр. «Дневная — График 2/2». */
    private fun mirrorMessage(context: Context, state: AppState, type: Int, date: LocalDate): String {
        val (title, text) = ReminderScheduler.resolveNotification(context, state, type, date)
        return "$title — $text"
    }

    /**
     * Убирает отзвонивший зеркальный будильник из списка «Часов». Для звонящего
     * (FIRED) одиночного будильника DISMISS_ALARM приводит к его полному удалению
     * (deleteAfterUse=true), поэтому запись не остаётся в списке и не мусорится.
     * Вызывается в момент срабатывания напоминания, сразу после — mirrorAll поставит
     * следующий будильник на следующую смену.
     *
     * ⚠ На MIUI DISMISS_ALARM гасит не только звонящий будильник, но и следующую
     * запись, поэтому шлём его только когда зеркало этого типа стоит ровно на
     * сработавшую дату (значит, звонит именно оно). Иначе запись в «Часах» тихо
     * выключалась бы, а mirrorAll из-за дедупликации её бы не восстановил.
     */
    fun dismissFired(context: Context, type: Int, date: LocalDate) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val stored = loadState(prefs)[type] ?: return
        if (stored.epochDay != date.toEpochDay()) return
        try {
            context.startActivity(
                Intent(AlarmClock.ACTION_DISMISS_ALARM)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION)
            )
        } catch (e: Exception) {
            // «Часы» недоступны или активного будильника нет — не критично.
        }
    }

    // ---------------- сохранение поставленных зеркал ----------------

    private fun loadState(prefs: SharedPreferences): Map<Int, MirrorState> {
        val raw = prefs.getString(PREFS_KEY, null) ?: return emptyMap()
        val result = mutableMapOf<Int, MirrorState>()
        for (record in raw.split("\u001F")) {
            val parts = splitEscaped(record, '|')
            if (parts.size != 5) continue
            val type = parts[0].toIntOrNull() ?: continue
            val hour = parts[1].toIntOrNull() ?: continue
            val minute = parts[2].toIntOrNull() ?: continue
            val epoch = parts[3].toLongOrNull() ?: continue
            result[type] = MirrorState(hour, minute, unescape(parts[4]), epoch)
        }
        return result
    }

    private fun saveState(prefs: SharedPreferences, next: Map<Int, MirrorState>) {
        val e = prefs.edit()
        if (next.isEmpty()) {
            e.remove(PREFS_KEY).apply()
            return
        }
        val raw = next.entries.sortedBy { it.key }.joinToString("\u001F") {
            "${it.key}|${it.value.hour}|${it.value.minute}|${it.value.epochDay}|${escape(it.value.message)}"
        }
        e.putString(PREFS_KEY, raw).apply()
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
}