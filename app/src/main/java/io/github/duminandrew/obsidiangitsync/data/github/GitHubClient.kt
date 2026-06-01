package io.github.duminandrew.obsidiangitsync.data.github

import io.github.duminandrew.obsidiangitsync.BuildConfig
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Builds a [GitHubApi] bound to a specific PAT.
 *
 * Security: the PAT is injected via an Authorization header by an interceptor.
 * The HTTP logging interceptor is enabled only in debug builds and is set to
 * BASIC level (method + URL + status), which NEVER includes headers or bodies,
 * so the token is never logged.
 */
object GitHubClient {

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
    }

    fun create(token: String): GitHubApi {
        val authInterceptor = okhttp3.Interceptor { chain ->
            val request = chain.request().newBuilder()
                .header("Authorization", "Bearer $token")
                .header("Accept", "application/vnd.github+json")
                .header("X-GitHub-Api-Version", "2022-11-28")
                .build()
            chain.proceed(request)
        }

        val clientBuilder = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)

        if (BuildConfig.DEBUG) {
            // BASIC never prints headers/bodies -> the Bearer token is not logged.
            clientBuilder.addInterceptor(
                HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
            )
        }

        val contentType = "application/json".toMediaType()

        return Retrofit.Builder()
            .baseUrl(GitHubApi.BASE_URL)
            .client(clientBuilder.build())
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(GitHubApi::class.java)
    }
}
