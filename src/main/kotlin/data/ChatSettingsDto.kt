package com.htth.sigmabot.data

data class ChatSettingsDto(
    val model: ModelEnum,
    val voice: VoiceEnum,
    val character: CharacterEnum,
    val language: String
)

fun ChatSettingsDto.toInstructions() = "${character.instructions} You must respond in short sentences and in $language"