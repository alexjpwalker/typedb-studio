import { Thing, RemoteThing, EntityType, Grakn, Merge } from "../../dependencies_internal";
import Transaction = Grakn.Transaction;
export interface Entity extends Thing {
    asRemote(transaction: Transaction): RemoteEntity;
}
export interface RemoteEntity extends Merge<RemoteThing, Entity> {
    getType(): Promise<EntityType>;
    asRemote(transaction: Transaction): RemoteEntity;
}
