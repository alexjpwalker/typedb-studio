import { Attribute, RemoteAttribute, BooleanAttribute, DateTimeAttribute, DoubleAttribute, LongAttribute, StringAttribute, RemoteBooleanAttribute, RemoteLongAttribute, RemoteStringAttribute, RemoteDoubleAttribute, RemoteDateTimeAttribute, ThingImpl, RemoteThingImpl, ThingType, AttributeTypeImpl, BooleanAttributeTypeImpl, DateTimeAttributeTypeImpl, DoubleAttributeTypeImpl, LongAttributeTypeImpl, StringAttributeTypeImpl, AttributeType, Grakn, Merge, Stream } from "../../../dependencies_internal";
import ValueClass = AttributeType.ValueClass;
import Transaction = Grakn.Transaction;
import ConceptProto from "grakn-protocol/protobuf/concept_pb";
export declare abstract class AttributeImpl<T extends ValueClass> extends ThingImpl implements Attribute<T> {
    protected constructor(iid: string);
    abstract asRemote(transaction: Transaction): RemoteAttribute<T>;
    abstract getValue(): T;
    isBoolean(): boolean;
    isString(): boolean;
    isDouble(): boolean;
    isLong(): boolean;
    isDateTime(): boolean;
    isAttribute(): boolean;
}
export declare abstract class RemoteAttributeImpl<T extends ValueClass> extends RemoteThingImpl implements RemoteAttribute<T> {
    protected constructor(transaction: Transaction, iid: string);
    getOwners(ownerType?: ThingType): Stream<ThingImpl>;
    getType(): Promise<AttributeTypeImpl>;
    isBoolean(): boolean;
    isString(): boolean;
    isDouble(): boolean;
    isLong(): boolean;
    isDateTime(): boolean;
    isAttribute(): boolean;
    abstract asRemote(transaction: Transaction): RemoteAttribute<T>;
    abstract getValue(): T;
}
export declare class BooleanAttributeImpl extends AttributeImpl<boolean> implements BooleanAttribute {
    private readonly _value;
    constructor(iid: string, value: boolean);
    static of(protoThing: ConceptProto.Thing): BooleanAttributeImpl;
    asRemote(transaction: Transaction): RemoteBooleanAttributeImpl;
    getValue(): boolean;
    isBoolean(): boolean;
}
export declare class RemoteBooleanAttributeImpl extends RemoteAttributeImpl<boolean> implements Merge<RemoteBooleanAttribute, BooleanAttribute> {
    private readonly _value;
    constructor(transaction: Transaction, iid: string, value: boolean);
    getValue(): boolean;
    getType(): Promise<BooleanAttributeTypeImpl>;
    asRemote(transaction: Transaction): RemoteBooleanAttributeImpl;
    isBoolean(): boolean;
}
export declare class LongAttributeImpl extends AttributeImpl<number> implements LongAttribute {
    private readonly _value;
    constructor(iid: string, value: number);
    static of(protoThing: ConceptProto.Thing): LongAttributeImpl;
    asRemote(transaction: Transaction): RemoteLongAttributeImpl;
    getValue(): number;
    isLong(): boolean;
}
export declare class RemoteLongAttributeImpl extends RemoteAttributeImpl<number> implements Merge<RemoteLongAttribute, LongAttribute> {
    private readonly _value;
    constructor(transaction: Transaction, iid: string, value: number);
    getValue(): number;
    getType(): Promise<LongAttributeTypeImpl>;
    asRemote(transaction: Transaction): RemoteLongAttributeImpl;
    isLong(): boolean;
}
export declare class DoubleAttributeImpl extends AttributeImpl<number> implements DoubleAttribute {
    private readonly _value;
    constructor(iid: string, value: number);
    static of(protoThing: ConceptProto.Thing): DoubleAttributeImpl;
    asRemote(transaction: Transaction): RemoteDoubleAttributeImpl;
    getValue(): number;
    isDouble(): boolean;
}
export declare class RemoteDoubleAttributeImpl extends RemoteAttributeImpl<number> implements Merge<RemoteDoubleAttribute, DoubleAttribute> {
    private readonly _value;
    constructor(transaction: Transaction, iid: string, value: number);
    getValue(): number;
    getType(): Promise<DoubleAttributeTypeImpl>;
    asRemote(transaction: Transaction): RemoteDoubleAttributeImpl;
    isDouble(): boolean;
}
export declare class StringAttributeImpl extends AttributeImpl<string> implements StringAttribute {
    private readonly _value;
    constructor(iid: string, value: string);
    static of(protoThing: ConceptProto.Thing): StringAttributeImpl;
    asRemote(transaction: Transaction): RemoteStringAttributeImpl;
    getValue(): string;
    isString(): boolean;
}
export declare class RemoteStringAttributeImpl extends RemoteAttributeImpl<string> implements Merge<RemoteStringAttribute, StringAttribute> {
    private readonly _value;
    constructor(transaction: Transaction, iid: string, value: string);
    getValue(): string;
    getType(): Promise<StringAttributeTypeImpl>;
    asRemote(transaction: Transaction): RemoteStringAttributeImpl;
    isString(): boolean;
}
export declare class DateTimeAttributeImpl extends AttributeImpl<Date> implements DateTimeAttribute {
    private readonly _value;
    constructor(iid: string, value: Date);
    static of(protoThing: ConceptProto.Thing): DateTimeAttributeImpl;
    asRemote(transaction: Transaction): RemoteDateTimeAttributeImpl;
    getValue(): Date;
    isDateTime(): boolean;
}
declare class RemoteDateTimeAttributeImpl extends RemoteAttributeImpl<Date> implements Merge<RemoteDateTimeAttribute, DateTimeAttribute> {
    private readonly _value;
    constructor(transaction: Transaction, iid: string, value: Date);
    getValue(): Date;
    getType(): Promise<DateTimeAttributeTypeImpl>;
    asRemote(transaction: Transaction): RemoteDateTimeAttributeImpl;
    isDateTime(): boolean;
}
export declare namespace AttributeImpl {
    function of(thingProto: ConceptProto.Thing): AttributeImpl<ValueClass>;
}
export {};
