/*
 * Copyright 2020 The RoboZonky Project
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

package com.github.robozonky.api.remote.enums;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * {@link #UNKNOWN} is an internal value, not in the Zonky API, and therefore should only ever go last.
 */
@JsonDeserialize(using = LoanHealth.LoanHealthInfoDeserializer.class)
public enum LoanHealth {

    HEALTHY,
    CURRENTLY_IN_DUE,
    HISTORICALLY_IN_DUE,
    UNKNOWN;

    static final class LoanHealthInfoDeserializer extends AbstractDeserializer<LoanHealth> {

        public LoanHealthInfoDeserializer() {
            super(LoanHealth::valueOf, UNKNOWN, true);
        }
    }
}
