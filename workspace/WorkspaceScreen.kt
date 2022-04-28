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

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.zIndex
import com.vaticle.force.graph.force.LinkForce
import com.vaticle.typedb.client.common.exception.TypeDBClientException
import com.vaticle.typedb.studio.appearance.StudioTheme
import com.vaticle.typedb.studio.appearance.VisualiserTheme
import com.vaticle.typedb.studio.data.EdgeEncoding.*
import com.vaticle.typedb.studio.data.QueryResponseStream
import com.vaticle.typedb.studio.data.emptyQueryResponseStream
import com.vaticle.typedb.studio.diagnostics.rememberErrorReporter
import com.vaticle.typedb.studio.routing.Router
import com.vaticle.typedb.studio.routing.WorkspaceRoute
import com.vaticle.typedb.studio.ui.elements.Icon
import com.vaticle.typedb.studio.ui.elements.IconSize.*
import com.vaticle.typedb.studio.ui.elements.StudioIcon
import com.vaticle.typedb.studio.ui.elements.StudioTab
import com.vaticle.typedb.studio.ui.elements.StudioTabs
import com.vaticle.typedb.studio.ui.elements.TabHighlight
import com.vaticle.typedb.studio.visualiser.EdgeState
import com.vaticle.typedb.studio.visualiser.TypeDBForceSimulation
import com.vaticle.typedb.studio.visualiser.TypeDBVisualiser
import com.vaticle.typedb.studio.visualiser.VertexState
import com.vaticle.typedb.studio.visualiser.runSimulation
import mu.KotlinLogging.logger
import java.awt.FileDialog
import java.io.File
import java.io.FileNotFoundException
import java.io.FilenameFilter
import java.io.IOException
import java.io.PrintWriter
import java.nio.file.Files
import java.nio.file.Path
import kotlin.math.pow

@Composable
fun WorkspaceScreen(routeData: WorkspaceRoute, visualiserTheme: VisualiserTheme, window: ComposeWindow, snackbarHostState: SnackbarHostState) {
    val snackbarCoroutineScope = rememberCoroutineScope()
    val log = remember { logger {} }
    val errorReporter = rememberErrorReporter(log, snackbarHostState, snackbarCoroutineScope)

    Column(Modifier.fillMaxSize()) {
        // TODO: combine these into meaningful groups of state objects
        var queryResponseStream: QueryResponseStream by remember { mutableStateOf(emptyQueryResponseStream()) }
        val forceSimulation = remember { TypeDBForceSimulation() }
        var visualiserFrameID by remember { mutableStateOf(0) }
        var visualiserWorldOffset by remember { mutableStateOf(Offset.Zero) }
        var visualiserSize by mutableStateOf(Size.Zero)
        var visualiserScale by remember { mutableStateOf(1F) }
        var selectedVertex: VertexState? by remember { mutableStateOf(null) }
        val selectedVertexNetwork: MutableList<VertexState> = remember { mutableStateListOf() }
        var queryStartTimeNanos: Long? by remember { mutableStateOf(null) }
        var querySettings by remember { mutableStateOf(QuerySettings()) }
        val queryTabs: MutableList<QueryTabState> = remember { mutableStateListOf(
            QueryTabState(title = "Query1.tql", query = "match \$x sub thing;\n" +
                    "offset 0;\n" +
                    "limit 1000;\n")
        ) }
        var queryTabNextIndex by remember { mutableStateOf(2) }
        var activeQueryTabIndex by remember { mutableStateOf(0) }
        val temporarilyFrozenNodes = remember { mutableStateListOf<VertexState>() }

        val db = routeData.db
        val activeQueryTab: QueryTabState? = queryTabs.getOrNull(activeQueryTabIndex)

        // TODO: with this many callbacks, the view becomes unreadable - create a VM (ToolbarViewModel?)
        fun switchWorkspace(dbName: String) {
            try {
                db.client.closeAllSessions() // TODO: switch workspaces
            } catch (e: Exception) {
                if (e is TypeDBClientException) errorReporter.reportOddBehaviour(e)
                else errorReporter.reportIDEError(e)
            }
        }

        fun openOpenQueryDialog() {
            try {
                FileDialog(window, "Open", FileDialog.LOAD).apply {
                    isMultipleMode = false

                    // TODO: investigate file extension filtering on Windows
//                // Windows
//                file = "*.tql"
                    // TODO: this seems to force the user to ONLY load .tql files and doesn't give them any other option
//                // Mac, Linux
//                filenameFilter = FilenameFilter { _, name -> name?.endsWith(".tql") ?: false }

                    isVisible = true
                    file?.let { filename ->
                        val path = Path.of(directory, filename)
                        try {
                            val fileContent: String = Files.readString(Path.of(directory, filename))
                            queryTabs += QueryTabState(title = filename, query = fileContent, file = path.toFile())
                        } catch (e: Exception) {
                            when (e) {
                                is IOException, is SecurityException -> {
                                    errorReporter.reportUserError(e) { "The file couldn't be opened" }
                                }
                                else -> throw e
                            }
                        }
                        activeQueryTabIndex = queryTabs.size - 1
                    }
                }
            } catch (e: Exception) {
                errorReporter.reportIDEError(e)
            }
        }

        fun openSaveQueryDialog() {
            if (activeQueryTab == null) {
                log.warn { "openSaveQueryDialog: activeQueryTab is null!" }
                return
            }

            try {
                FileDialog(window, "Save", FileDialog.SAVE).apply {
                    isMultipleMode = false
                    val queryTabFile = activeQueryTab.file
                    if (queryTabFile == null) {
                        file = activeQueryTab.title
                    } else {
                        file = queryTabFile.name
                        directory = queryTabFile.parentFile.absolutePath
                    }
                    // TODO: test if this has any effect on Windows
                    filenameFilter = FilenameFilter { _, name -> name?.endsWith(".tql") ?: false }
                    isVisible = true
                    file?.let { filename ->
                        try {
                            PrintWriter(Path.of(directory, filename).toFile()).use { printWriter ->
                                printWriter.print(activeQueryTab.query)
                                activeQueryTab.title = filename
                                activeQueryTab.file = File(filename)
                            }
                        } catch (e: Exception) {
                            when (e) {
                                is FileNotFoundException, is SecurityException -> {
                                    errorReporter.reportUserError(e) { "The file couldn't be saved" }
                                }
                                else -> throw e
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                errorReporter.reportIDEError(e)
            }
        }

        fun runQuery() {
            if (activeQueryTab == null) {
                log.warn { "runQuery: activeQueryTab is null!" }
                return
            }

            val query = activeQueryTab.query
            try {
                forceSimulation.init()
                visualiserFrameID = 0
                queryResponseStream = db.matchQuery(query, querySettings.enableReasoning)
            } catch (e: Exception) {
                errorReporter.reportIDEError(e)
                return
            }
            visualiserWorldOffset = visualiserSize.center
            queryStartTimeNanos = System.nanoTime()
            selectedVertex = null
            selectedVertexNetwork.clear()
        }

        fun logout() {
            try {
                db.client.closeAllSessions()
            } catch (e: Exception) {
                errorReporter.reportOddBehaviour(e)
            } finally {
                Router.navigateTo(routeData.connectionForm.toRoute())
            }
        }

        Toolbar(dbName = db.name, onOpen = { openOpenQueryDialog() }, onSave = { openSaveQueryDialog() }, onRun = { runQuery() }, onLogout = { logout() })

        Row(modifier = Modifier.fillMaxWidth().height(1.dp).background(StudioTheme.colors.uiElementBorder)) {}

        Row(modifier = Modifier.weight(1f)) {
            var showQuerySettingsPanel by remember { mutableStateOf(true) }
            var showConceptPanel by remember { mutableStateOf(true) }

            Column(modifier = Modifier.fillMaxHeight().weight(1F)) {
                fun onSelectVertex(vertex: VertexState?) {
                    selectedVertex = vertex
                    selectedVertexNetwork.clear()
                    if (vertex != null) {
                        val verticesToAdd = mutableSetOf(vertex)
                        val verticesByID = forceSimulation.data.vertices.associateBy { it.id }
                        forceSimulation.data.edges.forEach { edge: EdgeState ->
                            val vertexToAdd = when (vertex.id) {
                                edge.sourceID -> verticesByID[edge.targetID]
                                edge.targetID -> verticesByID[edge.sourceID]
                                else -> null
                            }
                            if (vertexToAdd != null) verticesToAdd += vertexToAdd
                        }
                        selectedVertexNetwork += verticesToAdd
                    }
                }

                StudioTabs(modifier = Modifier.fillMaxWidth().height(26.dp)) {
                    fun addNewQueryTab() {
                        queryTabs += QueryTabState(title = "Query${queryTabNextIndex}.tql", query = "\n")
                        queryTabNextIndex++
                    }

                    queryTabs.mapIndexed { index: Int, queryTab: QueryTabState ->
                        val selected = index == activeQueryTabIndex
                        StudioTab(text = queryTab.title, selected = selected, highlight = TabHighlight.BOTTOM,
                            showCloseButton = true, modifier = Modifier.clickable {
                                activeQueryTabIndex = index.coerceAtMost(queryTabs.size - 1)
                            }, onClose = {
                                queryTabs.removeAt(index)
                                if (queryTabs.isEmpty()) { addNewQueryTab() }
                                activeQueryTabIndex = index.coerceAtMost(queryTabs.size - 1)
                            })
                    }
                    Spacer(Modifier.width(6.dp))
                    StudioIcon(icon = Icon.Plus, size = Size14, modifier = Modifier.clickable {
                        addNewQueryTab()
                        activeQueryTabIndex = queryTabs.size - 1
                    })
                }

                Row(modifier = Modifier.fillMaxWidth().height(1.dp).background(StudioTheme.colors.uiElementBorder)) {}

                if (activeQueryTab != null) {
                    CodeEditor(
                        code = activeQueryTab.query, editorID = activeQueryTab.editorID,
                        onChange = { value -> activeQueryTab.query = value },
                        font = StudioTheme.typography.codeEditorSwing,
                        modifier = Modifier.fillMaxWidth().height(112.dp).background(StudioTheme.colors.editorBackground),
                        snackbarHostState = snackbarHostState
                    )
                }

                Row(modifier = Modifier.fillMaxWidth().height(1.dp).background(StudioTheme.colors.uiElementBorder)) {}

                PanelHeader(modifier = Modifier.fillMaxWidth()) {
                    StudioTabs(modifier = Modifier.weight(1f)) {
                        Spacer(Modifier.width(8.dp))
                        Text("Output", style = StudioTheme.typography.body2)
                        Spacer(Modifier.width(8.dp))
                    }
                }

                Row(modifier = Modifier.weight(1F)) {
                    val pixelDensity = LocalDensity.current.density

                    // TODO: this drag logic is too complex - should be extracted out into a VM (maybe TypeDBForceSimulation)
                    fun onVertexDragStart(vertex: VertexState) {
                        vertex.xFixed = true
                        vertex.yFixed = true

                        forceSimulation.removeChargeForce()
                        forceSimulation.removeCenterForce()
                        forceSimulation.removeXForce()
                        forceSimulation.removeYForce()
                        forceSimulation.alpha(0.25).alphaDecay(0.0)

                        forceSimulation.removeLinkForce()
                        if (forceSimulation.data.edges.any { it.targetID == vertex.id && it.encoding == ROLEPLAYER }) {
                            val attributeEdges = forceSimulation.data.edges
                                .filter { it.sourceID == vertex.id && it.encoding == HAS }
                            val attributeVertices = attributeEdges.map { it.target }
                            val roleplayerEdges = forceSimulation.data.edges
                                .filter { it.targetID == vertex.id && it.encoding == ROLEPLAYER }
                                .map { it.sourceID }
                                .flatMap { relationNodeID -> forceSimulation.data.edges
                                    .filter { it.sourceID == relationNodeID && it.encoding == ROLEPLAYER }
                                }
                            val relationVertices = roleplayerEdges.map { it.source }
                            val roleplayerVertices = roleplayerEdges.map { it.target }
                            val vertices = (attributeVertices + relationVertices + roleplayerVertices).toSet()
                            val edges = attributeEdges + roleplayerEdges
                            val nodesToFreeze = roleplayerVertices.filter { it != vertex && !it.isXFixed }
                            nodesToFreeze.forEach { it.freeze() }
                            temporarilyFrozenNodes += nodesToFreeze
                            forceSimulation.linkForce = forceSimulation.forces().add(LinkForce(vertices, edges, 90.0, 0.25))
                        }
                    }

                    fun onVertexDragMove(vertex: VertexState, position: Offset) {
                        vertex.let {
                            it.x(position.x.toDouble())
                            it.y(position.y.toDouble())
                        }
                    }

                    fun onVertexDragEnd() {
                        forceSimulation.removeLinkForce()
                        forceSimulation.alphaDecay(1 - forceSimulation.alphaMin().pow(1.0 / 300))
                        temporarilyFrozenNodes.forEach { it.unfreeze() }
                        temporarilyFrozenNodes.clear()
                    }

                    TypeDBVisualiser(modifier = Modifier.fillMaxSize().onGloballyPositioned { visualiserSize = it.size.toSize() / pixelDensity },
                        vertices = forceSimulation.data.vertices, edges = forceSimulation.data.edges,
                        hyperedges = forceSimulation.data.hyperedges, frameID = visualiserFrameID,
                        vertexExplanations = forceSimulation.data.vertexExplanations, theme = visualiserTheme,
                        worldOffset = visualiserWorldOffset, onWorldOffsetChange = { visualiserWorldOffset += it },
                        scale = visualiserScale, onZoom = { value -> visualiserScale = (visualiserScale + value).coerceIn(0.001f, 10f) },
                        explain = { vertex -> db.explainConcept(vertex.id) }, selectedVertex = selectedVertex,
                        onSelectVertex = ::onSelectVertex, selectedVertexNetwork = selectedVertexNetwork,
                        onVertexDragStart = ::onVertexDragStart, onVertexDragMove = ::onVertexDragMove,
                        onVertexDragEnd = ::onVertexDragEnd)
                }

                Row(modifier = Modifier.fillMaxWidth().height(1.dp).background(StudioTheme.colors.uiElementBorder)) {}
            }

            if (showQuerySettingsPanel || showConceptPanel) {
                Column(modifier = Modifier.fillMaxHeight().width(1.dp).background(StudioTheme.colors.uiElementBorder)) {}

                Column(modifier = Modifier.fillMaxHeight().requiredWidth(285.dp).background(StudioTheme.colors.background)) {
                    if (showQuerySettingsPanel) {
                        QuerySettingsPanel(settings = querySettings, onSettingsChange = { querySettings = it },
                            onCollapse = { showQuerySettingsPanel = false }, modifier = Modifier.weight(1f))
                    }
                    if (showConceptPanel) {
                        ConceptPanel(vertex = selectedVertex, onCollapse = { showConceptPanel = false },
                            modifier = Modifier.weight(2f))
                    }
                }
            }

            Column(modifier = Modifier.fillMaxHeight().width(1.dp).background(StudioTheme.colors.uiElementBorder).zIndex(20f)) {}

            Column(modifier = Modifier.width(20.dp)) {
                // TODO: We really need to make StudioTabs work - these hardcoded-height boxes are a dirty hack
                //       that will break if the font size is customised
                Box(Modifier.height(76.dp).background(if (showQuerySettingsPanel) StudioTheme.colors.backgroundHighlight else StudioTheme.colors.background)
                    .clickable { showQuerySettingsPanel = !showQuerySettingsPanel }) {
                    StudioIcon(Icon.Cog, modifier = Modifier.offset(x = 4.dp, y = 8.dp))
                    Text("Settings", maxLines = 1, style = StudioTheme.typography.body2, modifier = Modifier
                        .offset(y = 40.dp)
                        .requiredWidth(IntrinsicSize.Max)
                        .rotate(90f))
                }
                Box(Modifier.height(76.dp).background(if (showConceptPanel) StudioTheme.colors.backgroundHighlight else StudioTheme.colors.background)
                    .clickable { showConceptPanel = !showConceptPanel }) {
                    StudioIcon(Icon.SearchAround, modifier = Modifier.offset(x = 4.dp, y = 8.dp))
                    Text("Concept", maxLines = 1, style = StudioTheme.typography.body2, modifier = Modifier
                        .offset(y = 40.dp)
                        .requiredWidth(IntrinsicSize.Max)
                        .rotate(90f))
                }
                Row(modifier = Modifier.weight(1f)) {}
                Row(modifier = Modifier.fillMaxWidth().height(1.dp).background(StudioTheme.colors.uiElementBorder)) {}
            }
        }

        StatusBar(queryResponseStream = queryResponseStream, visualiserScale = visualiserScale,
            vertexCount = forceSimulation.data.vertices.size, edgeCount = forceSimulation.data.edges.size,
            queryStartTimeNanos = queryStartTimeNanos)

        LaunchedEffect(key1 = queryResponseStream) {
            runSimulation(forceSimulation, queryResponseStream, { visualiserFrameID++ }, snackbarHostState, snackbarCoroutineScope)
        }
    }
}
