import { GraknOptions, ConceptManager, QueryManager, LogicManager } from "./dependencies_internal";
export declare namespace Grakn {
    interface Client {
        session(databaseName: string, type: SessionType, options?: GraknOptions): Promise<Session>;
        databases(): DatabaseManager;
        close(): void;
    }
    interface DatabaseManager {
        contains(name: string): Promise<boolean>;
        create(name: string): Promise<void>;
        delete(name: string): Promise<void>;
        all(): Promise<string[]>;
    }
    interface Session {
        transaction(type: TransactionType, options?: GraknOptions): Promise<Transaction>;
        type(): SessionType;
        isOpen(): boolean;
        close(): Promise<void>;
        database(): string;
    }
    enum SessionType {
        DATA = 0,
        SCHEMA = 1
    }
    interface Transaction {
        type(): TransactionType;
        isOpen(): boolean;
        concepts(): ConceptManager;
        logic(): LogicManager;
        query(): QueryManager;
        commit(): Promise<void>;
        rollback(): Promise<void>;
        close(): Promise<void>;
    }
    enum TransactionType {
        READ = 0,
        WRITE = 1
    }
}
