package com.htth.sigmabot.datasource

import java.util.*

fun getInput(scanner: Scanner, message: String, validValues: List<String>, onValidInput: (String) -> Unit) {
    print(message)
    val input = scanner.nextLine()
    if (!validValues.contains(input.trim())) {
        println("Invalid Input")
        getInput(scanner, message, validValues, onValidInput)
    }
    onValidInput(input)
}