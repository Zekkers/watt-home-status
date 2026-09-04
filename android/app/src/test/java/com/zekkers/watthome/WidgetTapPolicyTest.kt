package com.zekkers.watthome

import com.zekkers.watthome.widget.WidgetTapPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class WidgetTapPolicyTest {
    @Test
    fun tapPolicyStartsActivityAndNeverListsRefresh() {
        assertEquals(listOf(WidgetTapPolicy.Step.StartActivity), WidgetTapPolicy.steps)
        assertFalse(WidgetTapPolicy.steps.any { it.name.contains("Refresh", ignoreCase = true) })
    }

    @Test
    fun glanceClickTargetsUseActionStartActivity() {
        val card = readMain("widget/WattGlanceWidget.kt")
        assertTrue(card.contains("actionStartActivity("))
        assertTrue(card.contains("WidgetTapPolicy.launchIntent"))
        assertFalse(card.contains("actionRunCallback"))
        assertFalse(card.contains("repository.refresh"))
    }

    @Test
    fun widgetTapActionDoesNotRefreshBeforeStartActivity() {
        val src = readMain("widget/WidgetTapAction.kt")
        val start = src.indexOf("startActivity")
        val refresh = src.indexOf("refresh(")
        val updateAll = src.indexOf("WidgetUpdater")
        assertTrue("fallback must still start the activity", start >= 0)
        assertTrue(
            "fallback must not call refresh at all",
            refresh < 0
        )
        assertTrue(
            "fallback must not rebuild widget bitmaps",
            updateAll < 0
        )
        val onAction = src.indexOf("fun onAction")
        val open = src.indexOf("WidgetTapPolicy.openMainActivity")
        assertTrue(onAction >= 0 && open > onAction)
    }

    @Test
    fun applicationOnCreateDoesNotTouchRepository() {
        val src = readMain("WattHomeApp.kt")
        assertFalse(src.contains("StatusRepository.get"))
        assertFalse(src.contains("TokenStore"))
        assertTrue(src.contains("enqueueNow"))
        assertTrue(src.contains("enqueuePeriodic"))
    }

    private fun readMain(relative: String): String {
        val path = File("src/main/java/com/zekkers/watthome", relative)
        assertTrue("missing $relative at ${path.absolutePath}", path.isFile)
        return path.readText()
    }
}
