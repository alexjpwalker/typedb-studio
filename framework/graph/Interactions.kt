/*
 * Copyright (C) 2022 Vaticle
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

package com.vaticle.typedb.studio.framework.graph

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import com.vaticle.typedb.client.api.logic.Explanation

class Interactions constructor(val graphArea: GraphArea) {

    var pointerPosition: Offset? by mutableStateOf(null)
    var hoveredVertex: Vertex? by mutableStateOf(null)
    var hoveredObjectExplanations: Set<Explanation> by mutableStateOf(emptySet())
    var hoveredEdge: Edge? by mutableStateOf(null)
    val hoveredObjectChecker = HoveredObjectChecker(graphArea)

    private var _focusedVertex: Vertex? by mutableStateOf(null)
    var focusedVertex: Vertex?
        get() = _focusedVertex
        set(value) {
            rebuildFocusedVertexNetwork(value)
            _focusedVertex = value
        }
    var focusedVertexNetwork: Set<Vertex> by mutableStateOf(emptySet())

    var draggedVertex: Vertex? by mutableStateOf(null)

    // Logically, if the vertex is dragged, it should also be hovered; however, this is not always true
    // because the vertex takes some time to "catch up" to the pointer. So check both conditions.
    val Vertex.isHovered get() = this == hoveredVertex || this == draggedVertex

    val Vertex.isBackground get() = focusedVertex != null && this !in focusedVertexNetwork

    val Edge.isBackground get() = focusedVertex != null && source != focusedVertex && target != focusedVertex

    fun rebuildFocusedVertexNetwork(focusedVertex: Vertex? = _focusedVertex) {
        focusedVertexNetwork = if (focusedVertex != null) {
            val linkedVertices = graphArea.graph.edges.mapNotNull { edge ->
                when (focusedVertex) {
                    edge.source -> edge.target
                    edge.target -> edge.source
                    else -> null
                }
            }
            setOf(focusedVertex) + linkedVertices
        } else emptySet()
    }

    class HoveredObjectChecker constructor(private val graphArea: GraphArea) : BackgroundTask(runIntervalMs = 33) {

        private val interactions get() = graphArea.interactions

        override fun canRun(): Boolean {
            return interactions.pointerPosition != null
        }

        override fun run() {
            var hasChanges = false
            val hoveredVertex = interactions.pointerPosition?.let { graphArea.viewport.findVertexAt(it, interactions) }
            if (interactions.hoveredVertex != hoveredVertex) {
                hasChanges = true
                interactions.hoveredVertex = hoveredVertex
            }
            val hoveredEdge = interactions.pointerPosition?.let { graphArea.viewport.findEdgeAt(it, interactions) }
            if (interactions.hoveredEdge != hoveredEdge) {
                hasChanges = true
                interactions.hoveredEdge = hoveredEdge
            }
            if (hasChanges) {
                interactions.hoveredObjectExplanations = when {
                    hoveredVertex == null && hoveredEdge == null -> emptySet()
                    hoveredVertex != null -> graphArea.graph.reasoning.explanationsByVertex[hoveredVertex] ?: emptySet()
                    hoveredEdge != null -> {
                        val set1 = graphArea.graph.reasoning.explanationsByEdge[hoveredEdge.source to hoveredEdge.target] ?: emptySet()
                        val set2 = graphArea.graph.reasoning.explanationsByVertex[hoveredEdge.source] ?: emptySet()
                        val set3 = graphArea.graph.reasoning.explanationsByVertex[hoveredEdge.target] ?: emptySet()
                        set1 union (set2 intersect set3)
                    }
                    else -> emptySet() /* should not be reachable */
                }
            }
        }
    }
}
