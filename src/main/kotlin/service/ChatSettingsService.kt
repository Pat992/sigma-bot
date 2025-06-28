package com.htth.sigmabot.service

import com.htth.sigmabot.data.CharacterEnum
import com.htth.sigmabot.data.ChatSettingsDto
import com.htth.sigmabot.data.ModelEnum
import com.htth.sigmabot.data.VoiceEnum
import com.htth.sigmabot.datasource.getInput
import java.util.*

fun initializeChatSettings(scanner: Scanner): ChatSettingsDto {
    // Default values
    var model = ModelEnum.GPT_4O_REALTIME_PREVIEW
    var voice = VoiceEnum.VERSE
    var character = CharacterEnum.DEFAULT_BOT
    var language = "english"

    // Get model
    valueFromInput(
        scanner,
        "model",
        ModelEnum.entries.joinToString("\n") { enum -> "${enum.ordinal}: ${enum.value}" },
        model.value,
        ModelEnum.entries.map { enum -> enum.ordinal.toString() }.toList(),
    )?.let {
        model = ModelEnum.entries[it.toInt()]
    }

    // Get voice
    valueFromInput(
        scanner,
        "voice",
        VoiceEnum.entries.joinToString("\n") { enum -> "${enum.ordinal}: ${enum.value}" },
        voice.value,
        VoiceEnum.entries.map { enum -> enum.ordinal.toString() }.toList(),
    )?.let {
        voice = VoiceEnum.entries[it.toInt()]
    }

    // Get language
    getInput(
        scanner, "Select language, default is english", emptyList(), null
    ) {
        if (it.trim().isNotBlank()) {
            println("$it selected.")
            language = it
        } else {
            println("$language selected.")
        }
    }

    // Get character
    valueFromInput(
        scanner,
        "character",
        CharacterEnum.entries.joinToString("\n") { enum -> "${enum.ordinal}: ${enum.value} - ${enum.description}" },
        character.value,
        CharacterEnum.entries.map { enum -> enum.ordinal.toString() }.toList(),
    )?.let {
        character = CharacterEnum.entries[it.toInt()]
    }

    return ChatSettingsDto(
        model = model,
        voice = voice,
        character = character,
        language = language
    )
}

private fun valueFromInput(
    scanner: Scanner,
    type: String,
    text: String,
    defaultValue: String,
    validValues: List<String>
): String? {
    var out: String? = null
    getInput(
        scanner,
        "Select $type, default is $defaultValue:\n$text",
        validValues,
        onInvalidInput = {
            println("Invalid $type selected, using '$defaultValue'")
        }
    ) {
        println("$it selected.")
        out = it
    }

    return out
}