package com.jamhour.util

import java.net.Authenticator
import java.net.CookieHandler
import java.net.ProxySelector
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.Optional
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLParameters

object HttpClient : HttpClient() {
    val client: HttpClient = newHttpClient()

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