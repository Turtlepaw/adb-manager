package com.turtlepaw.adb

object AppPreferences {
    private val preferences = PreferencesManager("adb_gui_preferences.properties")

    var lastConnectedIp: String
        get() = preferences.getString("last_ip", "")
        set(value) = preferences.putString("last_ip", value)

    var lastPort: Int
        get() = preferences.getInt("last_port", 5555)
        set(value) = preferences.putInt("last_port", value)

    var showAdbVersion: Boolean
        get() = preferences.getBoolean("showAdbVersion", true)
        set(value) = preferences.putBoolean("showAdbVersion", value)
}