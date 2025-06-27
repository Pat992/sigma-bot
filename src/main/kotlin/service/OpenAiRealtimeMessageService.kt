package com.htth.sigmabot.service

import kotlinx.serialization.json.*
import okio.ByteString
import java.util.*
import javax.sound.sampled.SourceDataLine

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
