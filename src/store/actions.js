import { GraknClient } from 'grakn-client/rpc/GraknClient';
import ServerSettings from '@/components/ServerSettings';

export const loadDatabases = async (context) => {
  try {
    const resp = await global.grakn.databases().all();
    context.commit('setIsGraknRunning', true);
    context.commit('setDatabases', resp);
  } catch (e) {
    context.commit('setIsGraknRunning', false);
  }
};

export const createDatabase = async (context, name) => {
  await global.grakn.databases().create(name);
};

export const deleteDatabase = async (context, name) => global.grakn.databases().delete(name)
  .then(async () => { await context.dispatch('loadDatabases'); });

export const login = (context, credentials) =>
  // TODO: Database 'grakn' is hardcoded until we will implement an authenticate endpoint in gRPC
  context.dispatch('initGrakn', credentials).then(() => {
    context.commit('setCredentials', credentials);
    context.commit('userLogged', true);
  });

export const initGrakn = (context, credentials) => {
  global.grakn = new GraknClient(ServerSettings.getServerUri(), /* credentials */);
  context.dispatch('loadDatabases', credentials);
};

export const logout = async (context) => {
  context.commit('setCredentials', undefined);
  context.commit('setDatabases', undefined);
  context.commit('userLogged', false);
  // Need to notify all the other states that they need to invalidate GraknClient
};
