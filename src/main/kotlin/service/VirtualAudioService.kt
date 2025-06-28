package com.htth.sigmabot.service

import com.htth.sigmabot.datasource.runInlineCommand

val virtualAiInDevices: List<Pair<String, String>> = listOf(
    "vrt_ai_in_speaker" to "AI_In_Speaker_For_Meeting",
    "vrt_ai_in_microphone" to "AI_In_Microphone"
)

val virtualAiOutDevices: List<Pair<String, String>> = listOf(
    "vrt_ai_out_speaker" to "AI_Out_Speaker",
    "vrt_ai_out_microphone" to "AI_Out_Microphone_For_Meeting"
)

fun getDefaultSink(): String = runInlineCommand("pactl", "get-default-sink")

fun getDefaultSource(): String = runInlineCommand("pactl", "get-default-source")

fun setVirtualAiInputAudioDevices(defaultSpeaker: String) {
    val (speakerName: String, speakerDescription: String) = virtualAiInDevices.first()
    val (microphoneName: String, microphoneDescription: String) = virtualAiInDevices.last()

    println("🎛 Creating virtual speaker (null sink)...")
    runInlineCommand(
        "pactl",
        "load-module",
        "module-null-sink",
        "sink_name=$speakerName",
        "sink_properties=device.description=$speakerDescription",
        "rate=48000"
    )

    println("🎙️ Creating remapped source from $speakerName.monitor...")
    runInlineCommand(
        "pactl",
        "load-module",
        "module-remap-source",
        "master=$speakerName.monitor",
        "source_name=$microphoneName",
        "source_properties=device.description=$microphoneDescription"
    )

    println("🔁 Routing virtual speaker output to real sink $defaultSpeaker...")
    runInlineCommand(
        "pactl",
        "load-module",
        "module-loopback",
        "source=$speakerName.monitor",
        "sink=$defaultSpeaker",
        "latency_msec=10",
        "resample-method=copy"
    )

    println("\uD83D\uDD27 Setting virtual microphone as default sink...")
    runInlineCommand("pactl", "set-default-source", "$microphoneName.monitor")
}

fun setVirtualAiOutputAudioDevices(defaultSpeaker: String) {
    val (speakerName: String, speakerDescription: String) = virtualAiOutDevices.first()
    val (microphoneName: String, microphoneDescription: String) = virtualAiOutDevices.last()

    println("🎧 Creating virtual AI speaker...")
    runInlineCommand(
        "pactl",
        "load-module",
        "module-null-sink",
        "sink_name=$speakerName",
        "sink_properties=device.description=$speakerDescription",
        "rate=48000"
    )

    println("🎙️ Creating remapped source from $speakerName.monitor...")
    runInlineCommand(
        "pactl",
        "load-module",
        "module-remap-source",
        "master=$speakerName.monitor",
        "source_name=$microphoneName",
        "source_properties=device.description=$microphoneDescription"
    )

    println("🔁 Loop AI speaker to real output: $defaultSpeaker")
    runInlineCommand(
        "pactl",
        "load-module",
        "module-loopback",
        "source=$speakerName.monitor",
        "sink=$defaultSpeaker",
        "latency_msec=10",
        "resample-method=copy"
    )


    println("\uD83D\uDD27 Setting virtual speaker as default sink...")
    runInlineCommand("pactl", "set-default-sink", speakerName)
}

fun cleanupVirtualAudioDevices(defaultSpeaker: String, defaultMicrophone: String) {
    println("🧹 Cleaning up virtual audio devices...")

    val modules = runInlineCommand("pactl", "list", "short", "modules")
        .lines()
        .filter { it.contains("vrt_") || it.contains("loopback") }

    val moduleIds = modules.mapNotNull { line ->
        line.split(Regex("\\s+")).firstOrNull()?.toIntOrNull()
    }

    moduleIds.forEach { id ->
        println("🔻 Unloading module ID $id")
        runInlineCommand("pactl", "unload-module", id.toString())
    }

    println("🎯 Restoring default sink: $defaultSpeaker")
    runInlineCommand("pactl", "set-default-sink", defaultSpeaker)

    println("🎯 Restoring default source: $defaultMicrophone")
    runInlineCommand("pactl", "set-default-source", defaultMicrophone)

    println("✅ Cleanup complete.")
}