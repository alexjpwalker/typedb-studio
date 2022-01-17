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

package com.vaticle.typedb.studio.view.common.component

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.mouse.MouseScrollEvent
import androidx.compose.ui.input.mouse.MouseScrollOrientation
import androidx.compose.ui.input.mouse.MouseScrollUnit
import androidx.compose.ui.input.mouse.mouseScrollFilter
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import com.vaticle.typedb.studio.view.common.theme.Theme.toDP
import java.lang.Integer.min
import kotlin.math.floor

/**
 * A custom LazyColumn library -- a variant of Compose' native
 * [androidx.compose.foundation.lazy.LazyColumn]. This library is different from
 * that of Compose' in that it is much simpler and lightweight: every entry in
 * the column has the same, fixed height, and uses the same lambda to produced a
 * [androidx.compose.runtime.Composable]
 */
object LazyColumn {

    class ScrollState internal constructor(private val itemHeight: Dp, val itemCount: Int) {
        private val contentHeight: Dp = itemHeight * itemCount
        private var offset: Dp by mutableStateOf(0.dp); private set
        internal var height: Dp by mutableStateOf(0.dp); private set
        internal var firstVisibleOffset: Dp by mutableStateOf(0.dp)
        internal var firstVisibleIndex: Int by mutableStateOf(0)
        internal var lastVisibleIndex: Int by mutableStateOf(0)

        @OptIn(ExperimentalComposeUiApi::class)
        internal fun updateOffset(event: MouseScrollEvent): Boolean {
            if (event.delta !is MouseScrollUnit.Line || event.orientation != MouseScrollOrientation.Vertical) return false

            println("----------------------------")
            println("raw delta: ${event.delta}")
            val delta = itemHeight * (event.delta as MouseScrollUnit.Line).value * -1
            println("computed delta: ${delta}")
            val max = max(contentHeight - height, 0.dp)
            offset = (offset - delta).coerceIn(0.dp, max)
            println("offset: $offset")

            updateView()
            return true
        }

        internal fun updateHeight(newHeight: Dp) {
            height = newHeight
            updateView()
        }

        private fun updateView() {
            firstVisibleIndex = floor(offset.value / itemHeight.value).toInt()
            println("firstVisibleIndex: $firstVisibleIndex")
            firstVisibleOffset = offset - itemHeight * firstVisibleIndex
            println("firstVisibleOffset: $firstVisibleOffset")
            val visibleItems = floor((height.value + firstVisibleOffset.value) / itemHeight.value).toInt()
            println("visibleItems: $visibleItems")
            lastVisibleIndex = min(firstVisibleIndex + visibleItems, itemCount - 1)
            println("lastVisibleIndex: $lastVisibleIndex")
        }
    }

    data class State<T : Any> internal constructor(
        internal val items: List<T>,
        internal val scroller: ScrollState
    )

    fun createScrollState(itemHeight: Dp, itemCount: Int): ScrollState {
        return ScrollState(itemHeight, itemCount)
    }

    fun <T : Any> createState(items: List<T>, itemHeight: Dp): State<T> {
        return State(items, createScrollState(itemHeight, items.size))
    }

    fun <T : Any> createState(items: List<T>, scroller: ScrollState): State<T> {
        return State(items, scroller)
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @Composable
    fun <T : Any> Area(
        state: State<T>,
        modifier: Modifier = Modifier,
        horizontalAlignment: Alignment.Horizontal = Alignment.Start,
        itemFn: @Composable (item: T) -> Unit
    ) {
        val density = LocalDensity.current.density
        Box(modifier = modifier.clipToBounds()
            .onSizeChanged { state.scroller.updateHeight(toDP(it.height, density)) }
            .mouseScrollFilter { event, _ -> state.scroller.updateOffset(event) }) {
            Column(modifier = Modifier.offset(y = -state.scroller.firstVisibleOffset), horizontalAlignment = horizontalAlignment) {
                for (i in state.scroller.firstVisibleIndex..state.scroller.lastVisibleIndex) {
                    itemFn(state.items[i])
                }
            }
        }
    }
}