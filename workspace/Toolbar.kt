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

package com.vaticle.typedb.studio.workspace

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.vaticle.typedb.studio.appearance.StudioTheme
import com.vaticle.typedb.studio.ui.elements.Icon
import com.vaticle.typedb.studio.ui.elements.IconSize.*
import com.vaticle.typedb.studio.ui.elements.StudioDatabaseIcon
import com.vaticle.typedb.studio.ui.elements.StudioIcon

@Composable
fun Toolbar(modifier: Modifier = Modifier, dbName: String, onOpen: () -> Unit, onSave: () -> Unit, onRun: () -> Unit, onLogout: () -> Unit) {
    Row(modifier.height(28.dp), verticalAlignment = Alignment.CenterVertically) {
        Spacer(Modifier.width(8.dp))
        StudioDatabaseIcon()
        Spacer(Modifier.width(4.dp))
        Text(dbName, style = StudioTheme.typography.body2)
        Spacer(Modifier.width(8.dp))

        Spacer(Modifier.width(10.dp))
        StudioIcon(Icon.FolderOpen, size = Size14, modifier = Modifier.clickable { onOpen() })

        Spacer(Modifier.width(12.dp))
        StudioIcon(Icon.FloppyDisk, modifier = Modifier.clickable { onSave() })

        Spacer(Modifier.width(10.dp))
        StudioIcon(Icon.Play, Color(0xFF499C54), size = Size18, modifier = Modifier.clickable { onRun() })

        Spacer(Modifier.weight(1F))

        StudioIcon(Icon.LogOut, modifier = Modifier.clickable { onLogout() })
        Spacer(Modifier.width(12.dp))
    }
}
