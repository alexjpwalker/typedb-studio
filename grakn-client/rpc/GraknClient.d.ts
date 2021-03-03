import { Grakn, GraknOptions } from "../dependencies_internal";
export declare const DEFAULT_URI = "localhost:1729";
export declare class GraknClient implements Grakn.Client {
    private readonly _databases;
    private readonly _graknGrpc;
    constructor(address?: string);
    session(database: string, type: Grakn.SessionType, options?: GraknOptions): Promise<Grakn.Session>;
    databases(): Grakn.DatabaseManager;
    close(): void;
}
