package com.zekkers.watthome

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zekkers.watthome.ui.StatusScreen
import com.zekkers.watthome.ui.TokenScreen
import com.zekkers.watthome.ui.theme.WattHomeTheme
import com.zekkers.watthome.worker.StatusRefreshScheduler

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        StatusRefreshScheduler.enqueuePeriodic(this)
        enableEdgeToEdge()
        setContent {
            WattHomeTheme {
                val viewModel: StatusViewModel = viewModel()
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                val showToken by viewModel.showTokenScreen.collectAsStateWithLifecycle()
                val tokenFeedback by viewModel.tokenFeedback.collectAsStateWithLifecycle()
                val fromSettings by viewModel.openedFromSettings.collectAsStateWithLifecycle()
                val series by viewModel.graphSeries.collectAsStateWithLifecycle()
                if (showToken) {
                    TokenScreen(
                        hasToken = state.hasToken,
                        message = tokenFeedback,
                        error = if (state.error?.contains("token rejected") == true) state.error else null,
                        canSkip = !fromSettings && !state.hasToken,
                        series = series,
                        onSeriesChange = viewModel::setGraphSeries,
                        onSave = viewModel::saveToken,
                        onTest = viewModel::testToken,
                        onRemove = viewModel::removeToken,
                        onSkip = viewModel::closeTokenScreen,
                        onBack = if (fromSettings) viewModel::closeTokenScreen else null
                    )
                } else {
                    StatusScreen(
                        state = state,
                        series = series,
                        onRefresh = viewModel::refresh,
                        onOpenSettings = viewModel::openTokenScreen
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        StatusRefreshScheduler.enqueueNow(this)
    }
}
