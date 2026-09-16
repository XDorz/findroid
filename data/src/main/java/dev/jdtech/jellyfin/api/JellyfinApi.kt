package dev.jdtech.jellyfin.api

import android.content.Context
import android.os.SystemClock
import dev.jdtech.jellyfin.data.BuildConfig
import dev.jdtech.jellyfin.settings.domain.Constants
import java.util.UUID
import kotlin.time.DurationUnit
import kotlin.time.toDuration
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.jellyfin.sdk.api.client.HttpClientOptions
import org.jellyfin.sdk.api.client.extensions.brandingApi
import org.jellyfin.sdk.api.client.extensions.devicesApi
import org.jellyfin.sdk.api.client.extensions.itemsApi
import org.jellyfin.sdk.api.client.extensions.mediaInfoApi
import org.jellyfin.sdk.api.client.extensions.mediaSegmentsApi
import org.jellyfin.sdk.api.client.extensions.playStateApi
import org.jellyfin.sdk.api.client.extensions.quickConnectApi
import org.jellyfin.sdk.api.client.extensions.sessionApi
import org.jellyfin.sdk.api.client.extensions.suggestionsApi
import org.jellyfin.sdk.api.client.extensions.systemApi
import org.jellyfin.sdk.api.client.extensions.trickplayApi
import org.jellyfin.sdk.api.client.extensions.tvShowsApi
import org.jellyfin.sdk.api.client.extensions.userApi
import org.jellyfin.sdk.api.client.extensions.userLibraryApi
import org.jellyfin.sdk.api.client.extensions.userViewsApi
import org.jellyfin.sdk.api.client.extensions.videosApi
import org.jellyfin.sdk.createJellyfin
import org.jellyfin.sdk.model.ClientInfo
import org.jellyfin.sdk.model.api.UserDto

/**
 * Jellyfin API class using org.jellyfin.sdk:jellyfin-platform-android
 *
 * @param androidContext The context
 * @param socketTimeout The socket timeout
 * @constructor Creates a new [JellyfinApi] instance
 */
class JellyfinApi(
    androidContext: Context,
    requestTimeout: Long = Constants.NETWORK_DEFAULT_REQUEST_TIMEOUT,
    connectTimeout: Long = Constants.NETWORK_DEFAULT_CONNECT_TIMEOUT,
    socketTimeout: Long = Constants.NETWORK_DEFAULT_SOCKET_TIMEOUT,
) {
    val jellyfin = createJellyfin {
        clientInfo =
            ClientInfo(
                name =
                    androidContext.applicationInfo
                        .loadLabel(androidContext.packageManager)
                        .toString(),
                version = BuildConfig.VERSION_NAME,
            )
        context = androidContext
    }
    val api =
        jellyfin.createApi(
            httpClientOptions =
                HttpClientOptions(
                    requestTimeout = requestTimeout.toDuration(DurationUnit.MILLISECONDS),
                    connectTimeout = connectTimeout.toDuration(DurationUnit.MILLISECONDS),
                    socketTimeout = socketTimeout.toDuration(DurationUnit.MILLISECONDS),
                )
        )
    var userId: UUID? = null

    val brandingApi = api.brandingApi
    val devicesApi = api.devicesApi
    val itemsApi = api.itemsApi
    val mediaInfoApi = api.mediaInfoApi
    val mediaSegmentsApi = api.mediaSegmentsApi
    val playStateApi = api.playStateApi
    val quickConnectApi = api.quickConnectApi
    val sessionApi = api.sessionApi
    val showsApi = api.tvShowsApi
    val suggestionsApi = api.suggestionsApi
    val systemApi = api.systemApi
    val trickplayApi = api.trickplayApi
    val userApi = api.userApi
    val userLibraryApi = api.userLibraryApi
    val videosApi = api.videosApi
    val viewsApi = api.userViewsApi

    private val userMutex = Mutex()
    private var cachedUser: UserDto? = null
    private var cachedUserKey: Triple<String?, UUID?, String?>? = null
    private var cachedUserAt = 0L

    // Configuration and administrator visibility share the same existing user request.
    suspend fun currentUser(): UserDto = userMutex.withLock {
        val key = Triple(api.baseUrl, userId, api.accessToken)
        val now = SystemClock.elapsedRealtime()
        cachedUser?.let {
            if (cachedUserKey == key && now - cachedUserAt < 60_000) return@withLock it
        }
        val user = userApi.getCurrentUser().content
        if (key != Triple(api.baseUrl, userId, api.accessToken)) {
            throw IllegalStateException("Server session changed")
        }
        cachedUser = user
        cachedUserKey = key
        cachedUserAt = now
        user
    }

    companion object {
        @Volatile private var INSTANCE: JellyfinApi? = null

        fun getInstance(
            context: Context,
            requestTimeout: Long = Constants.NETWORK_DEFAULT_REQUEST_TIMEOUT,
            connectTimeout: Long = Constants.NETWORK_DEFAULT_CONNECT_TIMEOUT,
            socketTimeout: Long = Constants.NETWORK_DEFAULT_SOCKET_TIMEOUT,
        ): JellyfinApi {
            synchronized(this) {
                var instance = INSTANCE
                if (instance == null) {
                    instance =
                        JellyfinApi(
                            androidContext = context.applicationContext,
                            requestTimeout = requestTimeout,
                            connectTimeout = connectTimeout,
                            socketTimeout = socketTimeout,
                        )
                    INSTANCE = instance
                }
                return instance
            }
        }
    }
}
