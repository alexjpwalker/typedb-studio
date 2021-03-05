<template>
    <div class="panel-container">
        <div @click="toggleContent" class="panel-header">
            <vue-icon :icon="(showRelationsPanel) ?  'chevron-down' : 'chevron-right'" iconSize="14" className="vue-icon"></vue-icon>
            <h1>Relations</h1>
        </div>

        <div v-show="showRelationsPanel">

            <div class="content noselect" v-if="!currentDatabase">
                Please select a database
            </div>
            <div class="content noselect" v-else-if="(!selectedNodes || selectedNodes.length > 1)">
                Please select a node
            </div>

            <div class="content" v-else>

                <div v-if="!currentRole">
                    This concept does not take part in any relations
                </div>

                <div v-else class="row plays-row">
                    <div class="label">
                        Plays
                    </div>
                    <div class="value">
                        <div v-bind:class="(showRolesList) ? 'btn role-btn role-list-shown' : 'btn role-btn'" @click="toggleRoleList"><div class="role-btn-text" >{{currentRole | truncate}}</div><vue-icon class="role-btn-caret" className="vue-icon" icon="caret-down"></vue-icon></div>
                    </div>
                </div>

                <div v-if="relations" class="panel-list-item">
                    <div class="role-list" v-show="showRolesList">
                        <ul v-for="role in Array.from(relations.keys())" :key="role">
                            <li class="role-item" @click="selectRole(role)" v-bind:class="[(role === currentRole) ? 'role-item-selected' : '']">{{role}}</li>
                        </ul>
                    </div>
                </div>

                <div v-if="showRolePlayers">
                    <div v-for="rel in Array.from(relations.get(currentRole).keys())" :key="rel">
                        <div class="column">
                            <div class="row content-item">
                                <div class="label">
                                    In
                                </div>
                                <div class="value relation-item">
                                    {{rel}}
                                </div>
                            </div>

                            <div class="row content-item">
                                <div class="label">
                                    Where
                                </div>
                                <div class="value">
                                </div>
                            </div>

                            <div v-if="showRolePlayers">
                                <div class="roleplayers-list content-item" v-for="(rp, index) in relations.get(currentRole).get(rel)" :key="index">
                                    <div class="label role-label">
                                        {{rp.role}}
                                    </div>
                                    <div class="value player-value">
                                        {{rp.player}}
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>

        </div>

    </div>
</template>

<script>
  import { createNamespacedHelpers } from 'vuex';

  export default {
    name: 'RelationsPanel',
    props: ['tabId'],
    data() {
      return {
        showRelationsPanel: true,
        showRolesList: false,
        currentRole: undefined,
        relations: undefined,
        showRolePlayers: false,
      };
    },
    beforeCreate() {
      const { mapGetters } = createNamespacedHelpers(`tab-${this.$options.propsData.tabId}`);

      // computed
      this.$options.computed = {
        ...(this.$options.computed || {}),
        ...mapGetters(['selectedNodes', 'currentDatabase']),
      };

      // methods
      this.$options.methods = {
        ...(this.$options.methods || {}),
      };
    },
    watch: {
      async selectedNodes() {
        this.showRolePlayers = false;
        // Initialise new relation map whenever a node is selected
        this.relations = new Map();
        this.currentRole = await this.loadRolesAndRelations();
      },
      async currentRole(currentRole) {
        this.showRolePlayers = false;
        // For each relation per role, compute all other role players
        // TODO: it's not clear what this does
        // await Promise.all(Array.from(this.relations.get(currentRole).keys()).map(async (x) => { await this.loadOtherRolePlayers(x); }));
        this.showRolePlayers = true;
      },
    },
    filters: {
      truncate(cr) {
        if (!cr) return null;
        if (cr.length > 13) return `${cr.substring(0, 13)}...`;
        return cr;
      },
    },
    methods: {
      toggleContent() {
        this.showRelationsPanel = !this.showRelationsPanel;
      },
      toggleRoleList() {
        this.showRolesList = !this.showRolesList;
      },
      selectRole(role) {
        this.showRolesList = false;
        this.currentRole = role;
      },
      async loadRolesAndRelations() {
        const node = this.selectedNodes[0];
        const tx = global.graknTx[this.$store.getters.activeTab];
        let roles;
        if (node.iid) {
          const thing = await tx.concepts().getThing(node.iid);
          roles = await thing.asRemote(tx).getPlays().collect();
        } else if (node.typeLabel) {
          const thingType = await tx.concepts().getThingType(node.typeLabel);
          roles = await thingType.asRemote(tx).getPlays().collect();
        } else {
          throw "Node must have either an IID or a Label";
        }
        // Map roles to their respective relations which map to an empty array of other role players in that relation
        // Role => { Relation => [] }
        for (const role of roles) {
          const roleLabel = role.getScopedLabel();
          if (!(roleLabel in this.relations)) {
            this.relations.set(roleLabel, new Map());
            (await role.asRemote(tx).getRelationTypes().collect()).forEach((x) => {
              this.relations.get(roleLabel).set(x.getLabel(), []);
            });
          }
        }
        return this.relations.keys().next().value;
      },
      async loadOtherRolePlayers(rel) {
        // TODO: it's not clear what this is supposed to do
        // If roleplayers have not already been computed
        // if (!this.relations.get(this.currentRole).get(rel).length) {
        //   const node = this.selectedNodes[0];
        //   const tx = global.graknTx[this.$store.getters.activeTab];
        //   let roles;
        //
        //   if (node.iid) {
        //     const thing = await tx.concepts().getThing(node.iid);
        //     roles = await thing.asRemote(tx).getPlays().collect();
        //   } else if (node.typeLabel) {
        //     const relationType = await tx.concepts().getRelationType(node.typeLabel);
        //     roles = await relationType.asRemote(tx).getPlays().collect();
        //   } else {
        //     throw "Node must have either an IID or a Label";
        //   }
        //
        //   console.log(roles);
        //   // Get role concept of selected current role
        //   const role = roles.find(r => r.getScopedLabel() === this.currentRole);
        //
        //   // Get relation concepts of current role
        //   let relationTypes = await role.getRelationTypes().collect();
        //
        //   // Filter relations
        //   relationTypes = await (Promise.all(relationTypes.map(async rel => {
        //     return (((await rel.asRemote(tx).getType()).getLabel() === rel.getLabel()) ? rel : null);
        //   }))).then(rels => rels.filter(r => r));
        //
        //   // For every relation, map relations to their respective roleplayer and the role it plays
        //   await Promise.all(relationTypes.map(async (rel) => {
        //     let roleplayers = await rel.getPlayersByRoleType();
        //     roleplayers = Array.from(roleplayers.entries());
        //
        //     await Promise.all(Array.from(roleplayers, async ([role, setOfThings]) => {
        //       const roleLabel = role.getLabel();
        //       await Promise.all(Array.from(setOfThings.values())
        //         .map(async (thing) => {
        //           const thingLabel = (await thing.asRemote(tx).getType()).getLabel();
        //
        //           // Do not include the current role
        //           if (thing.getIID() !== this.selectedNodes[0].iid && roleLabel !== this.currentRole) {
        //             this.relations.get(this.currentRole).get(rel).push({ role: roleLabel, player: `${thingLabel}: ${thing.getIID()}` });
        //           }
        //         }));
        //     }));
        //   }));
        // }
      },
    },
  };
</script>

<style scoped>

    .plays-row {
        padding-left: var(--container-padding);
        padding-right: var(--container-padding);
    }

    .column {
        display: flex;
        flex-direction: column;
        align-items: center;
        max-height: 220px;
        padding: var(--container-padding);
        overflow-y: scroll;
        overflow-x: hidden;
    }

    .column::-webkit-scrollbar {
        width: 2px;
    }

    .column::-webkit-scrollbar-thumb {
        background: var(--green-4);
    }

    .row {
        display: flex;
        flex-direction: row;
        align-items: center;
    }

    .roleplayers-list {
        display: flex;
        flex-direction: row;
        align-items: center;
        padding: 3px;
    }

    .content {
        padding: var(--container-padding);
        display: flex;
        flex-direction: column;
        overflow: auto;
        justify-content: center;
        border-bottom: var(--container-darkest-border);
    }

    .content-item {
        padding: var(--container-padding);
        display: flex;
        flex-direction: row;
        align-items: center;
    }

    .label {
        margin-right: 20px;
        width: 65px;
    }

    .value {
        width: 110px;
    }

    .role-btn {
        height: 22px;
        min-height: 22px !important;
        cursor: pointer;
        display: flex;
        flex-direction: row;
        width: 100%;
        margin: 0px !important;
        z-index: 2;
    }

    .role-btn-text {
        width: 100%;
        padding-left: 4px;
        display: block;
        white-space: normal !important;
        word-wrap: break-word;
        line-height: 19px;
    }

    .role-btn-caret {
        cursor: pointer;
        align-items: center;
        display: flex;
        min-height: 22px;
        margin: 0px !important;
    }

    .role-list {
        border-left: var(--container-darkest-border);
        border-right: var(--container-darkest-border);
        border-bottom: var(--container-darkest-border);


        background-color: var(--gray-1);
        max-height: 137px;
        overflow: auto;
        position: absolute;
        width: 108px;
        right: 11px;
        margin-top: 0px;
        z-index: 1;
    }

    .role-list-shown {
        border: 1px solid var(--button-hover-border-color) !important;
    }


    .role-list::-webkit-scrollbar {
        width: 2px;
    }

    .role-list::-webkit-scrollbar-thumb {
        background: var(--green-4);
    }

    .role-item {
        align-items: center;
        padding: 2px;
        cursor: pointer;
        white-space: normal;
        word-wrap: break-word;
    }

    .role-item:hover {
        background-color: var(--purple-4);
    }

    /*dynamic*/
    .role-item-selected {
        background-color: var(--purple-3);
    }

</style>
