import { ErrorMessage } from "../../dependencies_internal";
export declare class GraknClientError extends Error {
    constructor(error: string | Error | ErrorMessage);
}
