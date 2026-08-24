package com.zekkers.watthome

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zekkers.watthome.data.StatusRepository
import com.zekkers.watthome.widget.WidgetUpdater
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class StatusViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = StatusRepository.get(application)

    val uiState = repository.uiState.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = repository.uiState.value
    )

    init {
        viewModelScope.launch {
            repository.loadCached()
        }
    }

    fun refresh() {
        viewModelScope.launch {
            repository.refresh()
            WidgetUpdater.updateAll(getApplication())
        }
    }
}
