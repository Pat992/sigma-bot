package com.htth.sigmabot.service

import com.htth.sigmabot.data.ChatSettingsDto
import com.htth.sigmabot.data.toInstructions
import com.htth.sigmabot.infrastructure.getRequest
import kotlinx.serialization.json.*
import okhttp3.Request
import okhttp3.WebSocket
import okio.ByteString
import java.util.*
import javax.sound.sampled.SourceDataLine


fun getOpenAiRealtimeRequest(chatSettings: ChatSettingsDto, token: String): Request = getRequest(
    url = "wss://api.openai.com/v1/realtime?model=${chatSettings.model.value}",
    headers = listOf(
        "OpenAI-Beta" to "realtime=v1",
        "Authorization" to "Bearer $token"
    )
)

fun sendInitMessage(chatSettings: ChatSettingsDto, ws: WebSocket) {
    val config = """
            {
              "type": "session.update",
              "session": {
                "instructions": "${chatSettings.toInstructions()}",
                "voice": "${chatSettings.voice.value.lowercase()}",
                "temperature": 0.6,
                "speed": 1
              }
            }
        """.trimIndent()

    ws.send(config)
}

fun processMessage(msg: Any, sourceDataLine: SourceDataLine): Unit = when (msg) {
    is String -> processStringMessage(msg, sourceDataLine)
    is ByteString -> processByteStringMessage(msg)
    else -> println("Message $msg is invalid.")
}

private fun processStringMessage(msg: String, sourceDataLine: SourceDataLine) {
    try {
        val json = Json.parseToJsonElement(msg).jsonObject
        val type = json["type"]?.jsonPrimitive?.contentOrNull
        val response = json["response"]?.jsonObject
        val status = response?.get("status")?.jsonPrimitive?.contentOrNull

        if (type == "session.created" || type == "session.updated") {
            println(msg)
        } else if (type == "response.audio.delta") {
            val base64Audio = json["delta"]?.jsonPrimitive?.contentOrNull
            if (!base64Audio.isNullOrBlank()) {
                val audioBytes = Base64.getDecoder().decode(base64Audio)
                sourceDataLine.write(audioBytes, 0, audioBytes.size)
            }
        } else if (type == "response.done" && status == "completed") {
            val out =
                response["output"]
                    ?.jsonArray
                    ?.firstOrNull()
                    ?.jsonObject
                    ?.get("content")
                    ?.jsonArray
                    ?.firstOrNull()
                    ?.jsonObject?.get("transcript")
                    ?.jsonPrimitive
                    ?.contentOrNull

            println("Received response.done message: $out")
        }

    } catch (e: Exception) {
        println("Failed to parse message: $msg")
        println(e)
    }
}

private fun processByteStringMessage(msg: ByteString) = println("Received binary message: ${msg.hex()}")
