import { Grakn } from "grakn-client/Grakn";
const { TransactionType } = Grakn;

export const META_LABELS = new Set(['entity', 'relation', 'attribute', 'role', 'thing']);

export const baseTypes = {
  THING_TYPE: 'THING_TYPE',
  ENTITY_TYPE: 'ENTITY_TYPE',
  RELATION_TYPE: 'RELATION_TYPE',
  ATTRIBUTE_TYPE: 'ATTRIBUTE_TYPE',
  ROLE_TYPE: 'ROLE_TYPE',
  ENTITY_INSTANCE: 'ENTITY',
  RELATION_INSTANCE: 'RELATION',
  ATTRIBUTE_INSTANCE: 'ATTRIBUTE',
};

export const reopenTransaction = async (state, commit) => {
  const graknTx = global.graknTx[state.activeTab];
  const isGraknTxOpen = await graknTx.isOpen();
  if (!isGraknTxOpen) { // graknTx has been invalidated because of an error and so it's closed now
    global.graknTx[state.activeTab] = await global.graknSession.transaction(TransactionType.WRITE);
    commit('setGlobalErrorMsg', 'The transaction was refreshed and, as a result, the explanation of currently displayed inferred nodes may be incomplete.');
  }
};
