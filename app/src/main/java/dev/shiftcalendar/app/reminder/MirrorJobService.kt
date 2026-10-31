package dev.shiftcalendar.app.reminder

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import dev.shiftcalendar.app.data.ShiftRepository

/**
 * Периодическая досборка зеркала в системных «Часах».
 *
 * Зачем: `ACTION_SET_ALARM` — это запуск активности «Часов», а система разрешает его из
 * фона только при разблокированном экране. Ночное обновление в 00:01 и напоминание в
 * 06:00/17:00 попадают в заблокированный экран, поэтому там зеркало ставится не может.
 * Разблокировку через широковещательный приёмник отследить нельзя — Android не доставляет
 * `ACTION_USER_PRESENT` манифестным приёмникам («Background execution not allowed»).
 * Поэтому раз в 15 минут планировщик заданий будит приложение: если экран разблокирован
 * и зеркало устарело, оно ставится. Задание почти всегда завершается сразу — расход
 * минимальный.
 */
class MirrorJobService : JobService() {

    override fun onStartJob(params: JobParameters): Boolean {
        try {
            val state = ShiftRepository(applicationContext).state.value
            if (state.settings.reminderEnabled && state.settings.mirrorToSystemClock) {
                SystemAlarmMirror.mirrorAll(applicationContext, state)
            }
        } catch (e: Exception) {
            // Ничего не делаем: следующая попытка через 15 минут.
        }
        jobFinished(params, false)
        return false
    }

    override fun onStopJob(params: JobParameters): Boolean = false

    companion object {
        private const val JOB_ID = 7801
        private const val INTERVAL_MS = 15 * 60 * 1000L

        /** Идемпотентно планирует периодическое задание (вызывается при каждом пересчёте). */
        fun schedule(context: Context) {
            val scheduler = context.getSystemService(JobScheduler::class.java) ?: return
            for (job in scheduler.allPendingJobs) {
                if (job.id == JOB_ID) return
            }
            val component = ComponentName(context, MirrorJobService::class.java)
            val info = JobInfo.Builder(JOB_ID, component)
                .setPeriodic(INTERVAL_MS)
                .setPersisted(true)
                .build()
            try {
                scheduler.schedule(info)
            } catch (e: Exception) {
                // Задание не нужно для работы приложения — просто попыток будет меньше.
            }
        }
    }
}
