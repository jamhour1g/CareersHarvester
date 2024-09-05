package com.jamhour.util

import kotlinx.coroutines.future.await
import kotlinx.serialization.StringFormat
import kotlinx.serialization.decodeFromString
import java.net.Authenticator
import java.net.CookieHandler
import java.net.ProxySelector
import java.net.URI
import java.net.http.HttpClient.Redirect
import java.net.http.HttpClient.Version
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.Optional
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.logging.Logger
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLParameters
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

fun String.toURI(): URI = URI.create(this)

suspend fun <T> HttpRequest.sendAsync(bodyHandler: HttpResponse.BodyHandler<T>): T =
    HttpClient.sendAsync(this, bodyHandler).await().body()

inline fun <reified T> StringFormat.toBodyHandler(logger: Logger? = null) = HttpResponse.BodyHandler {
    HttpResponse.BodySubscribers.mapping(
        HttpResponse.BodySubscribers.ofString(Charsets.UTF_8)
    ) {
        logger?.info { "Received response from request" }
        try {
            logger?.info { "Attempting to convert response to ${T::class.simpleName}" }
            val result = decodeFromString<T>(it)
            logger?.info { "Successfully converted response to ${T::class.simpleName}" }
            result
        } catch (e: Exception) {
            logger?.severe { "Error converting response to ${T::class.simpleName}: ${e.stackTraceToString()}" }
            null.also { logger?.info { "Returning null due to conversion error" } }
        }
    }
}

object HttpClient : java.net.http.HttpClient() {
    val client: java.net.http.HttpClient = newBuilder()
        .sslParameters(SSLParameters())
        .connectTimeout(10.seconds.toJavaDuration())
        .build()

    override fun cookieHandler(): Optional<CookieHandler> = client.cookieHandler()
    override fun connectTimeout(): Optional<Duration> = client.connectTimeout()
    override fun followRedirects(): Redirect = client.followRedirects()
    override fun proxy(): Optional<ProxySelector> = client.proxy()
    override fun sslContext(): SSLContext = client.sslContext()
    override fun sslParameters(): SSLParameters = client.sslParameters()
    override fun authenticator(): Optional<Authenticator> = client.authenticator()
    override fun version(): Version = client.version()
    override fun executor(): Optional<Executor> = client.executor()

    override fun <T> send(
        request: HttpRequest,
        responseBodyHandler: HttpResponse.BodyHandler<T>
    ): HttpResponse<T> = client.send(request, responseBodyHandler)

    override fun <T> sendAsync(
        request: HttpRequest,
        responseBodyHandler: HttpResponse.BodyHandler<T>
    ): CompletableFuture<HttpResponse<T>> = client.sendAsync(request, responseBodyHandler)

    override fun <T> sendAsync(
        request: HttpRequest,
        responseBodyHandler: HttpResponse.BodyHandler<T>,
        pushPromiseHandler: HttpResponse.PushPromiseHandler<T>
    ): CompletableFuture<HttpResponse<T>> = client.sendAsync(request, responseBodyHandler, pushPromiseHandler)
}



