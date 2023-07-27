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

import androidx.compose.ui.geometry.Offset
import com.vaticle.typedb.client.api.TypeDBTransaction
import com.vaticle.typedb.client.api.answer.ConceptMap
import com.vaticle.typedb.client.api.answer.ConceptMap.Explainables
import com.vaticle.typedb.client.api.concept.Concept
import com.vaticle.typedb.client.api.concept.thing.Relation
import com.vaticle.typedb.client.api.concept.thing.Thing
import com.vaticle.typedb.client.api.concept.type.RoleType
import com.vaticle.typedb.client.api.concept.type.ThingType
import com.vaticle.typedb.client.api.concept.type.Type
import com.vaticle.typedb.client.api.logic.Explanation
import com.vaticle.typedb.client.common.exception.TypeDBClientException
import com.vaticle.typedb.studio.service.Service
import com.vaticle.typedb.studio.service.common.NotificationService
import com.vaticle.typedb.studio.service.common.util.Message.Visualiser.Companion.EXPLAIN_NOT_ENABLED
import com.vaticle.typedb.studio.service.common.util.Message.Visualiser.Companion.FULLY_EXPLAINED
import com.vaticle.typedb.studio.service.common.util.Message.Visualiser.Companion.UNEXPECTED_ERROR
import com.vaticle.typedb.studio.service.connection.TransactionState
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.withLock
import kotlinx.coroutines.CoroutineScope
import mu.KotlinLogging

class GraphBuilder(
    val graph: Graph, private val transactionState: TransactionState, val coroutines: CoroutineScope,
    val schema: Schema = Schema()
) {
    private val newThingVertices = ConcurrentHashMap<String, Vertex.Thing>()
    private val newTypeVertices = ConcurrentHashMap<String, Vertex.Type>()
    private val allThingVertices = ConcurrentHashMap<String, Vertex.Thing>()
    private val allTypeVertices = ConcurrentHashMap<String, Vertex.Type>()
    private val edges = ConcurrentLinkedQueue<Edge>()
    private val thingEdgeCandidates = ConcurrentHashMap<String, Collection<ThingEdgeCandidate>>()
    private val vertexExplainables = ConcurrentHashMap<Vertex.Thing, ConceptMap.Explainable>()
    private val vertexExplanations = ConcurrentLinkedQueue<Pair<Vertex.Thing, Explanation>>()
    private val hasEdgeExplainables = ConcurrentHashMap<Pair<Vertex.Thing, Vertex.Thing.Attribute>, ConceptMap.Explainable>()
    private val hasEdgeExplanations = ConcurrentLinkedQueue<Pair<Pair<Vertex.Thing, Vertex.Thing.Attribute>, Explanation>>()
    private val lock = ReentrantReadWriteLock(true)
    private val transactionID = transactionState.transaction?.hashCode()
    private val snapshotEnabled = transactionState.snapshot.value
    private val isInitialTransaction: Boolean
        get() = transactionState.transaction?.hashCode() == transactionID
    private val transactionSnapshot: TypeDBTransaction?
        get() = if (snapshotEnabled && isInitialTransaction) transactionState.transaction else null

    companion object {
        private val LOGGER = KotlinLogging.logger {}
        private val SUB = "SUB"
        private val OWNS = "OWNS"
    }

    fun loadConceptMap(conceptMap: ConceptMap, answerSource: AnswerSource = AnswerSource.Query) {
        val verticesByVarName = mutableMapOf<String, Vertex>()
        conceptMap.map().entries.forEach { (varName: String, concept: Concept) ->
            when {
                concept is Thing -> {
                    val (added, vertex) = when (answerSource) {
                        is AnswerSource.Query -> putVertexIfAbsent(concept)
                        is AnswerSource.Explanation -> putVertexAtPositionIfAbsent(concept, answerSource.position)
                    }
                    verticesByVarName[varName] = vertex
                    if (added) {
                        vertex as Vertex.Thing
                        if (transactionState.transaction?.options()?.explain()?.get() == true && concept.isInferred) {
                            addVertexExplainables(vertex, conceptMap.explainables(), varName)
                        }
                        if (answerSource is AnswerSource.Explanation) {
                            vertexExplanations += Pair(vertex, answerSource.explanation)
                        }
                    }
                }
                concept is ThingType && concept.isRoot -> { /* skip root thing types */
                }
                concept is ThingType -> {
                    verticesByVarName[varName] = putVertexIfAbsent(concept).vertex
                }
                concept is RoleType -> { /* skip role types */
                }
                else -> throw unsupportedEncodingException(concept)
            }
        }
        conceptMap.explainables().ownerships().entries.forEach { (varNames, explainable) ->
            val (ownerVarName, attributeVarName) = varNames.first() to varNames.second()
            val (ownerVertex, attributeVertex) = verticesByVarName.getValue(ownerVarName) to verticesByVarName.getValue(
                attributeVarName
            )
            if (ownerVertex !is Vertex.Thing) throw IllegalStateException("In an explainable ownership, the owner was not a Thing")
            if (attributeVertex !is Vertex.Thing.Attribute) throw IllegalStateException("In an explainable ownership, the attribute was not an Attribute")
            addHasEdgeExplainable(ownerVertex, attributeVertex, explainable)
        }
    }

    private fun putVertexIfAbsent(concept: Concept): PutVertexResult = when {
        concept is Thing -> putVertexIfAbsent(
            concept.iid, concept, newThingVertices, allThingVertices
        ) { Vertex.Thing.of(concept, graph) }
        concept is ThingType && !concept.isRoot -> putVertexIfAbsent(
            concept.label.name(), concept, newTypeVertices, allTypeVertices
        ) { Vertex.Type.of(concept, graph) }
        else -> throw unsupportedEncodingException(concept)
    }

    private fun putVertexAtPositionIfAbsent(thing: Thing, position: Offset): PutVertexResult = putVertexIfAbsent(
        thing.iid, thing, newThingVertices, allThingVertices
    ) { Vertex.Thing.of(thing, graph, position) }

    private fun <VERTEX : Vertex> putVertexIfAbsent(
        key: String, concept: Concept, newRecords: MutableMap<String, VERTEX>, allRecords: MutableMap<String, VERTEX>,
        vertexFn: () -> VERTEX
    ): PutVertexResult {
        var added = false
        val vertex = lock.readLock().withLock {
            val v = allRecords.computeIfAbsent(key) { added = true; vertexFn() }
            if (added) {
                newRecords[key] = v
                completeEdges(missingVertex = v)
                EdgeBuilder.of(concept, v, this, transactionState.transaction)?.build()
            }
            v
        }
        return PutVertexResult(added, vertex)
    }

    data class PutVertexResult(val added: Boolean, val vertex: Vertex)

    fun addEdge(edge: Edge) {
        lock.readLock().withLock { edges += edge }
    }

    fun addThingEdgeCandidate(edgeCandidate: ThingEdgeCandidate) {
        val key = when (edgeCandidate) {
            is ThingEdgeCandidate.Has -> edgeCandidate.targetIID
            is ThingEdgeCandidate.Isa -> edgeCandidate.targetLabel
        }
        thingEdgeCandidates.compute(key) { _, existing -> if (existing == null) listOf(edgeCandidate) else existing + edgeCandidate }
    }

    private fun completeEdges(missingVertex: Vertex) {
        if (missingVertex is Vertex.Thing) {
            val key = missingVertex.thing.iid
            thingEdgeCandidates.remove(key)?.let { candidates ->
                candidates.forEach { edges += it.toEdge(missingVertex) }
            }
        }
    }

    fun completeAllEdges() {
        // Since there is no protection against an edge candidate, and the vertex that completes it, being added
        // concurrently, we do a final sanity check once all vertices + edges have been loaded.
        lock.readLock().withLock {
            graph.thingVertices.values.forEach { completeEdges(it) }
            renderSchema()
        }
    }

    private fun renderSchema() {
        val rendered: MutableMap<Vertex, Set<Pair<String, Vertex.Type>>> = mutableMapOf()

        allTypeVertices.values.forEach {
            renderEdges(it.type, rendered)
        }
    }

    private fun renderEdges(type: Type, rendered: MutableMap<Vertex, Set<Pair<String, Vertex.Type>>>): Set<Pair<String, Vertex.Type>> {
        if (type.isRoot) return emptySet()

        val superType = type.asRemote(transactionState.transaction).supertype!!
        if (!allTypeVertices.containsKey(type.label.name())) return renderEdges(superType, rendered)

        val typeVertex = allTypeVertices[type.label.name()]!!
        if (rendered.containsKey(typeVertex)) return rendered[typeVertex]!!

        val edgesForTypeVertex = getSchemaEdges(typeVertex)
        val superTypeRendered = renderEdges(superType, rendered)
        edgesForTypeVertex
            .filter { !superTypeRendered.contains(it) }
            .forEach { pair -> renderEdge(typeVertex, pair) }
        rendered[typeVertex] = edgesForTypeVertex
        return edgesForTypeVertex
    }

    private fun renderEdge(vertex: Vertex.Type, pair: Pair<String, Vertex.Type>) {
        when (pair.first) {
            SUB -> addEdge(Edge.Sub(vertex, pair.second))
            OWNS -> addEdge(Edge.Owns(vertex, pair.second as Vertex.Type.Attribute))
            else -> addEdge(Edge.Plays(pair.second as Vertex.Type.Relation, vertex, pair.first))
        }
    }

    private fun getSchemaEdges(schemaVertex: Vertex.Type): Set<Pair<String, Vertex.Type>> {
        val pairs: MutableSet<Pair<String, Vertex.Type>> = mutableSetOf()
        val schemaVertexRemoteThingType = schemaVertex.type.asRemote(transactionState.transaction).asThingType()
        schemaVertexRemoteThingType.supertypes
            .filter {superType -> !superType.isRoot && !superType.equals(schemaVertex.type) && allTypeVertices.containsKey(superType.label.name()) }
            .forEach {superType -> pairs.add(Pair(SUB, allTypeVertices[superType.label.name()]!!))}
        schemaVertexRemoteThingType.owns
            .filter {attrType ->  allTypeVertices.containsKey(attrType.label.name())}
            .forEach {attrType -> pairs.add(Pair(OWNS, allTypeVertices[attrType.label.name()]!!))}
        schemaVertexRemoteThingType.plays
            .filter {plays -> allTypeVertices.containsKey(plays.label.scope().get())}
            .forEach {plays -> pairs.add(Pair(plays.label.name(), allTypeVertices[plays.label.scope().get()]!!))}
        return pairs
    }


    private fun addVertexExplainables(thingVertex: Vertex.Thing, explainables: Explainables, varName: String) {
        try {
            this.vertexExplainables.computeIfAbsent(thingVertex) {
                when (thingVertex) {
                    is Vertex.Thing.Relation -> explainables.relation(varName)
                    is Vertex.Thing.Attribute -> explainables.attribute(varName)
                    else -> throw IllegalStateException("Inferred Thing was neither a Relation nor an Attribute")
                }
            }
        } catch (_: TypeDBClientException) {
            // TODO: Currently we need to catch this exception because not every Inferred concept is
            //       Explainable. Once that bug is fixed, remove this catch statement.
            /* do nothing */
        }
    }

    private fun addHasEdgeExplainable(ownerVertex: Vertex.Thing, attributeVertex: Vertex.Thing.Attribute, explainable: ConceptMap.Explainable) {
        this.hasEdgeExplainables.putIfAbsent(Pair(ownerVertex, attributeVertex), explainable)
    }

    fun dumpTo(graph: Graph) {
        lock.writeLock().withLock {
            dumpVerticesTo(graph)
            dumpEdgesTo(graph)
            dumpExplainablesTo(graph)
            dumpExplanationStructureTo(graph)
        }
    }

    private fun dumpVerticesTo(graph: Graph) {
        newThingVertices.forEach { (iid, vertex) -> graph.putThingVertex(iid, vertex) }
        newTypeVertices.forEach { (label, vertex) -> graph.putTypeVertex(label, vertex) }
        newThingVertices.clear()
        newTypeVertices.clear()
    }

    private fun dumpEdgesTo(graph: Graph) {
        edges.forEach { graph.addEdge(it) }
        computeCurvedEdges(edges, graph)
        edges.clear()
    }

    private fun computeCurvedEdges(edges: Iterable<Edge>, graph: Graph) {
        val edgesBySource = graph.edges.groupBy { it.source }
        edges.forEach { edge ->
            val edgeBand = getEdgeBand(edge, edgesBySource)
            if (edgeBand.size > 1) edgeBand.forEach { graph.makeEdgeCurved(it) }
        }
    }

    private fun getEdgeBand(edge: Edge, allEdgesBySource: Map<Vertex, Collection<Edge>>): Collection<Edge> {
        // Grouping edges by source allows us to minimise the number of passes we do over the whole graph
        return (allEdgesBySource.getOrDefault(edge.source, listOf()).filter { it.target == edge.target }
                + allEdgesBySource.getOrDefault(edge.target, listOf()).filter { it.target == edge.source })
    }

    private fun dumpExplainablesTo(graph: Graph) {
        vertexExplainables.forEach { graph.reasoning.vertexExplainables.putIfAbsent(it.key, it.value) }
        vertexExplainables.clear()
        hasEdgeExplainables.forEach { graph.reasoning.hasEdgeExplainables.putIfAbsent(it.key, it.value) }
        hasEdgeExplainables.clear()
    }

    private fun dumpExplanationStructureTo(graph: Graph) {
        graph.reasoning.addObjectExplanations(vertexExplanations, hasEdgeExplanations)
        vertexExplanations.clear()
    }

    fun tryExplainThing(vertex: Vertex.Thing) {
        val canExplain = transactionSnapshot?.options()?.explain()?.get() ?: false
        if (!canExplain) {
            Service.notification.userWarning(LOGGER, EXPLAIN_NOT_ENABLED)
        } else {
            NotificationService.launchCompletableFuture(Service.notification, LOGGER) {
                val iterator = graph.reasoning.vertexExplanationIterators[vertex]
                    ?: runExplainQueryForVertex(vertex).also { graph.reasoning.vertexExplanationIterators[vertex] = it }
                fetchNextVertexExplanation(vertex, iterator)
            }.exceptionally { e -> Service.notification.systemError(LOGGER, e, UNEXPECTED_ERROR) }
        }
    }

    fun tryExplainOwnership(edge: Edge.Has) {
        val canExplain = transactionSnapshot?.options()?.explain()?.get() ?: false
        if (!canExplain) {
            Service.notification.userWarning(LOGGER, EXPLAIN_NOT_ENABLED)
        } else {
            NotificationService.launchCompletableFuture(Service.notification, LOGGER) {
                val iterator = graph.reasoning.hasEdgeExplanationIterators[edge]
                    ?: runExplainQueryForHasEdge(edge).also { graph.reasoning.hasEdgeExplanationIterators[edge] = it }
                fetchNextHasEdgeExplanation(edge, iterator)
            }.exceptionally { e -> Service.notification.systemError(LOGGER, e, UNEXPECTED_ERROR) }
        }
    }

    private fun runExplainQueryForVertex(vertex: Vertex.Thing): Iterator<Explanation> {
        val explainable = graph.reasoning.vertexExplainables[vertex] ?: throw IllegalStateException("Not explainable")
        return transactionSnapshot?.query()?.explain(explainable)?.iterator() ?: Collections.emptyIterator()
    }

    private fun runExplainQueryForHasEdge(edge: Edge.Has): Iterator<Explanation> {
        val explainable = graph.reasoning.hasEdgeExplainables[Pair(edge.source, edge.target)] ?: throw IllegalStateException("Not explainable")
        return transactionSnapshot?.query()?.explain(explainable)?.iterator() ?: Collections.emptyIterator()
    }

    private fun fetchNextVertexExplanation(vertex: Vertex.Thing, iterator: Iterator<Explanation>) {
        if (iterator.hasNext()) {
            val explanation = iterator.next()
            vertexExplanations += vertex to explanation
            loadConceptMap(explanation.condition(), AnswerSource.Explanation(explanation, vertex.geometry.position))
        } else Service.notification.info(LOGGER, FULLY_EXPLAINED)
    }

    private fun fetchNextHasEdgeExplanation(edge: Edge.Has, iterator: Iterator<Explanation>) {
        if (iterator.hasNext()) {
            val explanation = iterator.next()
            hasEdgeExplanations += Pair(edge.source, edge.target) to explanation
            loadConceptMap(explanation.condition(), AnswerSource.Explanation(explanation, edge.geometry.labelPosition))
        } else Service.notification.info(LOGGER, FULLY_EXPLAINED)
    }

    fun unsupportedEncodingException(concept: Concept): IllegalStateException {
        return IllegalStateException("[$concept]'s encoding is not supported by AnswerLoader")
    }

    sealed class AnswerSource {
        object Query : AnswerSource()
        class Explanation(val explanation: com.vaticle.typedb.client.api.logic.Explanation, val position: Offset) : AnswerSource()
    }

    sealed class ThingEdgeCandidate {

        interface Inferrable {
            val isInferred: Boolean
        }

        abstract fun toEdge(vertex: Vertex): Edge

        // Thing edges
        class Has(
            val source: Vertex.Thing, val targetIID: String, override val isInferred: Boolean = false
        ) : ThingEdgeCandidate(), Inferrable {

            override fun toEdge(vertex: Vertex) = Edge.Has(source, vertex as Vertex.Thing.Attribute, isInferred)
        }

        // Thing-to-type edges
        class Isa(val source: Vertex.Thing, val targetLabel: String) : ThingEdgeCandidate() {
            override fun toEdge(vertex: Vertex) = Edge.Isa(source, vertex as Vertex.Type)
        }
    }

    class Schema(val typeAttributeOwnershipMap: ConcurrentMap<String, Boolean> = ConcurrentHashMap())

    sealed class EdgeBuilder(val graphBuilder: GraphBuilder) {

        abstract fun build()

        companion object {
            fun of(concept: Concept, vertex: Vertex, graphBuilder: GraphBuilder, transaction: TypeDBTransaction?): EdgeBuilder? {
                return when (concept) {
                    is com.vaticle.typedb.client.api.concept.thing.Thing -> {
                        Thing(concept, vertex as Vertex.Thing, transaction, graphBuilder)
                    }
                    is ThingType -> {
                        null
                    }
                    else -> throw graphBuilder.unsupportedEncodingException(concept)
                }
            }
        }

        class Thing(
            val thing: com.vaticle.typedb.client.api.concept.thing.Thing,
            private val thingVertex: Vertex.Thing,
            private val transaction: TypeDBTransaction?,
            ctx: GraphBuilder,
            ) : EdgeBuilder(ctx) {
            private val remoteThing get() = transaction?.let { thing.asRemote(it) }

            override fun build() {
                loadIsaEdge()
                loadHasEdges()
                if (thing is Relation) loadRoleplayerEdgesAndVertices()
            }

            private fun loadIsaEdge() {
                thing.type.let { type ->
                    val typeVertex = graphBuilder.allTypeVertices[type.label.name()]
                    if (typeVertex != null) graphBuilder.addEdge(Edge.Isa(thingVertex, typeVertex))
                    else graphBuilder.addThingEdgeCandidate(ThingEdgeCandidate.Isa(thingVertex, type.label.name()))
                }
            }

            private fun loadHasEdges() {
                // construct TypeQL query so that reasoning can run
                // test for ability to own attributes, to ensure query will not throw during type inference
                if (!canOwnAttributes()) return
                graphBuilder.apply {
                    remoteThing?.has?.forEach {
                        val attributeVertex = graphBuilder.allThingVertices[it.iid] as? Vertex.Thing.Attribute
                        val ownershipIsInferred = it.isInferred
                        if (attributeVertex != null) {
                            addEdge(Edge.Has(thingVertex, attributeVertex, ownershipIsInferred))
                        } else {
                            addThingEdgeCandidate(ThingEdgeCandidate.Has(thingVertex, it.iid, ownershipIsInferred))
                        }
                    }
                }
            }

            private fun canOwnAttributes(): Boolean {
                val typeLabel = thing.type.label.name()
                return graphBuilder.schema.typeAttributeOwnershipMap.getOrPut(typeLabel) {
                    // non-atomic update as Concept API call is idempotent and cheaper than locking the map
                    transaction?.let {
                        thing.type.asRemote(it).owns.findAny().isPresent
                    } ?: false
                }
            }

            private fun loadRoleplayerEdgesAndVertices() {
                graphBuilder.apply {
                    remoteThing?.asRelation()?.playersByRoleType?.entries?.forEach { (roleType, roleplayers) ->
                        roleplayers.forEach { rp ->
                            val result = putVertexIfAbsent(rp.iid, rp, newThingVertices, allThingVertices) {
                                Vertex.Thing.of(rp, graph)
                            }
                            addEdge(
                                Edge.Roleplayer(
                                    thingVertex as Vertex.Thing.Relation, result.vertex as Vertex.Thing,
                                    roleType.label.name(), thing.isInferred
                                )
                            )
                        }
                    }
                }
            }

            private fun attributeIsExplainable(attributeVarName: String, conceptMap: ConceptMap): Boolean {
                return attributeVarName in conceptMap.explainables().attributes().keys
            }

            private fun ownershipIsExplainable(attributeVarName: String, conceptMap: ConceptMap): Boolean {
                return attributeVarName in conceptMap.explainables().ownerships().keys.map { it.second() }
            }
        }
    }
}
