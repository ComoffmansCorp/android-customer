package com.example.myapplication.network

import com.example.myapplication.AppSettings
import com.example.myapplication.AuthManager
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.Route
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    private val gson = GsonBuilder().setLenient().create()

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    // Bare client for the refresh call itself: it must not go through the
    // auth interceptor/authenticator below, or a refresh failure would
    // recursively try to refresh again.
    private val refreshClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    private fun refreshApi(baseUrl: String): ApiService = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(refreshClient)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()
        .create(ApiService::class.java)

    /** Blocks the calling (background) OkHttp thread — Authenticator has no suspend variant. */
    private val authenticator = Authenticator { _, response ->
        if (response.retryCount() >= 1) return@Authenticator null // already retried once, give up

        val refreshToken = AuthManager.refreshToken
        if (refreshToken == null) {
            AuthManager.clear()
            return@Authenticator null
        }

        val renewed = runBlocking {
            runCatching { refreshApi(AppSettings.baseUrl).refresh(RefreshRequest(refreshToken)) }.getOrNull()
        }
        if (renewed == null) {
            AuthManager.clear()
            return@Authenticator null
        }
        AuthManager.saveTokens(renewed.accessToken, renewed.refreshToken)

        response.request.newBuilder()
            .header("Authorization", "Bearer ${renewed.accessToken}")
            .build()
    }

    private fun Response.retryCount(): Int {
        var count = 0
        var prior = priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }

    private fun buildOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .addInterceptor { chain ->
            val token = AuthManager.accessToken
            val request = if (token != null) {
                chain.request().newBuilder().addHeader("Authorization", "Bearer $token").build()
            } else {
                chain.request()
            }
            chain.proceed(request)
        }
        .authenticator(authenticator)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private fun buildRetrofit(baseUrl: String): Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(buildOkHttpClient())
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()

    private var cachedBaseUrl: String? = null
    private var cachedApi: ApiService? = null

    // Rebuilt whenever AppSettings.baseUrl changes (e.g. user edits it on the
    // Settings screen), so a fresh Retrofit/OkHttp instance always points at
    // the currently configured server.
    val api: ApiService
        get() {
            val baseUrl = AppSettings.baseUrl
            if (cachedApi == null || cachedBaseUrl != baseUrl) {
                cachedBaseUrl = baseUrl
                cachedApi = buildRetrofit(baseUrl).create(ApiService::class.java)
            }
            return cachedApi!!
        }
}

/**
 * Backend errors are RFC 7807 problem+json with a human-readable `detail`
 * (e.g. "act must be filled in before completing the task") — far more
 * useful to show the inspector than the raw "HTTP 400" from [Throwable.message].
 */
fun Throwable.toUserMessage(): String {
    if (this is HttpException) {
        val body = response()?.errorBody()?.string()
        if (!body.isNullOrBlank()) {
            val parsed = runCatching { Gson().fromJson(body, ApiError::class.java) }.getOrNull()
            val detail = parsed?.detail?.takeIf { it.isNotBlank() } ?: parsed?.title?.takeIf { it.isNotBlank() }
            if (detail != null) return detail
        }
        return "Ошибка сервера (${code()})"
    }
    return message ?: "Не удалось выполнить запрос"
}
