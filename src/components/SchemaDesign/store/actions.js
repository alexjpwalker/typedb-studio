import {
  ADD_ATTRIBUTE_TYPE,
  ADD_ROLE_TYPE,
  CANVAS_RESET,
  COMMIT_TX,
  CURRENT_DATABASE_CHANGED,
  DEFINE_ATTRIBUTE_TYPE,
  DEFINE_ENTITY_TYPE,
  DEFINE_RELATION_TYPE,
  DEFINE_RULE,
  DELETE_ATTRIBUTE,
  DELETE_ROLE,
  DELETE_SCHEMA_CONCEPT,
  INITIALISE_VISUALISER,
  LOAD_SCHEMA,
  OPEN_GRAKN_TX,
  REFRESH_SELECTED_NODE,
  UPDATE_METATYPE_INSTANCES,
} from '@/components/shared/StoresActions';
import logger from '@/logger';

import SchemaHandler from '../SchemaHandler';
import { computeAttributes, computeRoles, loadMetaTypeInstances, updateNodePositions, } from '../SchemaUtils';
import SchemaCanvasEventsHandler from '../SchemaCanvasEventsHandler';
import CDB from '../../shared/CanvasDataBuilder';
import { Grakn } from "grakn-client/Grakn";

const { SessionType, TransactionType } = Grakn;

export default {
  async [OPEN_GRAKN_TX]({ commit }) {
    const graknTx = await global.graknSession.transaction(TransactionType.WRITE);
    if (!global.graknTx) global.graknTx = {};
    global.graknTx.schemaDesign = graknTx;
    commit('setSchemaHandler', new SchemaHandler(graknTx));
    return graknTx;
  },

  async [CURRENT_DATABASE_CHANGED]({ state, dispatch, commit }, database) {
    if (database !== state.currentDatabase) {
      dispatch(CANVAS_RESET);
      commit('currentDatabase', database);
      if (global.graknSession) await global.graknSession.close();
      global.graknSession = await global.grakn.session(database, SessionType.SCHEMA);
      dispatch(UPDATE_METATYPE_INSTANCES);
      dispatch(LOAD_SCHEMA);
    }
  },

  async [UPDATE_METATYPE_INSTANCES]({ dispatch, commit }) {
    const graknTx = await dispatch(OPEN_GRAKN_TX);
    const metaTypeInstances = await loadMetaTypeInstances(graknTx);
    graknTx.close();
    commit('metaTypeInstances', metaTypeInstances);
  },

  [CANVAS_RESET]({ state, commit }) {
    state.visFacade.resetCanvas();
    commit('selectedNodes', null);
    commit('updateCanvasData');
  },

  [INITIALISE_VISUALISER]({ state, commit, dispatch }, { container, visFacade }) {
    commit('setVisFacade', visFacade.initVisualiser(container, state.visStyle));
    SchemaCanvasEventsHandler.registerHandlers({ state, commit, dispatch });
  },

  async [LOAD_SCHEMA]({ state, commit, dispatch }) {
    const graknTx = await dispatch(OPEN_GRAKN_TX);

    try {
      if (!state.visFacade) return;
      commit('loadingSchema', true);

      const answers = await graknTx.query().match('match $x sub thing;').collect();

      const data = await CDB.buildTypes(answers);
      data.nodes = updateNodePositions(data.nodes);

      state.visFacade.addToCanvas({ nodes: data.nodes, edges: data.edges });
      state.visFacade.fitGraphToWindow();

      data.nodes = await computeAttributes(data.nodes, graknTx);
      data.nodes = await computeRoles(data.nodes, graknTx);
      state.visFacade.updateNode(data.nodes);

      graknTx.close();
      commit('loadingSchema', false);
    } catch (e) {
      logger.error(e.stack);
      graknTx.close();
      commit('loadingSchema', false);
      throw e;
    }
  },

  async [COMMIT_TX](store, graknTx) {
    return graknTx.commit();
  },

  async [DEFINE_ENTITY_TYPE]({ state, dispatch }, payload) {
    let tx = await dispatch(OPEN_GRAKN_TX);

    // define entity type
    await state.schemaHandler.defineEntityType(payload.entityLabel, payload.superType);

    // add attribute types to entity type
    await Promise.all(payload.attributeTypes.map(async (attributeType) => {
      await state.schemaHandler.addAttribute(payload.entityLabel, attributeType);
    }));

    // add roles to entity type
    await Promise.all(payload.roleTypes.map(async (roleType) => {
      const [relationLabel, roleLabel] = roleType.split(':');
      await state.schemaHandler.addPlaysRole(payload.entityLabel, relationLabel, roleLabel);
    }));

    await dispatch(COMMIT_TX, tx)
      .catch((e) => {
        tx.close();
        logger.error(e.stack);
        throw e;
      });

    await dispatch(UPDATE_METATYPE_INSTANCES);

    tx = await dispatch(OPEN_GRAKN_TX);

    const concept = await tx.concepts().getEntityType(payload.entityLabel);

    const node = CDB.getTypeNode(concept);
    const edges = await CDB.getTypeEdges(concept, [node.id, ...state.visFacade.getAllNodes().map(n => n.id)]);

    state.visFacade.addToCanvas({ nodes: [node], edges });

    // attach attributes and roles to visnode and update on graph to render the right bar attributes
    let nodes = await computeAttributes([node], tx);
    nodes = await computeRoles(nodes, tx);
    state.visFacade.updateNode(nodes);
    tx.close();
  },

  async [DEFINE_ATTRIBUTE_TYPE]({ state, dispatch }, payload) {
    let graknTx = await dispatch(OPEN_GRAKN_TX);

    // define entity type
    await state.schemaHandler.defineAttributeType(payload);

    // add attribute types to attribute type
    await Promise.all(payload.attributeTypes.map(async (attributeType) => {
      await state.schemaHandler.addAttribute(payload.attributeLabel, attributeType);
    }));

    // add roles to attribute type
    await Promise.all(payload.roleTypes.map(async (roleType) => {
      await state.schemaHandler.addPlaysRole(payload.attributeLabel, roleType);
    }));

    await dispatch(COMMIT_TX, graknTx)
      .catch((e) => {
        graknTx.close();
        logger.error(e.stack);
        throw e;
      });

    await dispatch(UPDATE_METATYPE_INSTANCES);

    graknTx = await dispatch(OPEN_GRAKN_TX);

    const concept = await graknTx.getSchemaConcept(payload.attributeLabel);
    concept.label = payload.attributeLabel;

    const node = CDB.getTypeNode(concept);
    const edges = await CDB.getTypeEdges(concept, [node.id, ...state.visFacade.getAllNodes().map(n => n.id)]);

    state.visFacade.addToCanvas({ nodes: [node], edges });

    // attach attributes and roles to visnode and update on graph to render the right bar attributes
    let nodes = await computeAttributes([node], graknTx);
    nodes = await computeRoles(nodes, graknTx);
    state.visFacade.updateNode(nodes);
    graknTx.close();
  },

  async [ADD_ATTRIBUTE_TYPE]({ state, dispatch }, payload) {
    let graknTx = await dispatch(OPEN_GRAKN_TX);

    // add attribute types to schema concept
    await Promise.all(payload.attributeTypes.map(async (attributeType) => {
      await state.schemaHandler.addAttribute(payload.label, attributeType);
    }));

    await dispatch(COMMIT_TX, graknTx)
      .catch((e) => {
        graknTx.close();
        logger.error(e.stack);
        throw e;
      });
    graknTx = await dispatch(OPEN_GRAKN_TX);

    const node = state.visFacade.getNode(state.selectedNodes[0].id);

    const ownerConcept = await graknTx.getSchemaConcept(node.typeLabel);
    const edges = await CDB.getTypeEdges(ownerConcept, state.visFacade.getAllNodes().map(n => n.id));

    state.visFacade.addToCanvas({ nodes: [], edges });

    graknTx.close();
    state.visFacade.updateNode(node);
  },

  async [ADD_ROLE_TYPE]({ state, dispatch }, payload) {
    let graknTx = await dispatch(OPEN_GRAKN_TX);

    // add role types to schema concept
    await Promise.all(payload.roleTypes.map(async (roleType) => {
      await state.schemaHandler.addPlaysRole(payload.label, roleType);
    }));

    await dispatch(COMMIT_TX, graknTx)
      .catch((e) => {
        graknTx.close();
        logger.error(e.stack);
        throw e;
      });
    graknTx = await dispatch(OPEN_GRAKN_TX);

    const node = state.visFacade.getNode(state.selectedNodes[0].id);

    const edges = await Promise.all(payload.roleTypes.map(async (roleType) => {
      const relationTypes = await (await (await graknTx.getSchemaConcept(roleType)).relations()).collect();
      node.roles = [...node.roles, roleType];

      return Promise.all(relationTypes.map(async relType => CDB.getTypeEdges(relType, state.visFacade.getAllNodes().map(n => n.id))));
    })).then(edges => edges.flatMap(x => x));

    state.visFacade.addToCanvas({ nodes: [], edges: edges.flatMap(x => x) });
    graknTx.close();
    state.visFacade.updateNode(node);
  },

  async [DELETE_ATTRIBUTE]({ state, dispatch }, payload) {
    let graknTx = await dispatch(OPEN_GRAKN_TX);

    await state.schemaHandler.deleteAttribute(payload);

    await dispatch(COMMIT_TX, graknTx)
      .catch((e) => {
        graknTx.close();
        logger.error(e.stack);
        throw e;
      });

    const node = state.visFacade.getNode(state.selectedNodes[0].id);
    node.attributes = Object.values(node.attributes).sort((a, b) => ((a.type > b.type) ? 1 : -1));
    node.attributes.splice(payload.index, 1);
    state.visFacade.updateNode(node);

    // delete edge to attribute type
    graknTx = await dispatch(OPEN_GRAKN_TX);

    const attributeTypeId = (await graknTx.getSchemaConcept(payload.attributeLabel)).id;
    const edgesIds = state.visFacade.edgesConnectedToNode(state.selectedNodes[0].id);

    edgesIds
      .filter(edgeId => (state.visFacade.getEdge(edgeId).to === attributeTypeId) &&
        ((state.visFacade.getEdge(edgeId).label === 'has') || (state.visFacade.getEdge(edgeId).hiddenLabel === 'has')))
      .forEach((edgeId) => { state.visFacade.deleteEdge(edgeId); });

    graknTx.close();
  },

  async [DELETE_ROLE]({ state, dispatch }, payload) {
    let graknTx = await dispatch(OPEN_GRAKN_TX);

    const type = await graknTx.getSchemaConcept(state.selectedNodes[0].label);

    if (await (await type.instances()).next()) throw Error('Cannot remove role type from schema concept with instances.');

    await state.schemaHandler.deletePlaysRole(payload);

    await dispatch(COMMIT_TX, graknTx)
      .catch((e) => {
        graknTx.close();
        logger.error(e.stack);
        throw e;
      });

    const node = state.visFacade.getNode(state.selectedNodes[0].id);
    node.roles = Object.values(node.roles).sort((a, b) => ((a.type > b.type) ? 1 : -1));
    node.roles.splice(payload.index, 1);
    state.visFacade.updateNode(node);

    // delete role edge
    graknTx = await dispatch(OPEN_GRAKN_TX);

    const edgesIds = state.visFacade.edgesConnectedToNode(state.selectedNodes[0].id);
    edgesIds
      .filter(edgeId => (state.visFacade.getEdge(edgeId).to === state.selectedNodes[0].id) &&
        ((state.visFacade.getEdge(edgeId).label === payload.roleLabel) || (state.visFacade.getEdge(edgeId).hiddenLabel === payload.roleLabel)))
      .forEach((edgeId) => { state.visFacade.deleteEdge(edgeId); });

    graknTx.close();
  },

  async [DEFINE_RELATION_TYPE]({ state, dispatch }, payload) {
    let graknTx = await dispatch(OPEN_GRAKN_TX);
    await state.schemaHandler.defineRelationType(payload);

    // define and relate roles to relation type
    await Promise.all(payload.defineRoles.map(async (roleType) => {
      await state.schemaHandler.addRelatesRole(payload.relationLabel, roleType.label);
    }));

    // relate roles to relation type
    await Promise.all(payload.relateRoles.map(async (roleType) => {
      await state.schemaHandler.addRelatesRole(payload.relationLabel, roleType);
    }));

    // add attribute types to relation type
    await Promise.all(payload.attributeTypes.map(async (attributeType) => {
      await state.schemaHandler.addAttribute(payload.relationLabel, attributeType);
    }));

    // add roles to relation type
    await Promise.all(payload.roleTypes.map(async (roleType) => {
      await state.schemaHandler.addPlaysRole(payload.relationLabel, roleType);
    }));

    await dispatch(COMMIT_TX, graknTx)
      .catch((e) => {
        graknTx.close();
        logger.error(e.stack);
        throw e;
      });

    await dispatch(UPDATE_METATYPE_INSTANCES);

    graknTx = await dispatch(OPEN_GRAKN_TX);

    const concept = await graknTx.getSchemaConcept(payload.relationLabel);
    concept.label = payload.relationLabel;

    const node = CDB.getTypeNode(concept);
    const edges = await CDB.getTypeEdges(concept, [node.id, ...state.visFacade.getAllNodes().map(n => n.id)]);

    state.visFacade.addToCanvas({ nodes: [node], edges });

    // attach attributes and roles to visnode and update on graph to render the right bar attributes
    let nodes = await computeAttributes([node], graknTx);
    nodes = await computeRoles(nodes, graknTx);
    state.visFacade.updateNode(nodes);
    graknTx.close();
  },

  async [DELETE_SCHEMA_CONCEPT]({ state, dispatch, commit }, payload) {
    const tx = await dispatch(OPEN_GRAKN_TX);

    const type = await tx.concepts().getThingType(payload.typeLabel);

    if (payload.baseType === 'RELATION_TYPE') {
      const roles = await type.asRemote(tx).getRelates().collect();
      await Promise.all(roles.map(async (role) => {
        const rolePlayers = await role.asRemote(tx).getPlayers().collect();

        await Promise.all(rolePlayers.map(async (player) => {
          await state.schemaHandler.deletePlaysRole(player.getLabel(), type.getLabel(), role.getLabel());
        }));

        await state.schemaHandler.deleteRelatesRole(type.getLabel(), role.getLabel());
      }));
    } else if (payload.baseType === 'ATTRIBUTE_TYPE') {
      const nodes = state.visFacade.getAllNodes();
      await Promise.all(nodes.map(async (node) => {
        await state.schemaHandler.deleteAttribute(node.typeLabel, payload.typeLabel);
        node.attributes = node.attributes.filter((x => x.typeLabel !== payload.typeLabel));
      }));
      state.visFacade.updateNode(nodes);
    }

    const typeId = await state.schemaHandler.deleteType(payload.typeLabel);

    await dispatch(COMMIT_TX, tx)
      .catch((e) => {
        tx.close();
        logger.error(e.stack);
        throw e;
      });
    state.visFacade.deleteFromCanvas([typeId]);
    commit('selectedNodes', null);
    await dispatch(UPDATE_METATYPE_INSTANCES);
  },

  [REFRESH_SELECTED_NODE]({ state, commit }) {
    const node = state.selectedNodes[0];
    if (!node) return;
    commit('selectedNodes', null);
    commit('selectedNodes', [node.id]);
  },

  async [DEFINE_RULE]({ state, dispatch }, payload) {
    const graknTx = await dispatch(OPEN_GRAKN_TX);

    // define rule
    await state.schemaHandler.defineRule(payload.ruleLabel, payload.when, payload.then);

    await dispatch(COMMIT_TX, graknTx)
      .catch((e) => {
        graknTx.close();
        logger.error(e.stack);
        throw e;
      });
  },
};
