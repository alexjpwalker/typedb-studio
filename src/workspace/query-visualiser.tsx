import { ipcRenderer, IpcRendererEvent } from "electron";
import moment from "moment";
import React from "react";
import { ConceptData, ConceptMapData, MatchQueryRequest, MatchQueryResponsePart } from "../ipc/event-args";
import { databaseState, themeState } from "../state/state";
import { studioStyles } from "../styles/studio-styles";
import { StudioTheme } from "../styles/theme";
import { TypeDBVisualiserData } from "../typedb-visualiser";
import TypeDBVisualiser from "../typedb-visualiser/react/TypeDBVisualiser";
import { uuidv4 } from "../util/uuid";
import { workspaceStyles } from "./workspace-styles";

interface GraphElementIDRegistry {
    nextID: number;
    types: {[label: string]: number};
    things: {[label: string]: number};
}

export interface QueryVisualiserProps {
    db: string;
    query: string;
    theme: StudioTheme;
}

type GraphNode = ConceptData & {nodeID: number};

export const QueryVisualiser: React.FC<QueryVisualiserProps> = ({db, query, theme}) => {
    const classes = workspaceStyles({ theme });
    const [rawAnswers, setRawAnswers] = React.useState<ConceptMapData[]>(null);
    const [visualiserData, setVisualiserData] = React.useState<TypeDBVisualiserData.Graph>(null);
    const [graphElementIDs, setGraphElementIDs] = React.useState<GraphElementIDRegistry>(null);

    const runQuery = () => {
        const req: MatchQueryRequest = { db, query };
        ipcRenderer.send("match-query-request", req);
        // setPrincipalStatus("Running Match query...");
        // setQueryRunning(true);
        // setQueryStartTime(Date.now());
        // setQueryRunTime("00:00.000");
        // setRenderRunTime(null);
        // setQueryCancelled(false);
        // setAnswerGraph({ simulationID: null, vertices: [], edges: [] });
        setVisualiserData({ simulationID: null, vertices: [], edges: [] });
        setGraphElementIDs({ nextID: 1, things: {}, types: {} });
        setRawAnswers([]);
        // setAnswerTable(null);
        // addLogEntry(code);
    };

    React.useEffect(() => {
        if (query) runQuery();
    }, [query]);

    React.useEffect(() => {
        const onReceiveMatchQueryResponsePart = (_event: IpcRendererEvent, res: MatchQueryResponsePart) => {
            // TODO: Concurrent responses may produce odd behaviour - can we correlate the event in the response
            //  to the one we sent in the request somehow?
            // if (queryCancelled) return;

            if (res.done) {
                // setPrincipalStatus("Ready");
                // setQueryRunning(false);
                // setTimeQuery(true);
                // setQueryEndTime(Date.now());
            }

            // setRenderRunTime("<<in progress>>");
            if (res.success) {
                rawAnswers.push(...res.answers);
                setRawAnswers(rawAnswers);
                // const answerCountString = `${rawAnswers.length} answer${rawAnswers.length !== 1 ? "s" : ""}`;
                // setQueryResult(answerCountString);
                // if (res.done) addLogEntry(answerCountString);
                const simulationID = visualiserData?.simulationID || uuidv4();
                const vertices: TypeDBVisualiserData.Vertex[] = visualiserData?.vertices || [];
                const edges: TypeDBVisualiserData.Edge[] = visualiserData?.edges || [];

                for (const conceptMap of res.answers) {
                    for (const varName in conceptMap) {
                        if (!conceptMap.hasOwnProperty(varName)) continue;
                        const concept = conceptMap[varName] as GraphNode;

                        if (concept.iid) {
                            const thingNodeID = graphElementIDs.things[concept.iid];
                            if (thingNodeID == null) {
                                concept.nodeID = graphElementIDs.nextID;
                                graphElementIDs.things[concept.iid] = graphElementIDs.nextID;
                            } else {
                                concept.nodeID = thingNodeID;
                                continue;
                            }
                        } else {
                            const typeNodeID = graphElementIDs.types[concept.label];
                            if (typeNodeID == null) {
                                concept.nodeID = graphElementIDs.nextID;
                                graphElementIDs.types[concept.label] = graphElementIDs.nextID;
                            } else {
                                concept.nodeID = typeNodeID;
                                continue;
                            }
                        }

                        const label = (concept.value != null
                            ? `${concept.type}:${concept.value instanceof Date ? moment(concept.value).format("DD-MM-YY HH:mm:ss") : concept.value.toString()}`
                            : (concept.label || concept.type)).slice(0, ["relation", "relationType"].includes(concept.encoding) ? 11 : 13);

                        vertices.push({
                            id: graphElementIDs.nextID,
                            width: ["relationType", "relation"].includes(concept.encoding) ? 120 : 110,
                            height: ["relationType", "relation"].includes(concept.encoding) ? 60 : 40,
                            label,
                            encoding: concept.encoding,
                        });
                        graphElementIDs.nextID++;
                    }
                }

                for (const conceptMap of rawAnswers) {
                    for (const varName in conceptMap) {
                        if (!conceptMap.hasOwnProperty(varName)) continue;
                        const concept = conceptMap[varName] as GraphNode;

                        if (concept.playsTypes) {
                            for (const roleType of concept.playsTypes) {
                                const relationTypeNodeID = graphElementIDs.types[roleType.relation];
                                if (relationTypeNodeID != null) {
                                    edges.push({ id: graphElementIDs.nextID, source: relationTypeNodeID, target: concept.nodeID, label: roleType.role });
                                    graphElementIDs.nextID++;
                                }
                            }
                        }

                        if (concept.ownsLabels) {
                            for (const attributeTypeLabel of concept.ownsLabels) {
                                const attributeTypeNodeID = graphElementIDs.types[attributeTypeLabel];
                                if (attributeTypeNodeID != null) {
                                    edges.push({ id: graphElementIDs.nextID, source: concept.nodeID, target: attributeTypeNodeID, label: "owns" });
                                    graphElementIDs.nextID++;
                                }
                            }
                        }

                        if (concept.playerInstances) {
                            for (const rolePlayer of concept.playerInstances) {
                                const rolePlayerNodeID = graphElementIDs.things[rolePlayer.iid];
                                if (rolePlayerNodeID != null) {
                                    edges.push({ id: graphElementIDs.nextID, source: concept.nodeID, target: rolePlayerNodeID, label: rolePlayer.role });
                                    graphElementIDs.nextID++;
                                }
                            }
                        }

                        if (concept.ownerIIDs) {
                            for (const ownerIID of concept.ownerIIDs) {
                                const ownerNodeID = graphElementIDs.things[ownerIID];
                                if (ownerNodeID != null) {
                                    edges.push({ id: graphElementIDs.nextID, source: ownerNodeID, target: concept.nodeID, label: "has" });
                                    graphElementIDs.nextID++;
                                }
                            }
                        }
                    }
                }

                setGraphElementIDs(graphElementIDs);
                // TODO: AnswerGraph and VisualiserData are not intuitive - they're usually the same unless the
                //  graph tab is inactive
                // setAnswerGraph({ simulationID, vertices, edges });
                // TODO: We should also skip the Concept API calls on the backend if the Graph tab is inactive
                // TODO: PoC - delete when redundant
                // if (selectedResultsTab === ResultsTab.GRAPH) {
                //     setVisualiserData({ simulationID, vertices: vertices.slice(0, 50), edges: [] });
                //     setTimeout(() => {
                //         setVisualiserData({ simulationID, vertices, edges: [] });
                //     }, 1000);
                //     setTimeout(() => {
                //         setVisualiserData({ simulationID, vertices, edges });
                //     }, 2000);
                // } else {
                //     setVisualiserData({ simulationID, vertices: [], edges: [] });
                // }
                // if (selectedResultsTab === ResultsTab.GRAPH) {
                //     setVisualiserData({simulationID, vertices, edges});
                // } else {
                //     setVisualiserData({simulationID: null, vertices: [], edges: []});
                // }
                setVisualiserData({simulationID, vertices, edges});

                // TODO: There must be a more efficient way of doing this
                // if (rawAnswers) {
                //     const headings = Object.keys(rawAnswers[0]);
                //     const rows = rawAnswers.map(answer => {
                //         const concepts = Object.values(answer);
                //         return concepts.map(concept => {
                //             // TODO: duplicated code
                //             return concept.value != null
                //                 ? `${concept.type}:${concept.value instanceof Date ? moment(concept.value).format("DD-MM-YY HH:mm:ss") : concept.value.toString().slice(0, 100)}`
                //                 : (concept.label || concept.type);
                //         });
                //     });
                //     // TODO: this setting of initialGridTemplateColumns is suspect
                //     setAnswerTable({ headings, rows, initialGridTemplateColumns: `40px ${"200px ".repeat(headings.length)}`.trim() });
                // } else {
                //     setAnswerTable(null); // We don't know what the column headings are if there are no answers
                // }
            } else {
                // setQueryResult("Error executing query");
                // setSnackbar({ open: true, variant: "error", message: res.error });
                // addLogEntry(res.error);
            }
        };

        ipcRenderer.on("match-query-response-part", onReceiveMatchQueryResponsePart);
        return () => {
            ipcRenderer.removeListener("match-query-response-part", onReceiveMatchQueryResponsePart);
        };
    }, [rawAnswers, graphElementIDs, visualiserData]);

    return (
        <TypeDBVisualiser data={visualiserData} className={classes.visualiser} theme={themeState.use()[0].visualiser}
                          onVertexClick={() => null} onZoom={() => null} onFirstTick={() => null}/>
    );
}
