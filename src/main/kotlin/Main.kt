package com.htth.sigmabot


import com.htth.sigmabot.data.toInstructions
import com.htth.sigmabot.datasource.*
import com.htth.sigmabot.service.*
import io.github.cdimascio.dotenv.Dotenv
import java.util.*


fun main() {
    val dotenv = Dotenv.load()
    val scanner = Scanner(System.`in`)
    val osName = System.getProperty("os.name").lowercase()

    val defaultSink = getDefaultSink()
    val defaultSource = getDefaultSource()

    // --------- Audio input and output method ---------
    getInput(
        scanner,
        "Select audio method:\n0 - Direct Chat (Chat directly with the AI)\n1 - Audio Pipe (Use the AI in a meeting or call, only works on Linux)",
        listOf("0", "1"),
        null
    ) {
        if (it == "1" && osName.lowercase().contains("linux")) {
            setVirtualAiInputAudioDevices(defaultSink)
            setVirtualAiOutputAudioDevices(defaultSink)
        }
    }

    // --------- Chat settings ---------
    val chatSettings = initializeChatSettings(scanner)

    val audioFormat = getAudioFormat()
    val speaker = audioFormat.getSourceDataLine()
    val mic = audioFormat.getTargetDataLine()

    mic.open(audioFormat)
    mic.start()

    speaker.open(audioFormat)
    speaker.start()

    val buffer = ByteArray(4096)

    val client = getClient()

    val request = getOpenAiRealtimeRequest(chatSettings.model.value, dotenv.get("OPENAI_TOKEN"))

    val listener = getWebsocketListener(
        onOpen = { ws, _ ->
            println("WebSocket opened, start streaming audio")

            // Send session config immediately
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

            mic.processAudioInput(buffer) {
                val audioMessage = """
                        {
                          "type": "input_audio_buffer.append",
                          "audio": "$it"
                        }
                        """.trimIndent()
                ws.send(audioMessage)
            }
        },
        onMessage = { _, msg ->
            processMessage(msg, speaker)
        },
        onFailure = { _, t, res ->
            println("WebSocket failed: ${t.message}")
            println(res?.message)
        },
        onClosing = { ws, code, reason ->
            println("WebSocket closing: $code / $reason")
            ws.close(code, reason)
        }
    )

    val ws = client.newWebSocket(request, listener)

    // shutdown hook
    Runtime.getRuntime().addShutdownHook(Thread {
        println("Cleanup code running before shutdown...")
        ws.close(1000, "Websocket shutdown")
        if (osName.lowercase().contains("linux")) cleanupVirtualAudioDevices(defaultSink, defaultSource)
        println("Cleanup code completed.")
    })
}