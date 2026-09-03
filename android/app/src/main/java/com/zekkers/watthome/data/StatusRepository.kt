package com.zekkers.watthome.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

private val Context.statusDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "watt_home_status"
)

class StatusRepository private constructor(context: Context) {
    private val appContext = context.applicationContext
    private val snapshotPrefs = appContext.getSharedPreferences(SNAPSHOT_PREFS, Context.MODE_PRIVATE)
    private val tokenStore by lazy { TokenStore.get(appContext) }
    private val givEnergy = GivEnergyClient()
    private val client = OkHttpClient.Builder()
        .connectTimeout(RefreshPolicy.CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(RefreshPolicy.READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .callTimeout(RefreshPolicy.CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()
    private val refreshMutex = Mutex()

    @Volatile
    private var lastSuccessAtMs: Long = 0L

    private val _uiState = MutableStateFlow(hydrateFirstPaint())
    val uiState: StateFlow<StatusUiState> = _uiState.asStateFlow()

    /**
     * Last-known SOC / solar / house / updated from a compact local snapshot.
     * No network, no token store, no remote JSON parse.
     */
    private fun hydrateFirstPaint(): StatusUiState {
        val raw = snapshotPrefs.getString(KEY_SNAPSHOT, null)
        val cached = raw?.let { runCatching { FirstPaintSnapshot.decode(it) }.getOrNull() }
        return StatusUiState(
            status = cached,
            isLoading = false,
            hasToken = snapshotPrefs.getBoolean(KEY_HAS_TOKEN, false),
            liveOk = snapshotPrefs.getBoolean(KEY_LIVE_OK, false),
            showingLastKnown = false
        )
    }

    suspend fun loadCached() {
        val cached = readCachedJson()?.let { runCatching { decode(it) }.getOrNull() }
        val hasToken = currentHasToken()
        if (cached != null) {
            writeSnapshot(cached, hasToken, liveOk = _uiState.value.liveOk)
        }
        val current = _uiState.value
        _uiState.value = current.copy(
            status = cached ?: current.status,
            isLoading = current.isLoading,
            hasToken = hasToken,
            showingLastKnown = current.showingLastKnown
        )
    }

    suspend fun cachedStatus(): HomeStatus? =
        readCachedJson()?.let { runCatching { decode(it) }.getOrNull() }
            ?: _uiState.value.status

    fun snapshotHasToken(): Boolean = snapshotPrefs.getBoolean(KEY_HAS_TOKEN, false)

    fun hasToken(): Boolean = snapshotHasToken() || runCatching { tokenStore.hasToken() }.getOrDefault(false)

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
        snapshotPrefs.edit().putBoolean(KEY_HAS_TOKEN, true).apply()
        _uiState.value = _uiState.value.copy(hasToken = true, tokenMessage = null, error = null)
    }

    fun clearToken() {
        tokenStore.clearToken()
        snapshotPrefs.edit().putBoolean(KEY_HAS_TOKEN, false).putBoolean(KEY_LIVE_OK, false).apply()
        _uiState.value = _uiState.value.copy(hasToken = false, liveOk = false, tokenMessage = null)
    }

    fun testToken(raw: String): String {
        val token = TokenStore.normalize(raw) ?: throw IllegalArgumentException("Paste a live-battery API token first")
        givEnergy.testToken(token)
        return "Live battery OK"
    }

    suspend fun refresh(includeSeries: Boolean = true): HomeStatus {
        return refreshMutex.withLock {
            val now = System.currentTimeMillis()
            val current = _uiState.value
            if (
                now - lastSuccessAtMs in 1 until RefreshPolicy.COALESCE_SUCCESS_MS &&
                current.status != null &&
                !current.showingLastKnown &&
                current.error == null
            ) {
                return@withLock current.status
            }
            val previous = current.status ?: cachedStatus()
            val hasToken = currentHasToken()
            _uiState.value = current.copy(isLoading = true, hasToken = hasToken)
            try {
                val status = RefreshPolicy.runBounded {
                    if (!includeSeries) {
                        val token = tokenStore.readToken()
                        if (previous != null && token != null) {
                            return@runBounded refreshLiveLatest(previous, token)
                        }
                    }
                    refreshFull()
                }
                lastSuccessAtMs = System.currentTimeMillis()
                status
            } catch (error: CancellationException) {
                finishQuietFailure(previous, hasToken)
                throw error
            } catch (error: TokenRejectedException) {
                throw error
            } catch (error: Exception) {
                finishQuietFailure(previous, hasToken)
                throw error
            }
        }
    }

    private fun finishQuietFailure(previous: HomeStatus?, hasToken: Boolean) {
        val kept = _uiState.value.status ?: previous
        _uiState.value = _uiState.value.copy(
            status = kept,
            isLoading = false,
            error = null,
            hasToken = hasToken,
            showingLastKnown = kept != null
        )
    }

    private suspend fun refreshFull(): HomeStatus {
        val publicStatus = decode(fetchPublicJson())
        val token = tokenStore.readToken()
        return if (token == null) {
            persist(HomeStatusJson.encode(publicStatus), publicStatus, hasToken = false, liveOk = false)
            _uiState.value = StatusUiState(
                status = publicStatus,
                isLoading = false,
                hasToken = false,
                liveOk = false,
                showingLastKnown = false
            )
            publicStatus
        } else {
            try {
                val live = givEnergy.fetchLive(token, includeSeries = true)
                val status = LiveStatus.merge(publicStatus, live)
                persist(HomeStatusJson.encode(status), status, hasToken = true, liveOk = true)
                _uiState.value = StatusUiState(
                    status = status,
                    isLoading = false,
                    hasToken = true,
                    liveOk = true,
                    showingLastKnown = false
                )
                status
            } catch (rejected: TokenRejectedException) {
                persist(HomeStatusJson.encode(publicStatus), publicStatus, hasToken = true, liveOk = false)
                _uiState.value = StatusUiState(
                    status = publicStatus,
                    isLoading = false,
                    error = rejected.message,
                    hasToken = true,
                    liveOk = false,
                    showingLastKnown = true
                )
                throw rejected
            } catch (liveError: CancellationException) {
                persist(HomeStatusJson.encode(publicStatus), publicStatus, hasToken = true, liveOk = false)
                if (RefreshErrors.isStructuredCancellation(liveError)) {
                    _uiState.value = _uiState.value.copy(
                        status = publicStatus,
                        isLoading = false,
                        error = null,
                        hasToken = true,
                        liveOk = false
                    )
                } else {
                    finishQuietFailure(publicStatus, hasToken = true)
                }
                throw liveError
            } catch (liveError: Exception) {
                persist(HomeStatusJson.encode(publicStatus), publicStatus, hasToken = true, liveOk = false)
                finishQuietFailure(publicStatus, hasToken = true)
                throw liveError
            }
        }
    }

    private suspend fun refreshLiveLatest(cached: HomeStatus, token: String): HomeStatus {
        return try {
            val live = givEnergy.fetchLive(token, includeSeries = false)
            val status = LiveStatus.merge(cached, live)
            persist(HomeStatusJson.encode(status), status, hasToken = true, liveOk = true)
            _uiState.value = StatusUiState(
                status = status,
                isLoading = false,
                hasToken = true,
                liveOk = true,
                showingLastKnown = false
            )
            status
        } catch (rejected: TokenRejectedException) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = rejected.message,
                hasToken = true,
                liveOk = false,
                status = cached,
                showingLastKnown = true
            )
            throw rejected
        }
    }

    private fun fetchPublicJson(): String {
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

    private suspend fun persist(raw: String, status: HomeStatus, hasToken: Boolean, liveOk: Boolean) {
        appContext.statusDataStore.edit { prefs ->
            prefs[KEY_JSON] = raw
        }
        writeSnapshot(status, hasToken, liveOk)
    }

    private fun writeSnapshot(status: HomeStatus, hasToken: Boolean, liveOk: Boolean) {
        snapshotPrefs.edit()
            .putString(KEY_SNAPSHOT, FirstPaintSnapshot.encode(status))
            .putBoolean(KEY_HAS_TOKEN, hasToken)
            .putBoolean(KEY_LIVE_OK, liveOk)
            .apply()
    }

    private suspend fun readCachedJson(): String? {
        val prefs = appContext.statusDataStore.data.first()
        return prefs[KEY_JSON]
    }

    private fun currentHasToken(): Boolean =
        snapshotPrefs.getBoolean(KEY_HAS_TOKEN, false) ||
            runCatching { tokenStore.hasToken() }.getOrDefault(false)

    companion object {
        const val STATUS_URL =
            "https://raw.githubusercontent.com/Zekkers/watt-home-status/main/status.json"
        private const val USER_AGENT =
            "WattHomeStatus/1.2.5 (home energy; +https://github.com/Zekkers/watt-home-status)"
        private val KEY_JSON = stringPreferencesKey("status_json")
        private const val SETUP_PREFS = "watt_home_setup"
        private const val SNAPSHOT_PREFS = "watt_home_first_paint"
        private const val KEY_SEEN_TOKEN = "seen_token_screen"
        private const val KEY_SNAPSHOT = "first_paint_json"
        private const val KEY_HAS_TOKEN = "has_token"
        private const val KEY_LIVE_OK = "live_ok"

        @Volatile
        private var instance: StatusRepository? = null

        fun get(context: Context): StatusRepository {
            return instance ?: synchronized(this) {
                instance ?: StatusRepository(context).also { instance = it }
            }
        }
    }
}
