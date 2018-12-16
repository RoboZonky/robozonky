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

package com.github.robozonky.api.remote.enums;

/**
 * Represents the status of a loan.
 */
public enum PaymentStatus {

    OK,
    DUE,
    COVERED,
    NOT_COVERED,
    // "zesplatněná"
    PAID_OFF,
    CANCELED,
    // "ztraceno"
    WRITTEN_OFF,
    // "splacená"
    PAID,
    IN_WITHDRAWAL;

    /**
     * @return Values of this enum that correspond to investments where there are overdue instalments.
     */
    public static PaymentStatuses getDelinquent() {
        return PaymentStatuses.of(PaymentStatus.DUE,
                                  PaymentStatus.PAID_OFF);
    }

    /**
     * @return Values of this enum that correspond to investments that still have expected future instalments.
     */
    public static PaymentStatuses getActive() {
        return PaymentStatuses.of(PaymentStatus.OK, PaymentStatus.DUE, PaymentStatus.COVERED);
    }

}
