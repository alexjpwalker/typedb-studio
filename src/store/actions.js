import { GraknClient } from 'grakn-client/rpc/GraknClient';
import ServerSettings from '@/components/ServerSettings';

export const loadKeyspaces = async (context) => {
  try {
    const resp = await global.grakn.keyspaces().retrieve();
    context.commit('setIsGraknRunning', true);
    context.commit('setKeyspaces', resp);
  } catch (e) {
    context.commit('setIsGraknRunning', false);
  }
};

export const createKeyspace = async (context, name) => {
  await global.grakn.databases().create(name);
};

export const deleteKeyspace = async (context, name) => global.grakn.keyspaces().delete(name)
  .then(async () => { await context.dispatch('loadKeyspaces'); });

export const login = (context, credentials) =>
  // TODO: Keyspace 'grakn' is hardcoded until we will implement an authenticate endpoint in gRPC
  context.dispatch('initGrakn', credentials).then(() => {
    context.commit('setCredentials', credentials);
    context.commit('userLogged', true);
  });

export const initGrakn = (context, credentials) => {
  global.grakn = new GraknClient(ServerSettings.getServerUri(), /* credentials */);
  context.dispatch('loadKeyspaces', credentials);
};

export const logout = async (context) => {
  context.commit('setCredentials', undefined);
  context.commit('setKeyspaces', undefined);
  context.commit('userLogged', false);
  // Need to notify all the other states that they need to invalidate GraknClient
};
