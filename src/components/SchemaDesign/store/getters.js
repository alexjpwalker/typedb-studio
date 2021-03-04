export default {
  currentDatabase: state => state.currentDatabase,
  metaTypeInstances: state => state.metaTypeInstances,
  showSpinner: state => state.loadingSchema,
  selectedNodes: state => state.selectedNodes,
  selectedNode: state => ((state.selectedNodes) ? state.selectedNodes[0] : null),
  canvasData: state => state.canvasData,
  isActive: state => (state.currentDatabase !== null),
  contextMenu: state => state.contextMenu,
  loadingSchema: state => state.loadingSchema,
  schemaHandler: state => state.schemaHandler,
  visFacade: state => state.visFacade,
};
