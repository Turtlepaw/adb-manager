package com.turtlepaw.adb

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Properties

class PreferencesManager(private val fileName: String = "app_preferences.properties") {
    private val properties = Properties()
    private val preferencesFile = File(System.getProperty("user.home"), ".${fileName}")

    init {
        loadPreferences()
    }

    private fun loadPreferences() {
        if (preferencesFile.exists()) {
            FileInputStream(preferencesFile).use {
                properties.load(it)
            }
        }
    }

    private fun savePreferences() {
        preferencesFile.parentFile?.mkdirs()
        FileOutputStream(preferencesFile).use {
            properties.store(it, "Application Preferences")
        }
    }

    fun getString(key: String, defaultValue: String = ""): String {
        return properties.getProperty(key, defaultValue)
    }

    fun putString(key: String, value: String) {
        properties.setProperty(key, value)
        savePreferences()
    }

    fun getBoolean(key: String, defaultValue: Boolean = false): Boolean {
        val value = properties.getProperty(key) ?: return defaultValue
        return value.lowercase() == "true"
    }

    fun putBoolean(key: String, value: Boolean) {
        properties.setProperty(key, value.toString())
        savePreferences()
    }

    fun getInt(key: String, defaultValue: Int = 0): Int {
        val value = properties.getProperty(key) ?: return defaultValue
        return try {
            value.toInt()
        } catch (e: NumberFormatException) {
            defaultValue
        }
    }

    fun putInt(key: String, value: Int) {
        properties.setProperty(key, value.toString())
        savePreferences()
    }

    fun remove(key: String) {
        properties.remove(key)
        savePreferences()
    }

    fun clear() {
        properties.clear()
        savePreferences()
    }
}