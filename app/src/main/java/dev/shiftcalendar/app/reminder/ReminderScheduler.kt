package dev.shiftcalendar.app.reminder

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
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

/** Планировщик будильников о смене: свой будильник на каждый (день, тип смены). */
object ReminderScheduler {

    const val ACTION_REMINDER = "dev.shiftcalendar.app.ACTION_REMINDER"
    const val EXTRA_DATE = "date"
    const val EXTRA_TYPE = "type"

    private const val CHANNEL_ID = "shift_reminder"
    private const val HORIZON_DAYS = 62L
    private const val TYPES_COUNT = 9

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
                    alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
                } else {
                    alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
                }
            }
        }
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
        val typeName = state.shiftTypes.getOrNull(type)?.name ?: ""
        val schedules = mutableListOf<String>()
        for (slot in 1..4) {
            if (state.dayType(slot, date) != type) continue
            val schedule = state.schedules.getOrNull(slot - 1)
            schedules += schedule?.title?.takeIf { it.isNotBlank() }
                ?: context.getString(R.string.slot_name, slot)
        }
        val text = schedules.joinToString(", ").let { list ->
            if (list.isBlank()) context.getString(R.string.reminder_title)
            else context.getString(R.string.reminder_text, typeName, list)
        }
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
            .setContentTitle(titleForType(context, type))
            .setContentText(text)
            .setStyle(android.app.Notification.BigTextStyle().bigText(text))
            .setAutoCancel(true)
            .setCategory(android.app.Notification.CATEGORY_ALARM)
            .setContentIntent(openPending)
            .setColor(accent)
            .build()
        context.getSystemService(NotificationManager::class.java)
            .notify(notificationId, notification)
    }
}