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
exports.GraknClient = exports.DEFAULT_URI = void 0;
const dependencies_internal_1 = require("../dependencies_internal");
const grpc_js_1 = require("@grpc/grpc-js");
const grakn_grpc_pb_1 = require("grakn-protocol/protobuf/grakn_grpc_pb");
exports.DEFAULT_URI = "localhost:1729";
class GraknClient {
    constructor(address = exports.DEFAULT_URI) {
        this._graknGrpc = new grakn_grpc_pb_1.GraknClient(address, grpc_js_1.ChannelCredentials.createInsecure());
        this._databases = new dependencies_internal_1.RPCDatabaseManager(this._graknGrpc);
    }
    async session(database, type, options) {
        const session = new dependencies_internal_1.RPCSession(this._graknGrpc, database, type);
        return session.open(options);
    }
    databases() {
        return this._databases;
    }
    close() {
        grpc_js_1.closeClient(this._graknGrpc);
    }
}
exports.GraknClient = GraknClient;
