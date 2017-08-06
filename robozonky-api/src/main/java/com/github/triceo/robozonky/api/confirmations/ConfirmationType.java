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

package com.github.triceo.robozonky.api.confirmations;

/**
 * Describes the type of response from the confirmation endpoint.
 *
 * At some point in the future, the endpoint will only ever support the "DELEGATED" response.
 */
@Deprecated
public enum ConfirmationType {

    /**
     * This investment is no longer RoboZonky's concern, the confirmation endpoint takes over the investment operation.
     */
    DELEGATED,
    /**
     * The endpoint approved the investment, RoboZonky can now submit the operation to Zonky API.
     */
    APPROVED,
    /**
     * The endpoint rejected the investment, RoboZonky must not invest.
     */
    REJECTED

}
