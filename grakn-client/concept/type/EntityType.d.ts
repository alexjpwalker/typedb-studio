import { ThingType, RemoteThingType, Entity, Grakn, Merge, Stream } from "../../dependencies_internal";
import Transaction = Grakn.Transaction;
export interface EntityType extends ThingType {
    asRemote(transaction: Transaction): RemoteEntityType;
}
export interface RemoteEntityType extends Merge<RemoteThingType, EntityType> {
    create(): Promise<Entity>;
    setSupertype(superEntityType: EntityType): Promise<void>;
    getSubtypes(): Stream<EntityType>;
    getInstances(): Stream<Entity>;
    asRemote(transaction: Transaction): RemoteEntityType;
}
