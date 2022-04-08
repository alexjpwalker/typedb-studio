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

package com.vaticle.typedb.studio.visualiser

import androidx.compose.ui.geometry.Offset
import com.vaticle.force.graph.api.Link
import com.vaticle.force.graph.api.Node
import com.vaticle.force.graph.force.CenterForce
import com.vaticle.force.graph.force.LinkForce
import com.vaticle.force.graph.force.ManyBodyForce
import com.vaticle.force.graph.force.XForce
import com.vaticle.force.graph.force.YForce
import com.vaticle.force.graph.impl.BasicNode
import com.vaticle.force.graph.impl.BasicSimulation
import com.vaticle.force.graph.util.RandomEffects
import java.util.concurrent.atomic.AtomicInteger

class TypeDBForceSimulation(val data: GraphState = GraphState()) : BasicSimulation() {
    var lastTickStartNanos: Long = 0
    var isStarted = false
    private val nextHyperedgeNodeID = AtomicInteger(-1)
    private val vertexNodes: MutableCollection<VertexState> = mutableListOf()
    val hyperedgeNodes: MutableMap<Int, Node> = mutableMapOf()
    var linkForce: LinkForce? = null
    var chargeForce: ManyBodyForce? = null
    var centerForce: CenterForce? = null
    var xForce: XForce? = null
    var yForce: YForce? = null

    fun init() {
        clear()
        placeNodes(data.vertices as Collection<Node>?)
        CenterForce(nodes(), 0.0, 0.0).let {
            centerForce = it
            addForce(it)
        }
        addCollideForce(vertexNodes as Collection<Node>?, 80.0)
        ManyBodyForce(vertexNodes as Collection<Node>?, -100.0).let {
            chargeForce = it
            addForce(it)
        }
        XForce(vertexNodes as Collection<Node>?, 0.0, 0.05).let {
            xForce = it
            addForce(it)
        }
        YForce(vertexNodes as Collection<Node>?, 0.0, 0.05).let {
            yForce = it
            addForce(it)
        }
        addCollideForce(hyperedgeNodes.values as Collection<Node>?, 40.0)
        alpha(1.0)
        alphaTarget(0.0)
        alphaMin(0.01)

        isStarted = true
        lastTickStartNanos = 0
    }

    fun isEmpty(): Boolean {
        return nodes().isEmpty()
    }

    override fun tick() {
        super.tick()
        val verticesByID: Map<Int, VertexState> = data.vertices.associateBy { it.id }
        data.edges.forEach {
            it.sourcePosition = verticesByID[it.sourceID]!!.position
            it.targetPosition = verticesByID[it.targetID]!!.position
        }
        data.hyperedges.forEach {
            val hyperedgeNode = hyperedgeNodes[it.hyperedgeNodeID]
                ?: throw IllegalStateException("Received bad simulation data: no hyperedge node found with ID ${it.hyperedgeNodeID}!")
            it.position = Offset(hyperedgeNode.x().toFloat(), hyperedgeNode.y().toFloat())
        }
    }

    override fun clear() {
        super.clear()
        isStarted = false
        data.clear()
        vertexNodes.clear()
        hyperedgeNodes.clear()
        nextHyperedgeNodeID.set(-1)
    }

    fun addVertices(vertices: List<VertexState>) {
        if (vertices.isEmpty()) return
        vertexNodes += vertices
        data.vertices += vertices
        placeNodes(vertices)
    }

    fun addEdges(edges: List<EdgeState>) {
        if (edges.isEmpty()) return
        data.edges += edges
        LinkForce(vertexNodes as Collection<Node>?, data.edges as Collection<Link>?, 90.0, 0.5).let {
            linkForce?.let { existing -> removeForce(existing) }
            linkForce = it
            addForce(it)
        }
        ManyBodyForce(vertexNodes as Collection<Node>?, -600.0 * data.edges.size / (data.vertices.size + 1)).let {
            chargeForce?.let { existing -> removeForce(existing) }
            chargeForce = it
            addForce(it)
        }

        val edgesBySource = data.edges.groupBy { it.sourceID }
        edges.forEach { edge ->
            val edgeBand = (edgesBySource.getOrDefault(edge.sourceID, listOf()).filter { it.targetID == edge.targetID }
                + edgesBySource.getOrDefault(edge.targetID, listOf()).filter { it.targetID == edge.sourceID })
            if (edgeBand.size > 1) {
                edgeBand.forEach(::addEdgeBandMember)
            }
        }
    }

    private fun addEdgeBandMember(edge: EdgeState) {
        if (edge.id !in hyperedgeNodes) {
            val edgeMidpoint = edgeMidpoint(edge)
            val hyperedgeNodeID = nextHyperedgeNodeID.getAndAdd(-1)
            BasicNode(edgeMidpoint.x, edgeMidpoint.y).let {
                placeNode(it)
                hyperedgeNodes[hyperedgeNodeID] = it
                val placementOffset = RandomEffects.jiggle()
                addForce(XForce(listOf(it), { edgeMidpoint(edge).x + placementOffset }, 0.35))
                addForce(YForce(listOf(it), { edgeMidpoint(edge).y + placementOffset }, 0.35))
            }
            data.hyperedges += HyperedgeState(edge.id, hyperedgeNodeID)
        }
    }

    private fun edgeMidpoint(edge: EdgeState): Point {
        val node1 = edge.source
        val node2 = edge.target
        return Point((node1.x() + node2.x()) / 2, (node1.y() + node2.y()) / 2)
    }

    fun addVertexExplanations(vertexExplanations: List<VertexExplanationState>) {
        if (vertexExplanations.isEmpty()) return
        data.vertexExplanations += vertexExplanations
    }
}

data class Point(val x: Double, val y: Double)
