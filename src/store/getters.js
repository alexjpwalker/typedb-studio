export const allDatabases = state => state.databases;
export const isAuthorised = state => (!state.isAuthenticated || state.credentials);
export const landingPage = state => state.landingPage;
export const userLogged = state => state.userLogged;
export const credentials = state => state.credentials;
export const isGraknRunning = state => state.isGraknRunning;
export const activeTab = state => state.activeTab;
