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

package com.vaticle.typedb.studio.view.output

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.Dp
import com.vaticle.typedb.studio.state.resource.Resource
import com.vaticle.typedb.studio.state.runner.TransactionRunner
import com.vaticle.typedb.studio.view.common.Label
import com.vaticle.typedb.studio.view.common.component.Form
import com.vaticle.typedb.studio.view.common.component.Form.ButtonArgs
import com.vaticle.typedb.studio.view.common.component.Frame
import com.vaticle.typedb.studio.view.common.component.Icon
import com.vaticle.typedb.studio.view.common.component.Separator
import com.vaticle.typedb.studio.view.common.component.Tabs
import com.vaticle.typedb.studio.view.common.theme.Theme
import com.vaticle.typedb.studio.view.common.theme.Theme.PANEL_BAR_HEIGHT
import com.vaticle.typedb.studio.view.common.theme.Theme.PANEL_BAR_SPACING
import kotlinx.coroutines.CoroutineScope

object RunOutputArea {

    const val DEFAULT_OPEN = false

    class State(
        internal val resource: Resource,
        private val paneState: Frame.PaneState,
        coroutineScope: CoroutineScope
    ) {

        private var unfreezeSize: Dp? by mutableStateOf(null)
        internal var isOpen: Boolean by mutableStateOf(DEFAULT_OPEN)
        internal val tabsState = Tabs.State<TransactionRunner>(coroutineScope)
        internal var density: Float
            get() = tabsState.density
            set(value) {
                tabsState.density = value
            }

        init {
            resource.runner.onRegister { toggle(true) }
        }

        internal fun toggle() {
            toggle(!isOpen)
        }

        private fun toggle(isOpen: Boolean) {
            this.isOpen = isOpen
            mayUpdatePaneState()
        }

        private fun mayUpdatePaneState() {
            if (!isOpen) {
                unfreezeSize = paneState.size
                paneState.freeze(PANEL_BAR_HEIGHT)
            } else if (paneState.isFrozen) {
                paneState.unfreeze(unfreezeSize ?: (paneState.frameState.maxSize / 2))
            }
        }
    }

    @Composable
    fun Layout(state: State) {
        state.density = LocalDensity.current.density
        Column(Modifier.fillMaxSize()) {
            Bar(state)
            if (state.isOpen) {
                Separator.Horizontal()
                RunOutputGroup(Modifier.fillMaxSize())
            }
        }
    }

    @Composable
    private fun Bar(state: State) {
        Row(
            Modifier.fillMaxWidth().height(PANEL_BAR_HEIGHT),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(Modifier.width(PANEL_BAR_SPACING))
            Form.Text(value = Label.RUN + ":")
            Spacer(Modifier.width(PANEL_BAR_SPACING))
            RunOutputGroupTabs(state, Modifier.weight(1f))
            ToggleButton(state)
        }
    }

    @Composable
    private fun RunOutputGroupTabs(state: State, modifier: Modifier) {
        val runnerMgr = state.resource.runner
        fun runnerName(runner: TransactionRunner): AnnotatedString {
            return AnnotatedString(state.resource.name + "::" + Label.RUN.lowercase() + runnerMgr.numberOf(runner))
        }
        Box(modifier) {
            Tabs.Layout(
                state = state.tabsState,
                tabs = runnerMgr.runners,
                labelFn = { runnerName(it) },
                isActiveFn = { runnerMgr.isActive(it) },
                onClick = { runnerMgr.activate(it) },
                closeButtonFn = { ButtonArgs(icon = Icon.Code.XMARK) { runnerMgr.delete(it) } },
                trailingTabButtonFn = {
                    ButtonArgs(
                        icon = Icon.Code.THUMBTACK,
                        color = { Theme.colors.icon.copy(if (runnerMgr.isSaved(it)) 1f else 0.3f) },
                        hoverColor = { Theme.colors.icon },
                        disabledColor = { Theme.colors.icon },
                        enabled = !runnerMgr.isSaved(it)
                    ) { if (!runnerMgr.isSaved(it)) runnerMgr.save(it) }
                }
            )
        }
    }

    @Composable
    private fun ToggleButton(state: State) {
        Form.IconButton(
            icon = if (state.isOpen) Icon.Code.CHEVRON_DOWN else Icon.Code.CHEVRON_UP,
            onClick = { state.toggle() },
            modifier = Modifier.size(PANEL_BAR_HEIGHT),
            bgColor = Color.Transparent,
            rounded = false,
        )
    }

    @Composable
    private fun RunOutputGroup(modifier: Modifier) {
        Column(modifier) {
            Output(Modifier.fillMaxWidth().weight(1f))
            Separator.Horizontal()
            OutputTabs(Modifier.fillMaxWidth().height(PANEL_BAR_HEIGHT))
        }
    }

    @Composable
    private fun Output(modifier: Modifier) {
        Row(modifier.background(Theme.colors.background2)) {

        }
    }

    @Composable
    private fun OutputTabs(modifier: Modifier) {
        Row(modifier) {

        }
    }
}