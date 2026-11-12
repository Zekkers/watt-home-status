package com.zekkers.watthome

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Locks the call sites: widgets stay on background work, and only the
 * foreground activity asks for an app-open history fetch.
 */
class FairUseWiringTest {
    @Test
    fun noSubFifteenMinuteFollowUpRemains() {
        val banned = listOf(
            "needsFastPoll",
            "enqueueFollowUp",
            "scheduleAfterSuccess",
            "FOLLOW_UP_SECONDS",
            "KEY_INCLUDE_SERIES",
            "setInitialDelay"
        )
        val hits = mainSources().flatMap { file ->
            val text = file.readText()
            banned.filter { text.contains(it) }.map { "${file.name}:$it" }
        }.toList()
        assertTrue(hits.joinToString(), hits.isEmpty())
    }

    @Test
    fun openingTheActivityFetchesAsAppOpenNotAsWidgetWork() {
        val main = readMain("MainActivity.kt")
        assertTrue(main.contains("onAppForeground"))
        assertTrue(main.contains("enqueuePeriodic"))
        assertFalse(main.contains("enqueueNow"))

        val viewModel = readMain("StatusViewModel.kt")
        assertTrue(viewModel.contains("Trigger.APP_OPEN"))
        assertTrue(viewModel.contains("Trigger.PULL_TO_REFRESH"))
        assertTrue(viewModel.contains("forceHistory = true"))
        assertFalse(viewModel.contains("includeSeries"))
        assertFalse(viewModel.contains("enqueueNow"))
    }

    @Test
    fun widgetsAndProcessStartStayOnBackgroundWork() {
        val app = readMain("WattHomeApp.kt")
        assertTrue(app.contains("enqueueNow"))
        assertTrue(app.contains("enqueuePeriodic"))
        assertFalse(app.contains("APP_OPEN"))

        val receiver = readMain("widget/StatusWidgetReceiver.kt")
        assertTrue(receiver.contains("enqueueNow"))
        assertTrue(receiver.contains("enqueuePeriodic"))
        assertFalse(receiver.contains("APP_OPEN"))
        assertFalse(receiver.contains("onAppForeground"))

        val scheduler = readMain("worker/StatusRefreshScheduler.kt")
        assertTrue(scheduler.contains("Trigger.BACKGROUND"))
        assertTrue(scheduler.contains("watt-home-status-followup"))
        assertFalse(scheduler.contains("APP_OPEN"))

        val worker = readMain("worker/StatusRefreshWorker.kt")
        assertTrue(worker.contains("Trigger.fromWire"))
        assertFalse(worker.contains("include_series"))
        assertFalse(worker.contains("includeSeries"))
    }

    @Test
    fun versionIsAheadOfThe1222Sideload() {
        val gradle = File("build.gradle.kts").readText()
        assertTrue(gradle.contains("versionCode = 35"))
        assertTrue(gradle.contains("versionName = \"1.2.23\""))
    }

    private fun mainSources(): Sequence<File> {
        val root = File("src/main/java/com/zekkers/watthome")
        assertTrue(root.isDirectory)
        return root.walk().filter { it.isFile && it.extension == "kt" }
    }

    private fun readMain(relative: String): String {
        val path = File("src/main/java/com/zekkers/watthome", relative)
        assertTrue("missing $relative at ${path.absolutePath}", path.isFile)
        return path.readText()
    }
}
