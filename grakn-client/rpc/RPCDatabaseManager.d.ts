import { GraknClient as GraknGrpc } from "grakn-protocol/protobuf/grakn_grpc_pb";
import { Grakn } from "../dependencies_internal";
export declare class RPCDatabaseManager implements Grakn.DatabaseManager {
    private _grpcClient;
    constructor(client: GraknGrpc);
    contains(name: string): Promise<boolean>;
    create(name: string): Promise<void>;
    delete(name: string): Promise<void>;
    all(): Promise<string[]>;
}
