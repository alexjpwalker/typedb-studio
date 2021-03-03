import { Grakn } from "../dependencies_internal";
import Transaction = Grakn.Transaction;
export interface Rule {
    getLabel(): string;
    getWhen(): string;
    getThen(): string;
    asRemote(transaction: Transaction): RemoteRule;
    isRemote(): boolean;
}
export interface RemoteRule extends Rule {
    setLabel(label: string): Promise<void>;
    delete(): Promise<void>;
    isDeleted(): Promise<boolean>;
    asRemote(transaction: Transaction): RemoteRule;
}
