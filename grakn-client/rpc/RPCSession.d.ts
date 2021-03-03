import { Grakn, GraknOptions } from "../dependencies_internal";
import GraknProto from "grakn-protocol/protobuf/grakn_grpc_pb";
import GraknGrpc = GraknProto.GraknClient;
export declare class RPCSession implements Grakn.Session {
    private readonly _grpcClient;
    private readonly _database;
    private readonly _type;
    private _sessionId;
    private _isOpen;
    private _pulse;
    constructor(grpcClient: GraknGrpc, database: string, type: Grakn.SessionType);
    open(options?: GraknOptions): Promise<Grakn.Session>;
    transaction(type: Grakn.TransactionType, options?: GraknOptions): Promise<Grakn.Transaction>;
    type(): Grakn.SessionType;
    isOpen(): boolean;
    close(): Promise<void>;
    database(): string;
    private pulse;
}
