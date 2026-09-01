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
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

private val Context.statusDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "watt_home_status"
)

class StatusRepository private constructor(context: Context) {
    private val appContext = context.applicationContext
    private val tokenStore = TokenStore.get(appContext)
    private val givEnergy = GivEnergyClient()
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .callTimeout(30, TimeUnit.SECONDS)
        .build()

    private val _uiState = MutableStateFlow(StatusUiState(isLoading = true, hasToken = tokenStore.hasToken()))
    val uiState: StateFlow<StatusUiState> = _uiState.asStateFlow()

    suspend fun loadCached() {
        val cached = readCachedJson()?.let { decode(it) }
        _uiState.value = StatusUiState(
            status = cached,
            isLoading = cached == null,
            hasToken = tokenStore.hasToken()
        )
    }

    suspend fun cachedStatus(): HomeStatus? = readCachedJson()?.let { decode(it) }

    fun hasToken(): Boolean = tokenStore.hasToken()

    fun seenTokenScreen(): Boolean {
        return appContext.getSharedPreferences(SETUP_PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_SEEN_TOKEN, false)
    }

    fun markTokenScreenSeen() {
        appContext.getSharedPreferences(SETUP_PREFS, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_SEEN_TOKEN, true)
            .apply()
    }

    fun saveToken(raw: String) {
        tokenStore.saveToken(raw)
        markTokenScreenSeen()
        _uiState.value = _uiState.value.copy(hasToken = true, tokenMessage = null, error = null)
    }

    fun clearToken() {
        tokenStore.clearToken()
        _uiState.value = _uiState.value.copy(hasToken = false, liveOk = false, tokenMessage = null)
    }

    fun testToken(raw: String): String {
        val token = TokenStore.normalize(raw) ?: throw IllegalArgumentException("Paste a GivEnergy API token first")
        givEnergy.testToken(token)
        return "Live battery OK"
    }

    suspend fun refresh(includeSeries: Boolean = true): HomeStatus {
        val previous = _uiState.value.status ?: cachedStatus()
        val hasToken = tokenStore.hasToken()
        _uiState.value = _uiState.value.copy(isLoading = true, error = null, hasToken = hasToken)
        return try {
            if (!includeSeries) {
                val token = tokenStore.readToken()
                if (previous != null && token != null) {
                    return refreshLiveLatest(previous, token)
                }
            }
            refreshFull(previous)
        } catch (error: Exception) {
            if (error is TokenRejectedException) throw error
            if (_uiState.value.error != null && _uiState.value.status != null) throw error
            _uiState.value = StatusUiState(
                status = previous ?: cachedStatus(),
                isLoading = false,
                error = humanMessage(error),
                hasToken = hasToken
            )
            throw error
        }
    }

    private suspend fun refreshFull(previous: HomeStatus?): HomeStatus {
        val publicStatus = decode(fetchPublicJson()).copy(
            solarWSeries = previous?.solarWSeries.orEmpty(),
            lastWidgetPollAt = previous?.lastWidgetPollAt
        )
        val token = tokenStore.readToken()
        return if (token == null) {
            commitStatus(publicStatus, previous, hasToken = false, liveOk = false)
        } else {
            try {
                val live = givEnergy.fetchLive(token, includeSeries = true)
                commitStatus(LiveStatus.merge(publicStatus, live), previous, hasToken = true, liveOk = true)
            } catch (rejected: TokenRejectedException) {
                commitStatus(
                    publicStatus,
                    previous,
                    hasToken = true,
                    liveOk = false,
                    error = rejected.message
                )
                throw rejected
            } catch (liveError: Exception) {
                commitStatus(
                    publicStatus,
                    previous,
                    hasToken = true,
                    liveOk = false,
                    error = humanMessage(liveError)
                )
                throw liveError
            }
        }
    }

    private suspend fun refreshLiveLatest(cached: HomeStatus, token: String): HomeStatus {
        return try {
            val live = givEnergy.fetchLive(token, includeSeries = false)
            commitStatus(LiveStatus.merge(cached, live), cached, hasToken = true, liveOk = true)
        } catch (rejected: TokenRejectedException) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = rejected.message,
                hasToken = true,
                liveOk = false,
                status = cached
            )
            throw rejected
        }
    }

    private suspend fun commitStatus(
        status: HomeStatus,
        previous: HomeStatus?,
        hasToken: Boolean,
        liveOk: Boolean,
        error: String? = null
    ): HomeStatus {
        val finished = SolarInterval.finish(status, previous)
        persist(HomeStatusJson.encode(finished))
        _uiState.value = StatusUiState(
            status = finished,
            isLoading = false,
            hasToken = hasToken,
            liveOk = liveOk,
            error = error
        )
        return finished
    }

    private suspend fun fetchPublicJson(): String {
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

    private fun decode(raw: String): HomeStatus = HomeStatusParser.parse(raw)

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
        if (error is TokenRejectedException) return error.message ?: "token rejected, re-enter"
        val detail = error.message?.takeIf { it.isNotBlank() && !looksSecret(it) }
        return if (detail != null) "Couldn't refresh: $detail" else "Couldn't refresh"
    }

    private fun looksSecret(text: String): Boolean {
        val token = tokenStore.readToken() ?: return false
        return token.isNotBlank() && text.contains(token)
    }

    companion object {
        const val STATUS_URL =
            "https://raw.githubusercontent.com/Zekkers/watt-home-status/main/status.json"
        private const val USER_AGENT =
            "WattHomeStatus/1.2.5 (family widget; +https://github.com/Zekkers/watt-home-status)"
        private val KEY_JSON = stringPreferencesKey("status_json")
        private const val SETUP_PREFS = "watt_home_setup"
        private const val KEY_SEEN_TOKEN = "seen_token_screen"

        @Volatile
        private var instance: StatusRepository? = null

        fun get(context: Context): StatusRepository {
            return instance ?: synchronized(this) {
                instance ?: StatusRepository(context).also { instance = it }
            }
        }
    }
}
