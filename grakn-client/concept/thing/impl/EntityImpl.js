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
exports.RemoteEntityImpl = exports.EntityImpl = void 0;
const dependencies_internal_1 = require("../../../dependencies_internal");
const concept_pb_1 = __importDefault(require("grakn-protocol/protobuf/concept_pb"));
class EntityImpl extends dependencies_internal_1.ThingImpl {
    constructor(iid) {
        super(iid);
    }
    static of(protoThing) {
        return new EntityImpl(dependencies_internal_1.Bytes.bytesToHexString(protoThing.getIid_asU8()));
    }
    asRemote(transaction) {
        return new RemoteEntityImpl(transaction, this.getIID());
    }
    isEntity() {
        return true;
    }
}
exports.EntityImpl = EntityImpl;
class RemoteEntityImpl extends dependencies_internal_1.RemoteThingImpl {
    constructor(transaction, iid) {
        super(transaction, iid);
    }
    asRemote(transaction) {
        return new RemoteEntityImpl(transaction, this.getIID());
    }
    async getType() {
        const res = await this.execute(new concept_pb_1.default.Thing.Req().setThingGetTypeReq(new concept_pb_1.default.Thing.GetType.Req()));
        return dependencies_internal_1.ThingTypeImpl.of(res.getThingGetTypeRes().getThingType());
    }
    isEntity() {
        return true;
    }
}
exports.RemoteEntityImpl = RemoteEntityImpl;
