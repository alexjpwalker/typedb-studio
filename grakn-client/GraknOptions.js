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
exports.GraknOptions = void 0;
const dependencies_internal_1 = require("./dependencies_internal");
class GraknOptions {
    constructor() {
        this._infer = null;
        this._traceInference = null;
        this._explain = null;
        this._batchSize = null;
        this._sessionIdleTimeoutMillis = null;
        this._schemaLockAcquireTimeoutMillis = null;
    }
    infer() {
        return this._infer;
    }
    setInfer(infer) {
        this._infer = infer;
        return this;
    }
    traceInference() {
        return this._traceInference;
    }
    setTraceInference(traceInference) {
        this._traceInference = traceInference;
        return this;
    }
    explain() {
        return this._explain;
    }
    setExplain(explain) {
        this._explain = explain;
        return this;
    }
    batchSize() {
        return this._batchSize;
    }
    setBatchSize(batchSize) {
        if (batchSize < 1) {
            throw new dependencies_internal_1.GraknClientError(dependencies_internal_1.ErrorMessage.Client.NONPOSITIVE_BATCH_SIZE.message(batchSize));
        }
        this._batchSize = batchSize;
        return this;
    }
    sessionIdleTimeoutMillis() {
        return this._sessionIdleTimeoutMillis;
    }
    setSessionIdleTimeoutMillis(sessionIdleTimeoutMillis) {
        this._sessionIdleTimeoutMillis = sessionIdleTimeoutMillis;
        return this;
    }
    schemaLockAcquireTimeoutMillis() {
        return this._schemaLockAcquireTimeoutMillis;
    }
    setSchemaLockAcquireTimeoutMillis(schemaLockAcquireTimeoutMillis) {
        this._schemaLockAcquireTimeoutMillis = schemaLockAcquireTimeoutMillis;
        return this;
    }
}
exports.GraknOptions = GraknOptions;
