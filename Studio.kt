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

package com.vaticle.typedb.studio

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Scaffold
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement.Maximized
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.vaticle.typedb.studio.appearance.StudioTheme
import com.vaticle.typedb.studio.appearance.defaultVisualiserTheme
import com.vaticle.typedb.studio.connection.ConnectionScreen
import com.vaticle.typedb.studio.routing.ConnectionRoute
import com.vaticle.typedb.studio.routing.Router
import com.vaticle.typedb.studio.routing.WorkspaceRoute
import com.vaticle.typedb.studio.storage.AppData
import com.vaticle.typedb.studio.ui.elements.StudioSnackbarHost
import com.vaticle.typedb.studio.workspace.WorkspaceScreen
import mu.KotlinLogging.logger
//import org.gephi.graph.api.GraphModel
//import org.gephi.layout.plugin.force.StepDisplacement
//import org.gephi.layout.plugin.force.yifanHu.YifanHuLayout
//import org.gephi.layout.plugin.forceAtlas2.ForceAtlas2
//import org.gephi.layout.plugin.forceAtlas2.ForceAtlas2Builder

@Composable
fun Studio(onCloseRequest: () -> Unit) {
    val snackbarHostState = rememberScaffoldState().snackbarHostState
    val pixelDensity = LocalDensity.current.density
    var titleBarHeight by remember { mutableStateOf(0F) }

    // TODO: we want no title bar, by passing undecorated = true, but it seems to cause intermittent crashes on startup
    //       (see #40). Test if they occur when running the distribution, or only with bazel run :studio-bin-*
    Window(title = "TypeDB Studio", onCloseRequest = onCloseRequest, state = rememberWindowState(Maximized)) {
        StudioTheme {
            Scaffold(modifier = Modifier.fillMaxSize()
                .border(BorderStroke(1.dp, SolidColor(StudioTheme.colors.uiElementBorder)))
                .onGloballyPositioned { coordinates ->
                    // used to translate from screen coordinates to window coordinates in the visualiser
                    titleBarHeight = window.height - coordinates.size.height / pixelDensity
                }) {
                when (val routeData = Router.currentRoute) {
                    is ConnectionRoute -> ConnectionScreen.Main(routeData, snackbarHostState)
                    is WorkspaceRoute -> WorkspaceScreen(
                        routeData, defaultVisualiserTheme(), window, snackbarHostState
                    )
                }
                Box(modifier = Modifier.fillMaxHeight(), contentAlignment = Alignment.BottomCenter) {
                    StudioSnackbarHost(snackbarHostState)
                }
            }
        }
    }
}

fun main() {
    AppData().initialise()
    val log = logger {}

    application {
        fun onCloseRequest() {
            log.debug { "Closing TypeDB Studio" }
            exitApplication() // TODO: I think this is the wrong behaviour on MacOS
        }
        Studio(::onCloseRequest)
    }

//    val graphModel: GraphModel = GraphModel.Factory.newInstance()
//    val builder = ForceAtlas2Builder()
//    val layout = ForceAtlas2(builder)
//    layout.setGraphModel(graphModel)
//    println(Runtime.getRuntime().availableProcessors())
//    println(layout.threadsCount)
//    val layout = YifanHuLayout(null, StepDisplacement(1f)).apply {
//        stepRatio = 0.99f
//        optimalDistance = 200f
//        barnesHutTheta = 1f
//    }
//
//    for (i in 0..3) { graphModel.directedGraph.addNode(graphModel.factory().newNode("n$i").apply { label = "person$i" }) }
//
//    graphModel.directedGraph.addEdge(graphModel.factory().newEdge(graphModel.directedGraph.getNode("n0"), graphModel.directedGraph.getNode("n1"), 1, true))
//    graphModel.directedGraph.addEdge(graphModel.factory().newEdge(graphModel.directedGraph.getNode("n0"), graphModel.directedGraph.getNode("n2"), 1, true))
//    graphModel.directedGraph.addEdge(graphModel.factory().newEdge(graphModel.directedGraph.getNode("n1"), graphModel.directedGraph.getNode("n2"), 1, true))
//
//    layout.setGraphModel(graphModel)
//
//    layout.initAlgo()
//    layout.resetPropertiesValues()
//    layout.stepRatio = 0.99f
//    layout.optimalDistance = 80f
//    layout.barnesHutTheta = 1f
//
//    var i = 0
//    while (i < 3000 && layout.canAlgo()) {
//        if (i < 3) { graphModel.directedGraph.addNode(graphModel.factory().newNode("n$i").apply { label = "person$i" }) }
//        if (i == 3) {
//            graphModel.directedGraph.addEdge(graphModel.factory().newEdge(graphModel.directedGraph.getNode("n0"), graphModel.directedGraph.getNode("n1"), 1, true))
//            graphModel.directedGraph.addEdge(graphModel.factory().newEdge(graphModel.directedGraph.getNode("n0"), graphModel.directedGraph.getNode("n2"), 1, true))
//            graphModel.directedGraph.addEdge(graphModel.factory().newEdge(graphModel.directedGraph.getNode("n1"), graphModel.directedGraph.getNode("n2"), 1, true))
//        }
//        if (i == 0 || i == 3) {
//            layout.initAlgo()
//            layout.resetPropertiesValues()
//            layout.optimalDistance = 80f
//        }
//        layout.goAlgo()
//        println("iteration $i: " + graphModel.directedGraph.nodes.map { "{ label: ${it.label}, x: ${it.x()}, y: ${it.y()} }" })
//        i++
//    }
}
