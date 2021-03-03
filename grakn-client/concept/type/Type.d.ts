import { Concept, RemoteConcept, Grakn, Merge, Stream } from "../../dependencies_internal";
import Transaction = Grakn.Transaction;
export interface Type extends Concept {
    getLabel(): string;
    isRoot(): boolean;
    asRemote(transaction: Transaction): RemoteType;
}
export interface RemoteType extends Merge<RemoteConcept, Type> {
    setLabel(label: string): Promise<void>;
    isAbstract(): Promise<boolean>;
    getSupertype(): Promise<Type>;
    getSupertypes(): Stream<Type>;
    getSubtypes(): Stream<Type>;
    asRemote(transaction: Transaction): RemoteType;
}
