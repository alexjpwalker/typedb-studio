import { Thing, RemoteThing, Grakn, RelationType, RoleType, Merge, Stream } from "../../dependencies_internal";
import Transaction = Grakn.Transaction;
export interface Relation extends Thing {
    asRemote(transaction: Transaction): RemoteRelation;
}
export interface RemoteRelation extends Merge<RemoteThing, Relation> {
    getType(): Promise<RelationType>;
    addPlayer(roleType: RoleType, player: Thing): Promise<void>;
    removePlayer(roleType: RoleType, player: Thing): Promise<void>;
    getPlayers(): Stream<Thing>;
    getPlayers(roleTypes: RoleType[]): Stream<Thing>;
    getPlayersByRoleType(): Promise<Map<RoleType, Thing[]>>;
    asRemote(transaction: Transaction): RemoteRelation;
}
