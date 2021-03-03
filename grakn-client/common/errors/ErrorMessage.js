"use strict";
/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
Object.defineProperty(exports, "__esModule", { value: true });
exports.ErrorMessage = void 0;
class ErrorMessage {
    constructor(codePrefix, codeNumber, messagePrefix, messageBody) {
        this._codePrefix = codePrefix;
        this._codeNumber = codeNumber;
        this._messagePrefix = messagePrefix;
        this._messageBody = messageBody;
        if (!ErrorMessage.knownErrors.has(codePrefix)) {
            ErrorMessage.knownErrors.set(codePrefix, new Map());
        }
        ErrorMessage.knownErrors.get(codePrefix).set(codeNumber, this);
        ErrorMessage.maxCodeNumber = Math.max(ErrorMessage.maxCodeNumber, codeNumber);
        ErrorMessage.maxCodeDigits = String(ErrorMessage.maxCodeNumber).length;
    }
    code() {
        if (this._code == null) {
            let zeros = "";
            for (let length = String(this._code).length; length < ErrorMessage.maxCodeDigits; length++) {
                zeros += "0";
            }
            this._code = `${this._codePrefix}${zeros}${this._codeNumber}`;
        }
        return this._code;
    }
    message(...args) {
        return `[${this.code()}] ${this._messagePrefix}: ${this._messageBody(args)}`;
    }
    toString() {
        return `[${this.code()}] ${this._messagePrefix}: ${this._messageBody([])}`;
    }
}
exports.ErrorMessage = ErrorMessage;
ErrorMessage.knownErrors = new Map();
ErrorMessage.maxCodeNumber = 0;
(function (ErrorMessage) {
    class Client extends ErrorMessage {
        constructor(code, message) { super("CLI", code, "Illegal Client State", message); }
    }
    ErrorMessage.Client = Client;
    (function (Client) {
        Client.TRANSACTION_CLOSED = new Client(1, () => `The transaction has been closed and no further operation is allowed.`);
        Client.NONPOSITIVE_BATCH_SIZE = new Client(2, (args) => `Batch size cannot be less than 1, was: '${args[0]}'.`);
        Client.MISSING_DB_NAME = new Client(3, () => `Database name cannot be null.`);
        Client.MISSING_RESPONSE = new Client(4, (args) => `The required field 'res' of type '${args[0]}' was not set.`);
        Client.UNKNOWN_REQUEST_ID = new Client(5, (args) => `Received a response with unknown request id '${args[0]}'.`);
        Client.UNRECOGNISED_SESSION_TYPE = new Client(6, (args) => `Session type '${args[0]}' was not recognised.`);
    })(Client = ErrorMessage.Client || (ErrorMessage.Client = {}));
    class Concept extends ErrorMessage {
        constructor(code, message) { super("CON", code, "Concept Error", message); }
    }
    ErrorMessage.Concept = Concept;
    (function (Concept) {
        Concept.INVALID_CONCEPT_CASTING = new Concept(1, (args) => `Invalid concept conversion from '${args[0]}' to '${args[1]}'.`);
        Concept.MISSING_TRANSACTION = new Concept(2, () => `Transaction cannot be null.`);
        Concept.MISSING_IID = new Concept(3, () => `IID cannot be null or empty.`);
        Concept.MISSING_LABEL = new Concept(4, () => `Label cannot be null or empty.`);
        Concept.BAD_ENCODING = new Concept(5, (args) => `The encoding '${args[0]}' was not recognised.`);
        Concept.BAD_VALUE_TYPE = new Concept(6, (args) => `The value type '${args[0]}' was not recognised.`);
        Concept.BAD_ATTRIBUTE_VALUE = new Concept(7, (args) => `The attribute value '${args[0]}' was not recognised›.`);
    })(Concept = ErrorMessage.Concept || (ErrorMessage.Concept = {}));
    class Query extends ErrorMessage {
        constructor(code, message) { super("QRY", code, "Query Error", message); }
    }
    ErrorMessage.Query = Query;
    (function (Query) {
        Query.VARIABLE_DOES_NOT_EXIST = new Query(1, (args) => `The variable '${args[0]}' does not exist.`);
        Query.NO_EXPLANATION = new Query(2, () => `No explanation was found.`);
        Query.BAD_ANSWER_TYPE = new Query(3, (args) => `The answer '${args[0]}' was not recognised.`);
        Query.MISSING_ANSWER = new Query(4, (args) => `The required field 'answer' of type '${args[0]}' was not set.`);
        Query.ILLEGAL_CAST = new Query(5, (args) => `Illegal casting operation from '${args[0]}' to '${args[1]}'.`);
    })(Query = ErrorMessage.Query || (ErrorMessage.Query = {}));
})(ErrorMessage = exports.ErrorMessage || (exports.ErrorMessage = {}));
