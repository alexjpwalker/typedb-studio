import { Thing, RemoteThing, AttributeType, Grakn, Merge, Stream, ThingType } from "../../dependencies_internal";
import ValueClass = AttributeType.ValueClass;
import Transaction = Grakn.Transaction;
export interface Attribute<T extends ValueClass> extends Thing {
    getValue(): T;
    asRemote(transaction: Transaction): RemoteAttribute<T>;
    isBoolean(): boolean;
    isLong(): boolean;
    isDouble(): boolean;
    isString(): boolean;
    isDateTime(): boolean;
}
export interface RemoteAttribute<T extends ValueClass> extends Merge<RemoteThing, Attribute<T>> {
    getOwners(): Stream<Thing>;
    getOwners(ownerType: ThingType): Stream<Thing>;
    getType(): Promise<AttributeType>;
    asRemote(transaction: Transaction): RemoteAttribute<T>;
}
export interface BooleanAttribute extends Attribute<boolean> {
    asRemote(transaction: Transaction): RemoteBooleanAttribute;
}
export interface RemoteBooleanAttribute extends Merge<RemoteAttribute<boolean>, BooleanAttribute> {
    asRemote(transaction: Transaction): RemoteBooleanAttribute;
}
export interface LongAttribute extends Attribute<number> {
    asRemote(transaction: Transaction): RemoteLongAttribute;
}
export interface RemoteLongAttribute extends Merge<RemoteAttribute<number>, LongAttribute> {
    asRemote(transaction: Transaction): RemoteLongAttribute;
}
export interface DoubleAttribute extends Attribute<number> {
    asRemote(transaction: Transaction): RemoteDoubleAttribute;
}
export interface RemoteDoubleAttribute extends Merge<RemoteAttribute<number>, LongAttribute> {
    asRemote(transaction: Transaction): RemoteDoubleAttribute;
}
export interface StringAttribute extends Attribute<string> {
    asRemote(transaction: Transaction): RemoteStringAttribute;
}
export interface RemoteStringAttribute extends Merge<RemoteAttribute<string>, StringAttribute> {
    asRemote(transaction: Transaction): RemoteStringAttribute;
}
export interface DateTimeAttribute extends Attribute<Date> {
    asRemote(transaction: Transaction): RemoteDateTimeAttribute;
}
export interface RemoteDateTimeAttribute extends Merge<RemoteAttribute<Date>, DateTimeAttribute> {
    asRemote(transaction: Transaction): RemoteDateTimeAttribute;
}
