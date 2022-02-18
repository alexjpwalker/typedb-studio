/*
 * Copyright (C) 2021 Vaticle
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 */

package com.vaticle.typedb.studio

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Scaffold
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement.Maximized
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.vaticle.typedb.studio.appearance.StudioTheme
import com.vaticle.typedb.studio.appearance.defaultVisualiserTheme
import com.vaticle.typedb.studio.connection.ConnectionScreen
import com.vaticle.typedb.studio.routing.ConnectionRoute
import com.vaticle.typedb.studio.routing.Router
import com.vaticle.typedb.studio.routing.WorkspaceRoute
import com.vaticle.typedb.studio.storage.AppData
import com.vaticle.typedb.studio.ui.elements.StudioSnackbarHost
import com.vaticle.typedb.studio.workspace.WorkspaceScreen
import mu.KotlinLogging.logger
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlin.io.path.Path

@Composable
fun Studio(onCloseRequest: () -> Unit) {
    val snackbarHostState = rememberScaffoldState().snackbarHostState
    val pixelDensity = LocalDensity.current.density
    var titleBarHeight by remember { mutableStateOf(0F) }

    // TODO: we want no title bar, by passing undecorated = true, but it seems to cause intermittent crashes on startup
    //       (see #40). Test if they occur when running the distribution, or only with bazel run :studio-bin-*
    Window(title = "TypeDB Studio", onCloseRequest = onCloseRequest, state = rememberWindowState(Maximized)) {
        StudioTheme {
            Scaffold(modifier = Modifier.fillMaxSize()
                .border(BorderStroke(1.dp, SolidColor(StudioTheme.colors.uiElementBorder)))
                .onGloballyPositioned { coordinates ->
                    // used to translate from screen coordinates to window coordinates in the visualiser
                    titleBarHeight = window.height - coordinates.size.height / pixelDensity
                }) {
                when (val routeData = Router.currentRoute) {
                    is ConnectionRoute -> ConnectionScreen.Main(routeData, snackbarHostState)
                    is WorkspaceRoute -> WorkspaceScreen(
                        routeData, defaultVisualiserTheme(), window, titleBarHeight, snackbarHostState
                    )
                }
                Box(modifier = Modifier.fillMaxHeight(), contentAlignment = Alignment.BottomCenter) {
                    StudioSnackbarHost(snackbarHostState)
                }
            }
        }
    }
}

fun main() {
    AppData().initialise()
    val log = logger {}
    println(File(".").listFiles()!!.map { it.path })
    val zis = ZipInputStream(FileInputStream("./studio.jar"))
    var nextEntry = zis.nextEntry
    while (nextEntry != null) {
        println(nextEntry)
        nextEntry = zis.nextEntry
    }

    application {
        fun onCloseRequest() {
            log.debug { "Closing TypeDB Studio" }
            exitApplication() // TODO: I think this is the wrong behaviour on MacOS
        }
        Studio(::onCloseRequest)
    }
}
