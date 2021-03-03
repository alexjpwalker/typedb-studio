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
var __importDefault = (this && this.__importDefault) || function (mod) {
    return (mod && mod.__esModule) ? mod : { "default": mod };
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.ProtoBuilder = void 0;
const options_pb_1 = __importDefault(require("grakn-protocol/protobuf/options_pb"));
var Options = options_pb_1.default.Options;
var ProtoBuilder;
(function (ProtoBuilder) {
    function options(options) {
        const optionsProto = new Options();
        if (options) {
            if (options.infer() != null)
                optionsProto.setInfer(options.infer());
            if (options.explain() != null)
                optionsProto.setExplain(options.explain());
            if (options.batchSize() != null)
                optionsProto.setBatchSize(options.batchSize());
            if (options.sessionIdleTimeoutMillis() != null)
                optionsProto.setSessionIdleTimeoutMillis(options.sessionIdleTimeoutMillis());
            if (options.schemaLockAcquireTimeoutMillis() != null)
                optionsProto.setSchemaLockAcquireTimeoutMillis(options.schemaLockAcquireTimeoutMillis());
        }
        return optionsProto;
    }
    ProtoBuilder.options = options;
})(ProtoBuilder = exports.ProtoBuilder || (exports.ProtoBuilder = {}));
