package com.turtlepaw.adb

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import com.konyaco.fluent.FluentTheme
import com.konyaco.fluent.darkColors
import com.konyaco.fluent.lightColors

@Composable
fun Theme(content: @Composable () -> Unit) {
    val isDarkMode = isSystemInDarkTheme()
    FluentTheme(
        colors = if (isDarkMode) darkColors() else lightColors(),
        content = content,
    )
}