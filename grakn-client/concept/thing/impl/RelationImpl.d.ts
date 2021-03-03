import { ThingImpl, RemoteThingImpl, Relation, RemoteRelation, Thing, RelationTypeImpl, RoleType, Grakn, Stream } from "../../../dependencies_internal";
import Transaction = Grakn.Transaction;
import ConceptProto from "grakn-protocol/protobuf/concept_pb";
export declare class RelationImpl extends ThingImpl implements Relation {
    protected constructor(iid: string);
    static of(protoThing: ConceptProto.Thing): RelationImpl;
    asRemote(transaction: Transaction): RemoteRelationImpl;
    isRelation(): boolean;
}
export declare class RemoteRelationImpl extends RemoteThingImpl implements RemoteRelation {
    constructor(transaction: Transaction, iid: string);
    asRemote(transaction: Transaction): RemoteRelationImpl;
    getType(): Promise<RelationTypeImpl>;
    getPlayersByRoleType(): Promise<Map<RoleType, Thing[]>>;
    getPlayers(roleTypes?: RoleType[]): Stream<ThingImpl>;
    addPlayer(roleType: RoleType, player: Thing): Promise<void>;
    removePlayer(roleType: RoleType, player: Thing): Promise<void>;
    isRelation(): boolean;
}
