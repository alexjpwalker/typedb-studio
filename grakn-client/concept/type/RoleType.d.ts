import { RelationType, RemoteThingType, ThingType, Grakn, Merge, Stream } from "../../dependencies_internal";
import Transaction = Grakn.Transaction;
export interface RoleType extends ThingType {
    getScope(): string;
    getScopedLabel(): string;
    asRemote(transaction: Transaction): RemoteRoleType;
}
export interface RemoteRoleType extends Merge<RemoteThingType, RoleType> {
    asRemote(transaction: Transaction): RemoteRoleType;
    getSupertype(): Promise<RoleType>;
    getSupertypes(): Stream<RoleType>;
    getSubtypes(): Stream<RoleType>;
    getRelationType(): Promise<RelationType>;
    getRelationTypes(): Stream<RelationType>;
    getPlayers(): Stream<ThingType>;
}
