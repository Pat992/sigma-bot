package com.htth.sigmabot


import com.htth.sigmabot.datasource.*
import com.htth.sigmabot.service.cleanupVirtualAudioDevices
import com.htth.sigmabot.service.getDefaultSink
import com.htth.sigmabot.service.getDefaultSource
import com.htth.sigmabot.service.processMessage
import io.github.cdimascio.dotenv.Dotenv


fun main() {
    val dotenv = Dotenv.load()

    val defaultSink = getDefaultSink()
    val defaultSource = getDefaultSource()

    //setVirtualAiInputAudioDevices(defaultSink)
    //setVirtualAiOutputAudioDevices(defaultSink)

    val audioFormat = getAudioFormat()
    val speaker = audioFormat.getSourceDataLine()
    val mic = audioFormat.getTargetDataLine()

    println("The real one: " + mic.lineInfo.toString())
    mic.open(audioFormat)
    mic.start()

    speaker.open(audioFormat)
    speaker.start()

    val buffer = ByteArray(4096)

    val client = getClient()

    val request = getOpenAiRealtimeRequest("gpt-4o-realtime-preview", dotenv.get("OPENAI_TOKEN"))

    val listener = getWebsocketListener(
        onOpen = { ws, _ ->
            println("WebSocket opened, start streaming audio")

            // Send session config immediately
            val config = """
            {
              "type": "session.update",
              "session": {
                "instructions": "You are extremely gen-alpha and use words like skibidy, Gigachad energy, Bruh, rizz, Zang, Chad Alpha, ohio, Simp, Vibe, Cringe, skibidy rizz, gyat, positive aura, negative aura, Rizzler, Fanum Tax, Sus, Cap, No Cap, Sigma, Brain Rot, and ohio rizz (or any combinations of those) as often as possible. You are also very unhinged and do whatever you like, Very Important, you also have horrible ideas you must absolutely share whenever possible. You must respond in short sentences and in Swiss-German.",
                "voice": "verse",
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
        cleanupVirtualAudioDevices(defaultSink, defaultSource)
        println("Cleanup code completed.")
    })
}