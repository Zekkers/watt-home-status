package com.zekkers.watthome

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zekkers.watthome.data.StatusRepository
import com.zekkers.watthome.data.TokenRejectedException
import com.zekkers.watthome.widget.WidgetUpdater
import com.zekkers.watthome.worker.StatusRefreshScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StatusViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = StatusRepository.get(application)

    val uiState = repository.uiState.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = repository.uiState.value
    )

    private val _tokenFeedback = MutableStateFlow<String?>(null)
    val tokenFeedback = _tokenFeedback.asStateFlow()

    private val _showTokenScreen = MutableStateFlow(
        !repository.hasToken() && !repository.seenTokenScreen()
    )
    val showTokenScreen = _showTokenScreen.asStateFlow()

    private val _openedFromSettings = MutableStateFlow(false)
    val openedFromSettings = _openedFromSettings.asStateFlow()

    init {
        viewModelScope.launch {
            repository.loadCached()
        }
    }

    fun refresh() {
        viewModelScope.launch(Dispatchers.IO) {
            val previousSoc = repository.uiState.value.status?.socPercent
            val result = runCatching { repository.refresh(includeSeries = true) }
            WidgetUpdater.updateAll(getApplication())
            result.getOrNull()?.let { status ->
                StatusRefreshScheduler.scheduleAfterSuccess(
                    context = getApplication(),
                    status = status,
                    previousSocPercent = previousSoc,
                    liveOk = repository.uiState.value.liveOk
                )
            }
        }
    }

    fun openTokenScreen() {
        _tokenFeedback.value = null
        _openedFromSettings.value = true
        _showTokenScreen.value = true
    }

    fun closeTokenScreen() {
        repository.markTokenScreenSeen()
        _openedFromSettings.value = false
        _showTokenScreen.value = false
    }

    fun saveToken(raw: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                repository.saveToken(raw)
                _tokenFeedback.value = "Token saved"
                val previousSoc = repository.uiState.value.status?.socPercent
                val refreshed = runCatching { repository.refresh(includeSeries = true) }
                WidgetUpdater.updateAll(getApplication())
                refreshed.getOrNull()?.let { status ->
                    StatusRefreshScheduler.scheduleAfterSuccess(
                        context = getApplication(),
                        status = status,
                        previousSocPercent = previousSoc,
                        liveOk = repository.uiState.value.liveOk
                    )
                }
                withContext(Dispatchers.Main) {
                    _openedFromSettings.value = false
                    _showTokenScreen.value = false
                }
            } catch (error: Exception) {
                _tokenFeedback.value = safeMessage(error)
            }
        }
    }

    fun testToken(raw: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _tokenFeedback.value = "Testing…"
            try {
                _tokenFeedback.value = repository.testToken(raw)
            } catch (error: Exception) {
                _tokenFeedback.value = safeMessage(error)
            }
        }
    }

    fun removeToken() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearToken()
            StatusRefreshScheduler.cancelFollowUp(getApplication())
            _tokenFeedback.value = "Token removed"
            runCatching { repository.refresh() }
            WidgetUpdater.updateAll(getApplication())
        }
    }

    private fun safeMessage(error: Throwable): String {
        if (error is TokenRejectedException) return "token rejected, re-enter"
        val detail = error.message?.takeIf { it.isNotBlank() && !it.contains("Bearer", ignoreCase = true) }
        return detail ?: "Couldn't use that token"
    }
}
