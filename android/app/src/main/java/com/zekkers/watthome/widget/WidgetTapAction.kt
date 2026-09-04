package com.zekkers.watthome.widget

import android.content.Context
import android.content.Intent
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import com.zekkers.watthome.MainActivity

/**
 * Fallback only. Widget click targets use Glance `actionStartActivity`, which
 * never enters this callback. If this does run, `startActivity` is the first
 * and only step — no refresh, series download, or widget rebuild.
 *
 * Refresh after first frame is Application / MainActivity / WorkManager.
 */
class WidgetTapAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        WidgetTapPolicy.openMainActivity(context)
    }
}

object WidgetTapPolicy {
    /**
     * Ordered tap work. Refresh is intentionally absent so a unit test can
     * assert the activity opens before any I/O.
     */
    enum class Step { StartActivity }

    val steps: List<Step> = listOf(Step.StartActivity)

    fun launchIntent(context: Context): Intent {
        check(steps.first() == Step.StartActivity) {
            "Widget tap must start MainActivity before any refresh"
        }
        return Intent(context.applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_SINGLE_TOP or
                Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
    }

    fun openMainActivity(context: Context) {
        context.applicationContext.startActivity(launchIntent(context))
    }
}
