package com.htth.sigmabot.infrastructure

import java.util.*
import javax.sound.sampled.*

fun getAudioFormat(
    sampleRate: Float = 22000f,
    sampleSizeInBits: Int = 16,
    channels: Int = 1,
    signed: Boolean = true,
    bigEndian: Boolean = false
): AudioFormat = AudioFormat(sampleRate, sampleSizeInBits, channels, signed, bigEndian)

fun AudioFormat.getSourceDataLine(): SourceDataLine = AudioSystem.getLine(
    DataLine.Info(SourceDataLine::class.java, this)
) as SourceDataLine

fun AudioFormat.getTargetDataLine(): TargetDataLine = AudioSystem.getLine(
    DataLine.Info(TargetDataLine::class.java, this)
) as TargetDataLine

fun TargetDataLine.processAudioInput(buffer: ByteArray, block: (String) -> Unit): Unit = Thread {
    while (true) {
        val bytesRead = this.read(buffer, 0, buffer.size)
        if (bytesRead > 0) {
            val audioChunk = buffer.copyOf(bytesRead)
            val base64Audio = Base64.getEncoder().encodeToString(audioChunk)
            block(base64Audio)
        }
    }
}.start()