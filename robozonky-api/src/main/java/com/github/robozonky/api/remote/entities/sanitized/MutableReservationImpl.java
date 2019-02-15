/*
 * Copyright 2019 The RoboZonky Project
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

package com.github.robozonky.api.remote.entities.sanitized;

import com.github.robozonky.api.remote.entities.MyReservation;
import com.github.robozonky.api.remote.entities.RawReservation;

final class MutableReservationImpl extends AbstractBaseLoanImpl<ReservationBuilder> implements ReservationBuilder {

    private MyReservation myReservation;

    MutableReservationImpl() {

    }

    MutableReservationImpl(final RawReservation original) {
        super(original);
        this.myReservation = original.getMyReservation();
    }

    @Override
    public ReservationBuilder setMyReservation(final MyReservation myReservation) {
        this.myReservation = myReservation;
        return this;
    }

    @Override
    public MyReservation getMyReservation() {
        return myReservation;
    }
}
