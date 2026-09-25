package ir.tivan.controller.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import ir.tivan.controller.MainActivity
import ir.tivan.controller.R
import ir.tivan.controller.TivanApp
import ir.tivan.controller.data.LogDirection
import ir.tivan.controller.sms.SmsSender
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Home-screen widget in the same tile language as the app: one row per
 * output (up to four), yellow when on and grey when off, each with its own
 * "روشن" / "خاموش" buttons that send the SMS directly — no need to open the
 * app. Tapping the title opens the app.
 */
class TivanWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        refresh(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action != ACTION_SET) return
        val index = intent.getIntExtra(EXTRA_INDEX, -1)
        val on = intent.getBooleanExtra(EXTRA_ON, true)
        if (index < 0) return

        val pending = goAsync()
        val app = context.applicationContext as TivanApp
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val device = currentDevice(app) ?: return@launch
                val command = "${index + 1}${if (on) 1 else 0}"
                if (SmsSender.send(app, device.phoneNumber, command) != SmsSender.Result.Failed) {
                    app.repository.addLog(device.id, LogDirection.OUT, "$command (ویجت)")
                }
                render(app, sentNote = "دستور «${device.outputName(index)} ${if (on) "روشن" else "خاموش"}» ارسال شد")
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        private const val ACTION_SET = "ir.tivan.controller.WIDGET_SET"
        private const val EXTRA_INDEX = "index"
        private const val EXTRA_ON = "on"

        private val ROW_IDS = intArrayOf(R.id.row0, R.id.row1, R.id.row2, R.id.row3)
        private val NAME_IDS = intArrayOf(R.id.name0, R.id.name1, R.id.name2, R.id.name3)
        private val ON_IDS = intArrayOf(R.id.on0, R.id.on1, R.id.on2, R.id.on3)
        private val OFF_IDS = intArrayOf(R.id.off0, R.id.off1, R.id.off2, R.id.off3)

        /** Redraws every placed widget from the latest saved device state. */
        fun refresh(context: Context) {
            val app = context.applicationContext as TivanApp
            CoroutineScope(Dispatchers.IO).launch { render(app, sentNote = null) }
        }

        private suspend fun currentDevice(app: TivanApp) =
            app.allDevicesSnapshot().let { list -> list.firstOrNull { it.isSelected } ?: list.firstOrNull() }

        private suspend fun render(app: TivanApp, sentNote: String?) {
            val manager = AppWidgetManager.getInstance(app)
            val ids = manager.getAppWidgetIds(ComponentName(app, TivanWidget::class.java))
            if (ids.isEmpty()) return

            val views = RemoteViews(app.packageName, R.layout.widget_tivan)
            val openApp = PendingIntent.getActivity(
                app, 0, Intent(app, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_title, openApp)

            val device = currentDevice(app)
            if (device == null) {
                views.setTextViewText(R.id.widget_title, "تیوان")
                views.setTextViewText(R.id.widget_sub, "برای شروع، برنامه را باز کنید و دستگاه اضافه کنید")
                ROW_IDS.forEach { views.setViewVisibility(it, View.GONE) }
            } else {
                val states = app.repository.statusOnce(device.id)?.outputStates.orEmpty()
                views.setTextViewText(R.id.widget_title, "${device.icon} ${device.name}")
                views.setTextViewText(R.id.widget_sub, sentNote ?: "زرد یعنی روشن · لمس عنوان برای باز کردن برنامه")
                for (i in ROW_IDS.indices) {
                    if (i >= device.channelCount) {
                        views.setViewVisibility(ROW_IDS[i], View.GONE)
                        continue
                    }
                    views.setViewVisibility(ROW_IDS[i], View.VISIBLE)
                    val on = states.getOrNull(i) == '1'
                    views.setInt(
                        ROW_IDS[i], "setBackgroundResource",
                        if (on) R.drawable.widget_tile_on else R.drawable.widget_tile_off
                    )
                    views.setTextViewText(NAME_IDS[i], "${device.outputIcon(i)} ${device.outputName(i)}")
                    views.setOnClickPendingIntent(ON_IDS[i], action(app, i, true))
                    views.setOnClickPendingIntent(OFF_IDS[i], action(app, i, false))
                }
            }
            manager.updateAppWidget(ids, views)
        }

        private fun action(context: Context, index: Int, on: Boolean): PendingIntent {
            val intent = Intent(context, TivanWidget::class.java).apply {
                action = ACTION_SET
                putExtra(EXTRA_INDEX, index)
                putExtra(EXTRA_ON, on)
            }
            return PendingIntent.getBroadcast(
                context, index * 2 + if (on) 1 else 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }
}
