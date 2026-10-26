package dev.shiftcalendar.app.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dev.shiftcalendar.app.data.ShiftRepository
import java.time.LocalDate

/** Получает сработавший будильник и перепланирует всё после перезагрузки/обновления. */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                val state = ShiftRepository(context).state.value
                ReminderScheduler.scheduleAll(context, state)
            }

            ReminderScheduler.ACTION_MIRROR_REFRESH -> {
                // Наступил новый день: ставим зеркало на сегодняшние смены и
                // перепланируем обновление на следующие сутки.
                val state = ShiftRepository(context).state.value
                ReminderScheduler.scheduleAll(context, state)
            }

            ReminderScheduler.ACTION_REMINDER -> {
                val epoch = intent.getLongExtra(ReminderScheduler.EXTRA_DATE, -1L)
                val type = intent.getIntExtra(ReminderScheduler.EXTRA_TYPE, -1)
                if (epoch >= 0 && type in 0..8) {
                    ReminderScheduler.showReminder(context, epoch, type)
                    // Зеркальный будильник в «Часах» отзвенел — снимаем его, чтобы
                    // запись не оставалась в списке (перепланировка ниже поставит следующую).
                    SystemAlarmMirror.dismissFired(context, type, LocalDate.ofEpochDay(epoch))
                    val state = ShiftRepository(context).state.value
                    ReminderScheduler.scheduleAll(context, state)
                }
            }
        }
    }
}