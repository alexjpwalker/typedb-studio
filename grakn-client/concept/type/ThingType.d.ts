import { AttributeType, RoleType, RemoteType, Type, Grakn, Merge, Stream, Thing } from "../../dependencies_internal";
import Transaction = Grakn.Transaction;
export interface ThingType extends Type {
    asRemote(transaction: Transaction): RemoteThingType;
}
export interface RemoteThingType extends Merge<RemoteType, ThingType> {
    getSupertype(): Promise<ThingType>;
    getSupertypes(): Stream<ThingType>;
    getSubtypes(): Stream<ThingType>;
    getInstances(): Stream<Thing>;
    setLabel(label: string): Promise<void>;
    setAbstract(): Promise<void>;
    unsetAbstract(): Promise<void>;
    setPlays(role: RoleType): Promise<void>;
    setPlays(role: RoleType, overriddenType: RoleType): Promise<void>;
    setOwns(attributeType: AttributeType): Promise<void>;
    setOwns(attributeType: AttributeType, isKey: boolean): Promise<void>;
    setOwns(attributeType: AttributeType, overriddenType: AttributeType): Promise<void>;
    setOwns(attributeType: AttributeType, overriddenType: AttributeType, isKey: boolean): Promise<void>;
    getPlays(): Stream<RoleType>;
    getOwns(): Stream<AttributeType>;
    getOwns(valueType: AttributeType.ValueType): Stream<AttributeType>;
    getOwns(keysOnly: boolean): Stream<AttributeType>;
    getOwns(valueType: AttributeType.ValueType, keysOnly: boolean): Stream<AttributeType>;
    unsetPlays(role: RoleType): Promise<void>;
    unsetOwns(attributeType: AttributeType): Promise<void>;
    asRemote(transaction: Transaction): RemoteThingType;
}
