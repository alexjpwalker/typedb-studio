export declare class GraknOptions {
    private _infer;
    private _traceInference;
    private _explain;
    private _batchSize;
    private _sessionIdleTimeoutMillis;
    private _schemaLockAcquireTimeoutMillis;
    constructor();
    infer(): boolean;
    setInfer(infer: boolean): GraknOptions;
    traceInference(): boolean;
    setTraceInference(traceInference: boolean): GraknOptions;
    explain(): boolean;
    setExplain(explain: boolean): GraknOptions;
    batchSize(): number;
    setBatchSize(batchSize: number): GraknOptions;
    sessionIdleTimeoutMillis(): number;
    setSessionIdleTimeoutMillis(sessionIdleTimeoutMillis: number): GraknOptions;
    schemaLockAcquireTimeoutMillis(): number;
    setSchemaLockAcquireTimeoutMillis(schemaLockAcquireTimeoutMillis: number): GraknOptions;
}
