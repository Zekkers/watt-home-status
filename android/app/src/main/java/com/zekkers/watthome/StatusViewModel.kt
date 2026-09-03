package com.zekkers.watthome

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zekkers.watthome.data.RefreshErrors
import com.zekkers.watthome.data.StatusRepository
import com.zekkers.watthome.data.TokenRejectedException
import com.zekkers.watthome.widget.WidgetUpdater
import com.zekkers.watthome.worker.StatusRefreshScheduler
import kotlinx.coroutines.CancellationException
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
        !repository.snapshotHasToken() && !repository.seenTokenScreen()
    )
    val showTokenScreen = _showTokenScreen.asStateFlow()

    private val _openedFromSettings = MutableStateFlow(false)
    val openedFromSettings = _openedFromSettings.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            repository.loadCached()
            if (!repository.hasToken() && !repository.seenTokenScreen()) {
                _showTokenScreen.value = true
            }
            refreshInternal()
        }
    }

    fun refresh() {
        viewModelScope.launch(Dispatchers.IO) {
            refreshInternal()
        }
    }

    private suspend fun refreshInternal() {
        val previousSoc = repository.uiState.value.status?.socPercent
        try {
            val status = repository.refresh(includeSeries = true)
            WidgetUpdater.updateAll(getApplication())
            StatusRefreshScheduler.scheduleAfterSuccess(
                context = getApplication(),
                status = status,
                previousSocPercent = previousSoc,
                liveOk = repository.uiState.value.liveOk
            )
        } catch (error: CancellationException) {
            WidgetUpdater.updateAll(getApplication())
            if (RefreshErrors.isStructuredCancellation(error)) throw error
        } catch (_: Exception) {
            WidgetUpdater.updateAll(getApplication())
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
                val status = runCatching { repository.refresh(includeSeries = true) }.getOrNull()
                WidgetUpdater.updateAll(getApplication())
                status?.let {
                    StatusRefreshScheduler.scheduleAfterSuccess(
                        context = getApplication(),
                        status = it,
                        previousSocPercent = previousSoc,
                        liveOk = repository.uiState.value.liveOk
                    )
                }
                withContext(Dispatchers.Main) {
                    _openedFromSettings.value = false
                    _showTokenScreen.value = false
                }
            } catch (error: CancellationException) {
                throw error
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
            } catch (error: CancellationException) {
                throw error
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

    private fun safeMessage(error: Throwable): String? {
        if (RefreshErrors.looksLikeCancellation(error)) return null
        if (error is TokenRejectedException) return "token rejected, re-enter"
        val detail = error.message?.takeIf { it.isNotBlank() && !it.contains("Bearer", ignoreCase = true) }
        return detail ?: "Couldn't use that token"
    }
}
