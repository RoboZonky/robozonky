/*
 * Copyright 2018 The RoboZonky Project
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

package com.github.robozonky.app;

/**
 * These are possible return codes for RoboZonky's {@link System#exit(int)}.
 */
public enum ReturnCode {
    /**
     * All is good.
     */
    OK(0),
    /**
     * Failure when parsing command line. No longer used, use {@link #ERROR_SETUP} instead.
     */
    @Deprecated
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
     * Unexpected error state, possibly app bug.
     */
    ERROR_UNEXPECTED(255);

    private final int code;

    ReturnCode(final int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
