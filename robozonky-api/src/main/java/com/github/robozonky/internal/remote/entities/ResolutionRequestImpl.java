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

package com.github.robozonky.internal.remote.entities;

import java.util.StringJoiner;

import com.github.robozonky.api.remote.entities.ResolutionRequest;
import com.github.robozonky.api.remote.enums.Resolution;

public class ResolutionRequestImpl implements ResolutionRequest {

    private long reservationId;
    private Resolution resolution;

    public ResolutionRequestImpl(final long reservationId, final Resolution resolution) {
        this.reservationId = reservationId;
        this.resolution = resolution;
    }

    ResolutionRequestImpl() {
        // for JAXB
    }

    @Override
    public long getReservationId() {
        return reservationId;
    }

    @Override
    public Resolution getResolution() {
        return resolution;
    }

    public void setReservationId(final long reservationId) {
        this.reservationId = reservationId;
    }

    public void setResolution(final Resolution resolution) {
        this.resolution = resolution;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", ResolutionRequestImpl.class.getSimpleName() + "[", "]")
            .add("reservationId=" + reservationId)
            .add("resolution=" + resolution)
            .toString();
    }
}
