package dev.shiftcalendar.app.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dev.shiftcalendar.app.data.ShiftRepository

/** Получает сработавший будильник и перепланирует всё после перезагрузки/обновления. */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                val state = ShiftRepository(context).state.value
                ReminderScheduler.scheduleAll(context, state)
            }

            ReminderScheduler.ACTION_REMINDER -> {
                val epoch = intent.getLongExtra(ReminderScheduler.EXTRA_DATE, -1L)
                val type = intent.getIntExtra(ReminderScheduler.EXTRA_TYPE, -1)
                if (epoch >= 0 && type in 0..8) ReminderScheduler.showReminder(context, epoch, type)
            }
        }
    }
}