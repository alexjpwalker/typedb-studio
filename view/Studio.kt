﻿/*
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

package com.vaticle.typedb.studio.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberWindowState
import com.vaticle.typedb.common.collection.Either
import com.vaticle.typedb.studio.state.GlobalState
import com.vaticle.typedb.studio.state.common.Message
import com.vaticle.typedb.studio.state.config.UserDataDirectory
import com.vaticle.typedb.studio.view.browser.BrowserArea
import com.vaticle.typedb.studio.view.common.Label
import com.vaticle.typedb.studio.view.common.component.Form.Text
import com.vaticle.typedb.studio.view.common.component.Form.TextSelectable
import com.vaticle.typedb.studio.view.common.component.Frame
import com.vaticle.typedb.studio.view.common.component.Separator
import com.vaticle.typedb.studio.view.common.theme.Theme
import com.vaticle.typedb.studio.view.dialog.ConfirmationDialog
import com.vaticle.typedb.studio.view.dialog.ConnectionDialog
import com.vaticle.typedb.studio.view.dialog.ProjectDialog
import com.vaticle.typedb.studio.view.page.PageArea
import javax.swing.UIManager
import kotlin.system.exitProcess
import mu.KotlinLogging
import java.io.File
import java.io.FileInputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipInputStream
import kotlin.io.path.exists

object Studio {

    private val ERROR_WINDOW_WIDTH: Dp = 1000.dp
    private val ERROR_WINDOW_HEIGHT: Dp = 610.dp
    private val mainWindowTitle: String
        get() = "${Label.TYPEDB_STUDIO}${GlobalState.project.current?.let { " — ${it.directory.name}" } ?: ""}"

    private val LOGGER = KotlinLogging.logger {}

    @JvmStatic
    fun main(args: Array<String>) {
        try {
//            println(Path.of(".").toFile().listFiles().map { it.path })
//            println(Files.readString(Path.of("./MANIFEST")))
            println(File(".").listFiles()!!.map { it.path })
            val jarPath = if (Path.of("./studio.jar").exists()) "./studio.jar" else "C:/users/alex/_bazel_alex/grsqfgsz/execroot/vaticle_typedb_studio/bazel-out/x64_windows-fastbuild/bin/studio.jar"
            val zis = ZipInputStream(FileInputStream(jarPath))
            var nextEntry = zis.nextEntry
            while (nextEntry != null) {
                println(nextEntry)
                nextEntry = zis.nextEntry
            }
            //println(Files.readString(Path.of("./MANIFEST")))
            setConfigurations()
            Message.loadClasses()
            UserDataDirectory.initialise()
            application { MainWindow(it) }
        } catch (exception: Exception) {
            application { ErrorWindow(exception, it) }
        } finally {
            LOGGER.debug { Label.CLOSING_TYPEDB_STUDIO }
            exitProcess(0)
        }
    }

    private fun setConfigurations() {
        // Enable anti-aliasing
        System.setProperty("awt.useSystemAAFontSettings", "on")
        System.setProperty("swing.aatext", "true")
        // Enable FileDialog to select "directories" on MacOS
        System.setProperty("apple.awt.fileDialogForDirectories", "true");
        // Enable native Windows UI style
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()) // Set UI style for Windows
    }

    private fun application(window: @Composable (onExit: () -> Unit) -> Unit) {
        androidx.compose.ui.window.application {
            Theme.Material {
                // TODO: we don't want to call exitApplication() onCloseRequest for MacOS
                window { exitApplication() }
            }
        }
    }

    @Composable
    private fun MainWindow(onClose: () -> Unit) {
        // TODO: we want no title bar, by passing undecorated=true, but it seems to cause intermittent crashes on startup
        //       (see #40). Test if they occur when running the distribution, or only with bazel run :studio-bin-*
        Window(
            title = mainWindowTitle,
            onCloseRequest = { onClose() },
            state = rememberWindowState(WindowPlacement.Maximized)
        ) {
            Column(modifier = Modifier.fillMaxSize().background(Theme.colors.background)) {
                Toolbar.Layout()
                Separator.Horizontal()
                Frame.Row(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    separator = Frame.SeparatorArgs(Separator.WEIGHT),
                    Frame.Pane(
                        id = BrowserArea.javaClass.name,
                        initSize = Either.first(BrowserArea.WIDTH),
                        minSize = BrowserArea.MIN_WIDTH
                    ) { BrowserArea.Layout(it) },
                    Frame.Pane(
                        id = PageArea.javaClass.name,
                        initSize = Either.second(1f),
                        minSize = PageArea.MIN_WIDTH
                    ) { PageArea.Layout() }
                )
                Separator.Horizontal()
                StatusBar.Layout()
            }
            NotificationArea.Layout()
            if (GlobalState.confirmation.dialog.isOpen) ConfirmationDialog.Layout()
            if (GlobalState.connection.connectServerDialog.isOpen) ConnectionDialog.ConnectServer()
            if (GlobalState.project.createItemDialog.isOpen) ProjectDialog.CreateProjectItem()
            if (GlobalState.project.openProjectDialog.isOpen) ProjectDialog.OpenProject()
            if (GlobalState.project.renameItemDialog.isOpen) ProjectDialog.RenameProjectItem()
            if (GlobalState.project.saveFileDialog.isOpen) ProjectDialog.SaveFile(window)
        }
    }

    @Composable
    private fun ErrorWindow(exception: Exception, onClose: () -> Unit) {
        Window(
            title = Label.TYPEDB_STUDIO_APPLICATION_ERROR,
            onCloseRequest = { onClose() },
            state = rememberWindowState(
                placement = WindowPlacement.Floating,
                position = WindowPosition.Aligned(Alignment.Center),
                size = DpSize(ERROR_WINDOW_WIDTH, ERROR_WINDOW_HEIGHT),
            )
        ) {
            Column(modifier = Modifier.fillMaxSize().background(Theme.colors.background).padding(5.dp)) {
                val rowVerticalAlignment = Alignment.Top
                val rowModifier = Modifier.padding(5.dp)
                val labelModifier = Modifier.width(40.dp)
                val labelStyle = Theme.typography.body1.copy(fontWeight = FontWeight.Bold)
                val contentColor = Theme.colors.error2
                Row(verticalAlignment = rowVerticalAlignment, modifier = rowModifier) {
                    Text(value = "${Label.TITLE}:", modifier = labelModifier, textStyle = labelStyle)
                    exception.message?.let { TextSelectable(value = it, color = contentColor) }
                }
                Row(verticalAlignment = rowVerticalAlignment, modifier = rowModifier) {
                    Text(value = "${Label.TRACE}:", modifier = labelModifier, textStyle = labelStyle)
                    TextSelectable(value = exception.stackTraceToString(), color = contentColor)
                }
            }
        }
    }
}
