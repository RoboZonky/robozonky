/*
 * Copyright 2016 Lukáš Petrovický
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.triceo.robozonky.app;

/**
 * These are possible return codes for this application's {@link System#exit(int)}.
 */
enum ReturnCode {
    /**
     * All is good.
     */
    OK(0),
    /**
     * Failure when parsing command line.
     */
    ERROR_WRONG_PARAMETERS(1),
    /**
     * Failure before the start of investing, most likely login.
     */
    ERROR_SETUP(2),
    /**
     * Zonky API not accepting calls.
     */
    ERROR_DOWN(3),
    /**
     * Remote API call failed.
     */
    ERROR_REMOTE(4),
    /**
     * Failed acquiring run lock for an unknown reason.
     */
    ERROR_LOCK(5),
    /**
     * Unexpected error state, possibly app bug.
     */
    ERROR_UNEXPECTED(255);

    private final int code;

    ReturnCode(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
