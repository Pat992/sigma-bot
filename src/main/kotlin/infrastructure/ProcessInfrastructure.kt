package com.htth.sigmabot.infrastructure

fun runInlineCommand(vararg command: String): String {
    val process = ProcessBuilder(*command)
        .redirectErrorStream(true)
        .start()

    val output = process
        .inputStream
        .bufferedReader()
        .readText()
        .trim()

    process.waitFor()

    return output
}