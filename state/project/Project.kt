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

package com.vaticle.typedb.studio.state.project

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.vaticle.typedb.studio.state.notification.Error
import com.vaticle.typedb.studio.state.notification.Message.Project.Companion.PATH_NOT_DIRECTORY
import com.vaticle.typedb.studio.state.notification.Message.Project.Companion.PATH_NOT_EXIST
import com.vaticle.typedb.studio.state.notification.Message.Project.Companion.PATH_NOT_READABLE
import com.vaticle.typedb.studio.state.notification.Notifier
import mu.KotlinLogging
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.isReadable
import kotlin.io.path.notExists

class Project(private val notifier: Notifier) {

    companion object {
        private val LOGGER = KotlinLogging.logger {}
    }

    var directory: Path? by mutableStateOf(null)
    var pastDirectories: Set<Path> by mutableStateOf(emptySet()) // TODO: initialise from user data
    var showWindow: Boolean by mutableStateOf(false)
    val windowTitle: String? get() = directory?.let { "${it.fileName}" }

    fun toggleWindow() {
        showWindow = !showWindow
    }

    fun tryOpen(newDirectory: String) {
        val path = Path.of(newDirectory)
        if (!path.exists()) notifier.userError(Error.fromUser(PATH_NOT_EXIST, newDirectory), LOGGER)
        else if (!path.isReadable()) notifier.userError(Error.fromUser(PATH_NOT_READABLE, newDirectory), LOGGER)
        else if (!path.isDirectory()) notifier.userError(Error.fromUser(PATH_NOT_DIRECTORY, newDirectory), LOGGER)
        else {
            directory = path
            pastDirectories = pastDirectories + path
            showWindow = false
        }
    }
}