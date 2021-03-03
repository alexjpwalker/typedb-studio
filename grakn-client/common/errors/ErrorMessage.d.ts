import { Stringable } from "../../dependencies_internal";
export declare abstract class ErrorMessage {
    private readonly _codePrefix;
    private readonly _codeNumber;
    private readonly _messagePrefix;
    private readonly _messageBody;
    private _code;
    private static knownErrors;
    private static maxCodeNumber;
    private static maxCodeDigits;
    protected constructor(codePrefix: string, codeNumber: number, messagePrefix: string, messageBody: (args: Stringable[]) => string);
    code(): string;
    message(...args: Stringable[]): string;
    toString(): string;
}
export declare namespace ErrorMessage {
    class Client extends ErrorMessage {
        constructor(code: number, message: (args?: Stringable[]) => string);
    }
    namespace Client {
        const TRANSACTION_CLOSED: Client;
        const NONPOSITIVE_BATCH_SIZE: Client;
        const MISSING_DB_NAME: Client;
        const MISSING_RESPONSE: Client;
        const UNKNOWN_REQUEST_ID: Client;
        const UNRECOGNISED_SESSION_TYPE: Client;
    }
    class Concept extends ErrorMessage {
        constructor(code: number, message: (args: Stringable[]) => string);
    }
    namespace Concept {
        const INVALID_CONCEPT_CASTING: Concept;
        const MISSING_TRANSACTION: Concept;
        const MISSING_IID: Concept;
        const MISSING_LABEL: Concept;
        const BAD_ENCODING: Concept;
        const BAD_VALUE_TYPE: Concept;
        const BAD_ATTRIBUTE_VALUE: Concept;
    }
    class Query extends ErrorMessage {
        constructor(code: number, message: (args: Stringable[]) => string);
    }
    namespace Query {
        const VARIABLE_DOES_NOT_EXIST: Query;
        const NO_EXPLANATION: Query;
        const BAD_ANSWER_TYPE: Query;
        const MISSING_ANSWER: Query;
        const ILLEGAL_CAST: Query;
    }
}
