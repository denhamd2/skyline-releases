package com.denham.skyline.data.api

import com.denham.skyline.core.XtreamAccount
import com.denham.skyline.core.XtreamJson
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Builds the OkHttp + Retrofit stack for one Xtream account.
 *
 * The user agent is read through [uaProvider] on every request, so changing
 * it in Settings applies immediately — no client rebuild. Many providers
 * filter/block on UA (an empty UA is commonly rejected outright).
 */
class ApiClientFactory(
    private val uaProvider: () -> String,
) {

    fun okHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .addInterceptor(UserAgentInterceptor(uaProvider))
        .build()

    fun xtreamApi(client: OkHttpClient, account: XtreamAccount): XtreamApi =
        Retrofit.Builder()
            .baseUrl(account.portal.baseUrl + "/")
            .client(
                client.newBuilder()
                    .addInterceptor(CredentialsInterceptor(account))
                    .build()
            )
            .addConverterFactory(
                XtreamJson.asConverterFactory("application/json".toMediaType())
            )
            .build()
            .create(XtreamApi::class.java)
}

class UserAgentInterceptor(private val uaProvider: () -> String) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val ua = uaProvider().ifBlank { DEFAULT_USER_AGENT }
        return chain.proceed(
            chain.request().newBuilder().header("User-Agent", ua).build()
        )
    }

    companion object {
        // A widely-accepted player UA; configurable in Settings.
        const val DEFAULT_USER_AGENT = "IPTVSmarters"
    }
}

/** Appends username/password query params to every player_api call. */
class CredentialsInterceptor(private val account: XtreamAccount) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val url = chain.request().url.newBuilder()
            .addQueryParameter("username", account.username)
            .addQueryParameter("password", account.password)
            .build()
        return chain.proceed(chain.request().newBuilder().url(url).build())
    }
}
