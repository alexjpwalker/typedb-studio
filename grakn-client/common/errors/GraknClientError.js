"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
exports.GraknClientError = void 0;
class GraknClientError extends Error {
    constructor(error) {
        if (typeof error === "string") {
            super(error);
        }
        else {
            super(error.toString());
        }
        this.name = "GraknClientError"; // Required to correctly report error type in default throw
    }
}
exports.GraknClientError = GraknClientError;
