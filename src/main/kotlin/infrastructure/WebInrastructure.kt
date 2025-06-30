package com.htth.sigmabot.infrastructure


import okhttp3.*
import okio.ByteString
import java.util.concurrent.TimeUnit

fun getClient(readTimeoutInMs: Long = 0, pingIntervalInS: Long = 0): OkHttpClient = OkHttpClient
    .Builder()
    .readTimeout(readTimeoutInMs, TimeUnit.MILLISECONDS)
    .pingInterval(pingIntervalInS, TimeUnit.SECONDS)
    .build()

fun getRequest(url: String, headers: List<Pair<String, String>>): Request = Request
    .Builder()
    .url(url)
    .also { req ->
        headers.forEach { header ->
            req.addHeader(header.first, header.second)
        }
    }
    .build()

fun getWebsocketListener(
    onOpen: (WebSocket, Response) -> Unit,
    onMessage: (WebSocket, Any) -> Unit,
    onFailure: (WebSocket, Throwable, Response?) -> Unit,
    onClosing: (WebSocket, Int, String) -> Unit
): WebSocketListener =
    object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            onOpen(webSocket, response)
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            onMessage(webSocket, text)
        }

        override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
            onMessage(webSocket, bytes)
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            onFailure(webSocket, t, response)
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            onClosing(webSocket, code, reason)
        }
    }