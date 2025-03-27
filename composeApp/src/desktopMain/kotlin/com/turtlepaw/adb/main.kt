package com.turtlepaw.adb

import androidx.compose.foundation.background
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.interaction.HoverInteraction
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowDecoration
import androidx.compose.ui.window.WindowScope
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material.Surface
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.singleWindowApplication
import com.konyaco.fluent.FluentTheme
import com.konyaco.fluent.background.Mica
import com.konyaco.fluent.component.Icon
import com.konyaco.fluent.icons.Icons
import com.konyaco.fluent.icons.regular.Dismiss
import com.konyaco.fluent.icons.regular.PlugDisconnected
import com.konyaco.fluent.icons.regular.Subtract
import com.mayakapps.compose.windowstyler.WindowBackdrop
import com.mayakapps.compose.windowstyler.WindowCornerPreference
import com.mayakapps.compose.windowstyler.WindowFrameStyle
import com.mayakapps.compose.windowstyler.WindowStyle
import com.turtlepaw.adb.resources.Res
import com.turtlepaw.adb.resources.icon
import org.jetbrains.compose.reload.DevelopmentEntryPoint
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import kotlin.system.exitProcess

//fun main() = application {
//    Window(
//        onCloseRequest = ::exitApplication,
//        title = "ADB",
//    ) {
//        App()
//    }
//}

fun main() = application {
    val state = rememberWindowState(width = 650.dp, height = 450.dp)

    System.setProperty("skiko.renderApi", "OPENGL")
    Window(
        title = "ADB Manager",
        state = state,
        onCloseRequest = ::exitApplication,
        undecorated = true,
        alwaysOnTop = false,
        icon = painterResource(Res.drawable.icon),
        transparent = true,
    ) {
        WindowStyle(
            backdropType = WindowBackdrop.Transparent, // Use None for true transparency
            frameStyle = WindowFrameStyle(
                cornerPreference = WindowCornerPreference.ROUNDED,
                borderColor = Color.Transparent,
                titleBarColor = Color.Transparent,
                captionColor = Color.Transparent,
            ),
            isDarkTheme = isSystemInDarkTheme(),
        )
            Surface(
                modifier = Modifier.fillMaxSize(),
                    //.padding(20.dp),
                color = Color.Transparent,
                shape = RoundedCornerShape(12.dp),
                //elevation = 3.dp
            ) {
                Theme {
                    // Content within the box
                    Mica(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            AppWindowTitleBar {
                                state.isMinimized = !state.isMinimized
                            }
                            App(state)
                        }
                    }
                }
        }
    }
}

@Composable
private fun WindowScope.AppWindowTitleBar(onMinimize: () -> Unit) = WindowDraggableArea {
    Row(
        Modifier.fillMaxWidth().height(80.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            "ADB Manager",
            Modifier.padding(start = 15.dp, top = 10.dp),
            color = FluentTheme.colors.text.text.primary,
            style = FluentTheme.typography.body
        )

        Row {
            Button(
                onClick = {
                    onMinimize()
                },
            ) {
                Icon(
                    imageVector = Icons.Regular.Subtract,
                    contentDescription = "Subtract"
                )
            }
            Button(
                onClick = {
                    exitProcess(0)
                },
                hoverColor = hexToColor("#c42b1c")
            ) {
                Icon(
                    imageVector = Icons.Regular.Dismiss,
                    contentDescription = "Dismiss"
                )
            }
        }
    }
}

@Composable
private fun Button(
    onClick: () -> Unit,
    hoverColor: Color = Color.White.copy(alpha = 0.1f), // Fixed alpha parameter name
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    Box(
        modifier = Modifier
            .pointerHoverIcon(PointerIcon.Default)
            .background(if (isHovered) hoverColor else Color.Transparent) // Moved before clickable
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(15.dp)
    ) {
        content()
    }
}

