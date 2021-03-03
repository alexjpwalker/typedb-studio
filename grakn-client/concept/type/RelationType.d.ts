import { ThingType, RemoteThingType, Relation, RoleType, Grakn, Merge, Stream } from "../../dependencies_internal";
import Transaction = Grakn.Transaction;
export interface RelationType extends ThingType {
    asRemote(transaction: Transaction): RemoteRelationType;
}
export interface RemoteRelationType extends Merge<RemoteThingType, RelationType> {
    create(): Promise<Relation>;
    getRelates(roleLabel: string): Promise<RoleType>;
    getRelates(): Stream<RoleType>;
    setRelates(roleLabel: string): Promise<void>;
    setRelates(roleLabel: string, overriddenLabel: string): Promise<void>;
    unsetRelates(roleLabel: string): Promise<void>;
    setSupertype(relationType: RelationType): Promise<void>;
    getSubtypes(): Stream<RelationType>;
    getInstances(): Stream<Relation>;
    asRemote(transaction: Transaction): RemoteRelationType;
}
