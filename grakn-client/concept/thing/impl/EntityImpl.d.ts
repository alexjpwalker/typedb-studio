import { ThingImpl, RemoteThingImpl, Entity, RemoteEntity, EntityTypeImpl, Grakn } from "../../../dependencies_internal";
import Transaction = Grakn.Transaction;
import ConceptProto from "grakn-protocol/protobuf/concept_pb";
export declare class EntityImpl extends ThingImpl implements Entity {
    protected constructor(iid: string);
    static of(protoThing: ConceptProto.Thing): EntityImpl;
    asRemote(transaction: Transaction): RemoteEntityImpl;
    isEntity(): boolean;
}
export declare class RemoteEntityImpl extends RemoteThingImpl implements RemoteEntity {
    constructor(transaction: Transaction, iid: string);
    asRemote(transaction: Transaction): RemoteEntityImpl;
    getType(): Promise<EntityTypeImpl>;
    isEntity(): boolean;
}
