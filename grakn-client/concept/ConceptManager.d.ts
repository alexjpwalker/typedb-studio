import { ThingType, EntityType, RelationType, AttributeType, RPCTransaction, Thing } from "../dependencies_internal";
export declare class ConceptManager {
    private readonly _rpcTransaction;
    constructor(rpcTransaction: RPCTransaction);
    getRootThingType(): Promise<ThingType>;
    getRootEntityType(): Promise<EntityType>;
    getRootRelationType(): Promise<RelationType>;
    getRootAttributeType(): Promise<AttributeType>;
    putEntityType(label: string): Promise<EntityType>;
    getEntityType(label: string): Promise<EntityType>;
    putRelationType(label: string): Promise<RelationType>;
    getRelationType(label: string): Promise<RelationType>;
    putAttributeType(label: string, valueType: AttributeType.ValueType): Promise<AttributeType>;
    getAttributeType(label: string): Promise<AttributeType>;
    getThing(iid: string): Promise<Thing>;
    getThingType(label: string): Promise<ThingType>;
    private execute;
}
