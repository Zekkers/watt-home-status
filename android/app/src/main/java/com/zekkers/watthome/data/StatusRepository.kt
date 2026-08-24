package com.zekkers.watthome.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

private val Context.statusDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "watt_home_status"
)

class StatusRepository private constructor(context: Context) {
    private val appContext = context.applicationContext
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .callTimeout(30, TimeUnit.SECONDS)
        .build()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
    }

    private val _uiState = MutableStateFlow(StatusUiState(isLoading = true))
    val uiState: StateFlow<StatusUiState> = _uiState.asStateFlow()

    suspend fun loadCached() {
        val cached = readCachedJson()?.let { decode(it) }
        _uiState.value = StatusUiState(status = cached, isLoading = cached == null)
    }

    suspend fun cachedStatus(): HomeStatus? = readCachedJson()?.let { decode(it) }

    suspend fun refresh(): HomeStatus {
        val previous = _uiState.value.status
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        return try {
            val body = fetchJson()
            persist(body)
            val status = decode(body)
            _uiState.value = StatusUiState(status = status, isLoading = false, error = null)
            status
        } catch (error: Exception) {
            _uiState.value = StatusUiState(
                status = previous ?: cachedStatus(),
                isLoading = false,
                error = humanMessage(error)
            )
            throw error
        }
    }

    private suspend fun fetchJson(): String {
        val request = Request.Builder()
            .url(STATUS_URL)
            .header("User-Agent", USER_AGENT)
            .header("Accept", "application/json")
            .header("Cache-Control", "no-cache")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Status feed HTTP ${response.code}")
            }
            return response.body?.string()?.takeIf { it.isNotBlank() }
                ?: throw IOException("Status feed was empty")
        }
    }

    private fun decode(raw: String): HomeStatus = json.decodeFromString(HomeStatus.serializer(), raw)

    private suspend fun persist(raw: String) {
        appContext.statusDataStore.edit { prefs ->
            prefs[KEY_JSON] = raw
        }
    }

    private suspend fun readCachedJson(): String? {
        val prefs = appContext.statusDataStore.data.first()
        return prefs[KEY_JSON]
    }

    private fun humanMessage(error: Throwable): String {
        val detail = error.message?.takeIf { it.isNotBlank() }
        return if (detail != null) "Couldn't refresh: $detail" else "Couldn't refresh"
    }

    companion object {
        const val STATUS_URL =
            "https://raw.githubusercontent.com/Zekkers/watt-home-status/main/status.json"
        private const val USER_AGENT =
            "WattHomeStatus/1.0 (family widget; +https://github.com/Zekkers/watt-home-status)"
        private val KEY_JSON = stringPreferencesKey("status_json")

        @Volatile
        private var instance: StatusRepository? = null

        fun get(context: Context): StatusRepository {
            return instance ?: synchronized(this) {
                instance ?: StatusRepository(context).also { instance = it }
            }
        }
    }
}
