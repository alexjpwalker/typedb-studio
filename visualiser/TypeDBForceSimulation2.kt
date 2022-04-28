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
import org.gephi.graph.api.GraphModel
import org.gephi.graph.api.Node
import org.gephi.layout.plugin.force.StepDisplacement
import org.gephi.layout.plugin.force.yifanHu.YifanHuLayout
import org.gephi.layout.plugin.forceAtlas2.ForceAtlas2
import org.gephi.layout.plugin.forceAtlas2.ForceAtlas2Builder
import org.gephi.layout.spi.Layout
import java.util.concurrent.atomic.AtomicBoolean

class TypeDBForceSimulation2 {
    val data = GraphState()
    private val graphModel: GraphModel = GraphModel.Factory.newInstance()
    private val layout: Layout = ForceAtlas2(ForceAtlas2Builder()).apply {
        setGraphModel(graphModel)
        gravity = 0.0
//        scalingRatio = 1.0
        isStrongGravityMode = false
    }
    private var sun = createSun()
//    private val layout: Layout = YifanHuLayout(null, StepDisplacement(1f)).apply {
//        setGraphModel(graphModel)
//    }

    var lastTickStartNanos: Long = 0
    var isStarted = false
    val wasInitAlgoCalled = AtomicBoolean(false)
//    private val nextHyperedgeNodeID = AtomicInteger(-1)
//    private val vertices: MutableList<VertexState> = mutableListOf()
//    val hyperedgeNodes: MutableList<Vertex> = mutableListOf()
//    val hyperedgeNodesByID: MutableMap<Int, Vertex> = mutableMapOf()
//    var linkForce: LinkForce? = null
//    var chargeForce: ManyBodyForce? = null
//    var centerForce: CenterForce? = null
//    var xForce: XForce? = null
//    var yForce: YForce? = null

    fun init() {
        clear()
        sun = createSun()
        graphModel.directedGraph.addNode(sun)
        if (wasInitAlgoCalled.compareAndSet(false, true)) {
            println("created sun")
//            println("nodes count when initAlgo called: ${vertices.size}")
            layout.initAlgo()
            layout.resetPropertiesValues()
        }
//        centerForce = forces().addCenterForce(vertexList, 0.0, 0.0)
//        forces().addCollideForce(vertexList, 80.0)
//        chargeForce = forces().addManyBodyForce(vertexList, -100.0)
//        xForce = forces().addXForce(vertexList, 0.0, 0.05)
//        yForce = forces().addYForce(vertexList, 0.0, 0.05)
//        forces().addCollideForce(hyperedgeNodes, 40.0)
//        alpha(1.0)
//        alphaTarget(0.0)
//        alphaMin(0.01)
        isStarted = true
        lastTickStartNanos = 0
    }

    fun isEmpty(): Boolean {
        return data.vertices.isEmpty()
    }

    private fun createSun(): Node {
        return graphModel.factory().newNode("0").apply { this.setSize(1f); }
    }

    @Synchronized
    fun tick() {
        if (layout.canAlgo()) layout.goAlgo() else println("fa2.canAlgo returned 'false'!")
        val verticesByID = data.vertices.associateBy { it.id.toString() }
        graphModel.directedGraph.nodes.forEach {
            if (it.id == "0") return@forEach
            val vertex = verticesByID[it.id] ?: throw IllegalStateException("Received bad simulation data: no vertex found with ID ${it.id}!")
            vertex.position = Offset(if (it.x().isNaN()) 0f else it.x() * 8f, if (it.y().isNaN()) 0f else it.y() * 8f)
        }
//        data.hyperedges.forEach {
//            val hyperedgeNode = hyperedgeNodesByID[it.hyperedgeNodeID]
//                ?: throw IllegalStateException("Received bad simulation data: no hyperedge node found with ID ${it.hyperedgeNodeID}!")
//            it.position = Offset(hyperedgeNode.x().toFloat(), hyperedgeNode.y().toFloat())
//        }
    }

    @Synchronized
    fun clear() {
        isStarted = false
        data.clear()
        graphModel.directedGraph.clear()
//        hyperedgeNodes.clear()
//        nextHyperedgeNodeID.set(-1)
        wasInitAlgoCalled.set(false)
    }

    @Synchronized
    fun addVertices(vertices: List<VertexState>) {
        if (vertices.isEmpty()) return
        data.vertices += vertices
        val newNodes = vertices.map { graphModel.factory().newNode(it.id.toString()).apply { this.setSize(1f); } }
        graphModel.directedGraph.addAllNodes(newNodes)
        graphModel.directedGraph.addAllEdges(newNodes.map { graphModel.factory().newEdge(it, sun, 1, Math.random() * 5f + 1f, true) })
        if (wasInitAlgoCalled.compareAndSet(false, true)) {
//            println("nodes count when initAlgo called: ${vertices.size}")
            layout.initAlgo()
            layout.resetPropertiesValues()
        }
    }

    @Synchronized
    fun addEdges(edges: List<EdgeState>) {
        if (edges.isEmpty()) return
        data.edges += edges
        graphModel.directedGraph.addAllEdges(edges.map {
            val source = graphModel.directedGraph.getNode(it.sourceID.toString())
            val target = graphModel.directedGraph.getNode(it.targetID.toString())
            graphModel.factory().newEdge(source, target, 1, 20.0, true)
        })
//        val vertexCollection = vertices as Collection<Vertex>
//        val edgeCollection = data.edges as Collection<Edge>
//        linkForce?.let { existing -> forces().remove(existing) }
//        linkForce = forces().addLinkForce(vertexCollection, edgeCollection, 90.0, 0.5)
//        chargeForce?.let { existing -> forces().remove(existing) }
//        chargeForce = forces().addManyBodyForce(vertexCollection, (-500.0 - data.vertices.size / 2) * data.edges.size / (data.vertices.size + 1))

//        val edgesBySource = data.edges.groupBy { it.sourceID }
//        edges.forEach { edge ->
//            val edgeBand = (edgesBySource.getOrDefault(edge.sourceID, listOf()).filter { it.targetID == edge.targetID }
//                + edgesBySource.getOrDefault(edge.targetID, listOf()).filter { it.targetID == edge.sourceID })
//            if (edgeBand.size > 1) {
//                edgeBand.forEach(::addEdgeBandMember)
//            }
//        }
    }

    fun removeChargeForce() {
//        chargeForce?.let { forces().remove(it); chargeForce = null }
    }

    fun removeCenterForce() {
//        centerForce?.let { forces().remove(it); centerForce = null }
    }

    fun removeXForce() {
//        xForce?.let { forces().remove(it); xForce = null }
    }

    fun removeYForce() {
//        yForce?.let { forces().remove(it); yForce = null }
    }

    fun removeLinkForce() {
//        linkForce?.let { forces().remove(it); linkForce = null }
    }

    private fun addEdgeBandMember(edge: EdgeState) {
//        val edgeMidpoint = edgeMidpoint(edge)
//        val hyperedgeNodeID = nextHyperedgeNodeID.getAndAdd(-1)
//        BasicVertex(edgeMidpoint.x, edgeMidpoint.y).let {
//            placeNode(it)
//            hyperedgeNodes += it
//            hyperedgeNodesByID[hyperedgeNodeID] = it
//            val placementOffset = RandomEffects.jiggle()
//            forces().addXForce(listOf(it), { edgeMidpoint(edge).x + placementOffset }, 0.35)
//            forces().addYForce(listOf(it), { edgeMidpoint(edge).y + placementOffset }, 0.35)
//        }
//        data.hyperedges += HyperedgeState(edge.id, hyperedgeNodeID)
    }

//    private fun edgeMidpoint(edge: EdgeState): Point {
//        val node1 = edge.source
//        val node2 = edge.target
//        return Point((node1.x() + node2.x()) / 2, (node1.y() + node2.y()) / 2)
//    }

    fun addVertexExplanations(vertexExplanations: List<VertexExplanationState>) {
        if (vertexExplanations.isEmpty()) return
        data.vertexExplanations += vertexExplanations
    }
}

//data class Point(val x: Double, val y: Double)
