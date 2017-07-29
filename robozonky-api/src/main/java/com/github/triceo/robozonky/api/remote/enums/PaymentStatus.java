/*
 * Copyright 2017 Lukáš Petrovický
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

package com.github.triceo.robozonky.api.remote.enums;

/**
 * Represents the status of a loan.
 */
public enum PaymentStatus {

    OK,
    DUE,
    COVERED,
    NOT_COVERED,
    PAID_OFF,
    CANCELED,
    WRITTEN_OFF,
    PAID;

    /**
     * @return Values of this enum that correspond to investments where there are overdue instalments.
     */
    public static PaymentStatuses getDelinquent() {
        return PaymentStatuses.of(PaymentStatus.DUE);
    }

    /**
     *
     * @return Values of this enum that correspond to investments that have no expected future instalments.
     */
    public static PaymentStatuses getDone() {
        return PaymentStatuses.of(PaymentStatus.CANCELED, PaymentStatus.PAID_OFF, PaymentStatus.WRITTEN_OFF,
                                  PaymentStatus.PAID);
    }

}
