package com.htth.sigmabot


import io.github.cdimascio.dotenv.Dotenv
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.*
import okio.ByteString
import java.util.*
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import javax.sound.sampled.*

fun main() {
    val dotenv = Dotenv.load()

    val sampleRate = 16000f
    val audioFormat = AudioFormat(sampleRate, 16, 1, true, false)
    val audioFormatSpeaker = AudioFormat(sampleRate, 16, 1, true, false)
    val info = DataLine.Info(TargetDataLine::class.java, audioFormat)
    val mic = AudioSystem.getLine(info) as TargetDataLine
    val infoSpeaker = DataLine.Info(SourceDataLine::class.java, audioFormatSpeaker)
    val speaker = AudioSystem.getLine(infoSpeaker) as SourceDataLine
    mic.open(audioFormat)
    mic.start()
    speaker.open(audioFormat)
    speaker.start()

    val buffer = ByteArray(4096)

    val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS) // No read timeout for streaming
        .pingInterval(0, TimeUnit.SECONDS)
        .build()

    val request = Request.Builder()
        .url("wss://api.openai.com/v1/realtime?model=gpt-4o-realtime-preview")
        .addHeader("OpenAI-Beta", "realtime=v1")
        .addHeader(
            "Authorization",
            "Bearer ${dotenv.get("OPENAI_TOKEN")}"
        )
        .build()

    // Queue to hold decoded PCM16 byte arrays from server
    val audioQueue = LinkedBlockingQueue<ByteArray>()

    // Consumer thread that plays audio at real-time speed
    val consumerThread = Thread {
        try {
            while (true) {
                val audioChunk = audioQueue.take() // waits if queue is empty

                // Calculate expected duration of this chunk in ms
                val bytesPerSecond = 16000 * 2 // sampleRate * bytesPerSample
                val durationMs = (audioChunk.size.toFloat() / bytesPerSecond) * 1000

                // Write audio to line
                speaker.write(audioChunk, 0, audioChunk.size)

                // Sleep to maintain real-time playback speed
                Thread.sleep(durationMs.toLong())
            }
        } catch (ex: InterruptedException) {
            // Thread interrupted, stop playback
        } finally {
            speaker.drain()
            speaker.close()
        }
    }
    consumerThread.start()

    val listener = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            println("WebSocket opened, start streaming audio")

            // MUST send session config immediately
            val config = """
            {
              "type": "session.update",
              "session": {
                "instructions": "You are extremely gen-alpha and use words like skibidy, Gigachad energy, Bruh, rizz, Zang, Chad Alpha, ohio, Simp, Vibe, Cringe, skibidy rizz, gyat, positive aura, negative aura, Rizzler, Fanum Tax, Sus, Cap, No Cap, Sigma, Brain Rot, and ohio rizz (or any combinations of those) as often as possible. You are also very unhinged and do whatever you like, Very Important, you also have horrible ideas you must absolutely share whenever possible. You must respond in short sentences and in Swiss-German.",
                "voice": "verse",
                "temperature": 0.6,
                "speed": 1.5
              }
            }
        """.trimIndent()

            webSocket.send(config)

            Thread {
                while (true) {
                    val bytesRead = mic.read(buffer, 0, buffer.size)
                    if (bytesRead > 0) {
                        val audioChunk = buffer.copyOf(bytesRead)
                        val base64Audio = Base64.getEncoder().encodeToString(audioChunk)
                        val audioMessage = """
                        {
                          "type": "input_audio_buffer.append",
                          "audio": "$base64Audio"
                        }
                        """.trimIndent()
                        webSocket.send(audioMessage)
                    }
                }
            }.start()
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            try {
                val json = kotlinx.serialization.json.Json.parseToJsonElement(text).jsonObject
                val type = json["type"]?.jsonPrimitive?.contentOrNull
                val response = json["response"]?.jsonObject
                val status = response?.get("status")?.jsonPrimitive?.contentOrNull
                if (type == "session.created" || type == "session.updated") {
                    println(text)
                } else if (type == "response.audio.delta") {
                    val base64Audio = json["delta"]?.jsonPrimitive?.contentOrNull
                    if (!base64Audio.isNullOrBlank()) {
                        val audioBytes = Base64.getDecoder().decode(base64Audio)
                        // audioQueue.put(audioBytes)
                        // audioQueue.put(audioBytes)
                        speaker.write(audioBytes, 0, audioBytes.size)
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
                println("Failed to parse message: $text")
                println(e)
            }
        }

        override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
            println("Received binary message: ${bytes.hex()}")
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            println("WebSocket failed: ${t.message}")
            println(response?.message)
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            println("WebSocket closing: $code / $reason")
            webSocket.close(code, reason)
        }
    }

    val ws = client.newWebSocket(request, listener)

    // Keep the main thread alive or handle shutdown cleanly
    // ws.close(code = 0, reason = "OK")
}