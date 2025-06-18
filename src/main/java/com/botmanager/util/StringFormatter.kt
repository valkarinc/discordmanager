package com.botmanager.util

object StringFormatter {
    @JvmStatic
    fun formatBotStatus(botName: String, isRunning: Boolean): String {
        return if (isRunning) {
            "$botName is currently ONLINE."
        } else {
            "$botName is OFFLINE."
        }
    }

    @JvmStatic
    fun getGreeting(name: String): String {
        return "Hello, $name from Kotlin!"
    }
}