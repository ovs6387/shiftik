package dev.shiftcalendar.app.reminder

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import dev.shiftcalendar.app.MainActivity
import dev.shiftcalendar.app.R
import dev.shiftcalendar.app.data.AppState
import dev.shiftcalendar.app.data.Settings
import dev.shiftcalendar.app.data.nonWorkShiftTypes
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Планировщик будильников о смене: свой будильник на каждый (день, тип смены). */
object ReminderScheduler {

    const val ACTION_REMINDER = "dev.shiftcalendar.app.ACTION_REMINDER"
    const val ACTION_MIRROR_REFRESH = "dev.shiftcalendar.app.ACTION_MIRROR_REFRESH"
    const val EXTRA_DATE = "date"
    const val EXTRA_TYPE = "type"

    private const val MIRROR_REFRESH_REQUEST = 900_000

    private const val CHANNEL_ID = "shift_reminder"
    private const val HORIZON_DAYS = 62L
    private const val TYPES_COUNT = 9
    private const val PREFS_NAME = "shift_calendar"
    private const val POSTED_KEY = "opt_reminder_posted"

    /** Проверяет, может ли приложение ставить точные будильники (Android 12+). */
    fun canScheduleExactAlarms(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
        } else {
            true
        }
    }

    /** Intent для перехода в системные настройки точных будильников. */
    fun exactAlarmSettingsIntent(): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
        } else {
            Intent()
        }
    }

    fun scheduleAll(context: Context, state: AppState) {
        val settings = state.settings
        val today = LocalDate.now()
        val alarm = context.getSystemService(AlarmManager::class.java)

        for (d in 0 until HORIZON_DAYS) {
            val date = today.plusDays(d)
            // Отменяем возможные старые будильники на этот день (по всем типам).
            for (t in 0 until TYPES_COUNT) {
                alarm.cancel(pendingIntent(context, date, today, t))
            }
            if (!settings.reminderEnabled) continue
            for ((type, time) in timesForDate(state, settings, date)) {
                val triggerAt = date.atTime(time.first, time.second)
                    .atZone(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli()
                if (triggerAt <= System.currentTimeMillis()) continue
                val pi = pendingIntent(context, date, today, type)
                if (alarm.canScheduleExactAlarms()) {
                    // setAlarmClock, а не setExactAndAllowWhileIdle: такие алармы система
                    // относит к будильникам — они не откладываются в Doze и MIUI-«заморозке»
                    // (проверено: setExactAndAllowWhileIdle после нескольких часов простоя
                    // приходил на ~4 минуты позже, а будильники «Часов» — секунда в секунду).
                    // Побочный плюс: ближайший будильник виден на экране блокировки.
                    alarm.setAlarmClock(AlarmManager.AlarmClockInfo(triggerAt, showPendingIntent(context)), pi)
                } else {
                    alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
                }
            }
        }

        // Будильник выключен — снимаем из шторки ранее показанные уведомления о смене.
        if (!settings.reminderEnabled) {
            cancelShownReminders(context)
        }

        // Зеркало в системные «Часы»: ближайшая смена по типу, переустановка при изменении.
        SystemAlarmMirror.mirrorAll(context, state)

        // Досборка зеркала, когда экран разблокирован (см. MirrorJobService).
        if (settings.reminderEnabled && settings.mirrorToSystemClock) {
            MirrorJobService.schedule(context)
        }

        // Ежедневное обновление зеркала в «Часах» в 00:01: ACTION_SET_ALARM не умеет
        // ставить будильник на дату, поэтому зеркало ставится в день смены, а не заранее.
        if (settings.reminderEnabled && settings.mirrorToSystemClock) {
            scheduleMirrorRefresh(context, alarm)
        } else {
            alarm.cancel(mirrorRefreshPendingIntent(context))
        }
    }

    /** Точный аларм на 00:01 (сегодня или завтра), по нему зеркало обновляется на новый день. */
    private fun scheduleMirrorRefresh(context: Context, alarm: AlarmManager) {
        val pi = mirrorRefreshPendingIntent(context)
        alarm.cancel(pi)
        val now = java.time.LocalDateTime.now()
        var at = now.toLocalDate().atTime(0, 1)
        if (!at.isAfter(now)) at = at.plusDays(1)
        val triggerAt = at.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        if (alarm.canScheduleExactAlarms()) {
            alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        } else {
            alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        }
    }

    private fun mirrorRefreshPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).setAction(ACTION_MIRROR_REFRESH)
        return PendingIntent.getBroadcast(
            context,
            MIRROR_REFRESH_REQUEST,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    /** (тип, время) для дня: типы дня по слотам, у которых задано время будильника. */
    private fun timesForDate(state: AppState, settings: Settings, date: LocalDate): Map<Int, Pair<Int, Int>> {
        val result = mutableMapOf<Int, Pair<Int, Int>>()
        for (slot in 1..4) {
            val type = state.dayType(slot, date) ?: continue
            if (type in nonWorkShiftTypes) continue
            val time = settings.reminderTimes[type] ?: continue
            result[type] = time
        }
        return result
    }

    /** Заголовок уведомления по типу смены: дневная/ночная — свои, остальные — общий. */
    private fun titleForType(context: Context, type: Int): String = when (type) {
        0 -> context.getString(R.string.reminder_title_day)
        1 -> context.getString(R.string.reminder_title_night)
        else -> context.getString(R.string.reminder_title)
    }

    /**
     * Заголовок и текст уведомления для (тип, дата): из пользовательского шаблона
     * (с подстановками), а при пустом поле — значение по умолчанию (поведение 1.0).
     */
    fun resolveNotification(context: Context, state: AppState, type: Int, date: LocalDate): Pair<String, String> {
        val typeName = state.shiftTypes.getOrNull(type)?.name.orEmpty()
        val scheduleList = mutableListOf<String>()
        for (slot in 1..4) {
            if (state.dayType(slot, date) != type) continue
            val schedule = state.schedules.getOrNull(slot - 1)
            scheduleList += schedule?.title?.takeIf { it.isNotBlank() }
                ?: context.getString(R.string.slot_name, slot)
        }
        val schedules = scheduleList.joinToString(", ")
        val time = state.settings.reminderTimes[type]
            ?.let { String.format("%02d:%02d", it.first, it.second) }.orEmpty()
        val dateStr = date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))

        fun apply(s: String): String = s
            .replace("{тип}", typeName).replace("{type}", typeName)
            .replace("{расписание}", schedules).replace("{schedule}", schedules)
            .replace("{время}", time).replace("{time}", time)
            .replace("{дата}", dateStr).replace("{date}", dateStr)

        val defaultTitle = titleForType(context, type)
        val defaultText =
            if (schedules.isBlank()) defaultTitle
            else context.getString(R.string.reminder_text, typeName, schedules)

        val template = state.settings.reminderTemplates[type]
        val title = template?.title?.takeIf { it.isNotBlank() }?.let { apply(it) } ?: defaultTitle
        val text = template?.text?.takeIf { it.isNotBlank() }?.let { apply(it) } ?: defaultText
        return title to text
    }

    /** Intent «показать будильник»: открывается при нажатии на будильник на экране блокировки. */
    private fun showPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun pendingIntent(context: Context, date: LocalDate, today: LocalDate, type: Int): PendingIntent {
        val dayIndex = (date.toEpochDay() - today.toEpochDay()).toInt()
        val requestCode = dayIndex * TYPES_COUNT + type
        val intent = Intent(context, ReminderReceiver::class.java)
            .setAction(ACTION_REMINDER)
            .putExtra(EXTRA_DATE, date.toEpochDay())
            .putExtra(EXTRA_TYPE, type)
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    /** Создание/обновление канала уведомлений (вызывается до показа уведомления). */
    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.reminder_channel),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.reminder_channel_desc)
            setSound(
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    // «Будильник обязан быть слышимым»: ОС учитывает флаг при
                    // маршрутизации — не сбрасывает на тихий/фоновый вывод, а
                    // ведёт через слышимый (динамики телефона), даже при BT-наушниках.
                    .setFlags(AudioAttributes.FLAG_AUDIBILITY_ENFORCED)
                    .build(),
            )
            enableVibration(true)
            setBypassDnd(true)
            setShowBadge(true)
        }
        nm.createNotificationChannel(channel)
    }

    /** Уведомление «начинается смена типа [type]» для дня [dateEpochDay]. */
    fun showReminder(context: Context, dateEpochDay: Long, type: Int) {
        ensureChannel(context)
        val date = LocalDate.ofEpochDay(dateEpochDay)
        val state = dev.shiftcalendar.app.data.ShiftRepository(context).state.value
        val (title, text) = resolveNotification(context, state, type, date)
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openPending = PendingIntent.getActivity(
            context,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notificationId = ((date.toEpochDay() % 200_000L).toInt()) * TYPES_COUNT + type
        val accent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.resources.getColor(android.R.color.system_accent1_200, null)
        } else {
            0xFF6750A4.toInt()
        }
        val notification = android.app.Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_shift)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(
                android.app.Notification.BigTextStyle()
                    .setBigContentTitle(title)
                    .bigText(text),
            )
            .setAutoCancel(true)
            .setCategory(android.app.Notification.CATEGORY_ALARM)
            .setContentIntent(openPending)
            .setColor(accent)
            .build()
        context.getSystemService(NotificationManager::class.java)
            .notify(notificationId, notification)
        recordPosted(context, notificationId)
    }

    // ---------------- снятие показанных уведомлений ----------------

    /**
     * Снимает из шторки все ранее показанные уведомления о смене. Вызывается при
     * выключении будильника, чтобы «Начинается смена!» не висело после отключения.
     */
    fun cancelShownReminders(context: Context) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val ids = loadPosted(prefs)
        if (ids.isEmpty()) return
        val nm = context.getSystemService(NotificationManager::class.java)
        for (id in ids) {
            try {
                nm.cancel(id)
            } catch (e: Exception) {
                // Уведомление уже снято или недоступно — не критично.
            }
        }
        prefs.edit().remove(POSTED_KEY).apply()
    }

    private fun loadPosted(prefs: SharedPreferences): List<Int> =
        (prefs.getString(POSTED_KEY, null) ?: "")
            .split(',')
            .mapNotNull { it.toIntOrNull() }

    private fun recordPosted(context: Context, id: Int) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val ids = loadPosted(prefs).toMutableList()
        if (id in ids) return
        ids += id
        prefs.edit().putString(POSTED_KEY, ids.sorted().joinToString(",")).apply()
    }
}