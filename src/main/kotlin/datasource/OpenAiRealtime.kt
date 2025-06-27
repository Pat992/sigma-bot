package com.htth.sigmabot.datasource

import okhttp3.*
import okio.ByteString
import java.util.concurrent.TimeUnit

fun getClient(readTimeoutInMs: Long = 0, pingIntervalInS: Long = 0): OkHttpClient = OkHttpClient
    .Builder()
    .readTimeout(readTimeoutInMs, TimeUnit.MILLISECONDS)
    .pingInterval(pingIntervalInS, TimeUnit.SECONDS)
    .build()

fun getOpenAiRealtimeRequest(model: String, token: String): Request = Request
    .Builder()
    .url("wss://api.openai.com/v1/realtime?model=$model")
    .addHeader("OpenAI-Beta", "realtime=v1")
    .addHeader("Authorization", "Bearer $token")
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