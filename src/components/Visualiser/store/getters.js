export default {
  currentQuery: state => state.currentQuery,
  currentDatabase: state => state.currentDatabase,
  metaTypeInstances: state => state.metaTypeInstances,
  showSpinner: state => state.loadingQuery,
  selectedNodes: state => state.selectedNodes,
  selectedNode: state => ((state.selectedNodes) ? state.selectedNodes[0] : null),
  canvasData: state => state.canvasData,
  isActive: state => (state.currentDatabase !== null),
  contextMenu: state => state.contextMenu,
  globalErrorMsg: state => state.globalErrorMsg,
};
