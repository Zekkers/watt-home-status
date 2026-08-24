package com.zekkers.watthome

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zekkers.watthome.ui.StatusScreen
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
                StatusScreen(
                    state = state,
                    onRefresh = viewModel::refresh
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        StatusRefreshScheduler.enqueueNow(this)
    }
}
