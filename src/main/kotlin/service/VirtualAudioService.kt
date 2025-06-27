package com.htth.sigmabot.service

import com.htth.sigmabot.datasource.runInlineCommand

fun listSinks(): String = runInlineCommand("pactl", "list", "short", "sinks")

fun listSources(): String = runInlineCommand("pactl", "list", "short", "sources")

fun getDefaultSink(): String = runInlineCommand("pactl", "get-default-sink")

fun getDefaultSource(): String = runInlineCommand("pactl", "get-default-source")

fun setVirtualAiInputAudioDevices(defaultSpeaker: String) {
    println("🎛 Creating virtual speaker (null sink)...")
    runInlineCommand(
        "pactl",
        "load-module",
        "module-null-sink",
        "sink_name=virtual_speaker",
        "sink_properties=device.description=VirtualSpeaker",
        "rate=48000"
    )

    println("\uD83C\uDFA4 Creating virtual microphone...")
    runInlineCommand(
        "pactl",
        "load-module",
        "module-null-sink",
        "sink_name=virtual_mic_sink",
        "sink_properties=device.description=VirtualMicrophone",
        "rate=48000"
    )

    println("\uD83D\uDD27 Setting virtual microphone as default sink...")
    runInlineCommand("pactl", "set-default-source", "virtual_mic_sink.monitor")

    println("🔁 Routing virtual speaker output to real sink $defaultSpeaker...")
    runInlineCommand(
        "pactl",
        "load-module",
        "module-loopback",
        "source=virtual_speaker.monitor",
        "sink=$defaultSpeaker",
        "latency_msec=10",
        "resample-method=copy"
    )

    println("🔁 Routing virtual speaker output to virtual mic sink...")
    runInlineCommand(
        "pactl",
        "load-module",
        "module-loopback",
        "source=virtual_speaker.monitor",
        "sink=virtual_mic_sink",
        "latency_msec=10",
        "resample-method=copy"
    )

    println("\uD83C\uDFA7 Available Sinks:")
    println(listSinks())

    println("\uD83C\uDF99 Available Sources:")
    println(listSources())
}

fun setVirtualAiOutputAudioDevices(defaultSpeaker: String) {
    println("🎧 Creating virtual AI speaker...")
    runInlineCommand(
        "pactl",
        "load-module",
        "module-null-sink",
        "sink_name=virtual_ai_sink",
        "sink_properties=device.description=VirtualAISpeaker",
        "rate=48000"
    )

    println("\uD83D\uDD27 Setting virtual speaker as default sink...")
    runInlineCommand("pactl", "set-default-sink", "virtual_ai_sink.monitor")

    println("\uD83C\uDFA4 Creating virtual microphone...")
    runInlineCommand(
        "pactl",
        "load-module",
        "module-null-sink",
        "sink_name=virtual_ai_mic_sink",
        "sink_properties=device.description=VirtualAiMicrophone",
        "rate=48000"
    )

    println("🔁 Loop AI speaker to real output: $defaultSpeaker")
    runInlineCommand(
        "pactl",
        "load-module",
        "module-loopback",
        "source=virtual_ai_sink.monitor",
        "sink=$defaultSpeaker",
        "latency_msec=10",
        "resample-method=copy"
    )

    println("🔁 Loop AI speaker to virtual mic sink")
    runInlineCommand(
        "pactl",
        "load-module",
        "module-loopback",
        "source=virtual_ai_sink.monitor",
        "sink=virtual_ai_mic_sink",
        "latency_msec=10",
        "resample-method=copy"
    )

    println("\uD83C\uDFA7 Available Sinks:")
    println(listSinks())

    println("\uD83C\uDF99 Available Sources:")
    println(listSources())
}

fun cleanupVirtualAudioDevices(defaultSpeaker: String, defaultMicrophone: String) {
    println("🧹 Cleaning up virtual audio devices...")

    val modules = runInlineCommand("pactl", "list", "short", "modules")
        .lines()
        .filter { it.contains("virtual_") || it.contains("loopback") }

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