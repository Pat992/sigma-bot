package com.htth.sigmabot.infrastructure

import java.util.*

fun getInput(
    scanner: Scanner,
    message: String,
    validValues: List<String> = emptyList(),
    onInvalidInput: (() -> Unit)?,
    onValidInput: (String) -> Unit
) {
    println(message)
    print("> ")
    val input = scanner.nextLine()
    if (validValues.isNotEmpty() && !validValues.contains(input.trim())) {
        if (onInvalidInput != null) {
            onInvalidInput()
        } else {
            println("Invalid Input")
            getInput(scanner, message, validValues, null, onValidInput)
        }
        return
    }
    onValidInput(input)
}