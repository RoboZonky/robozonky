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

package com.github.robozonky.api.remote.entities;

import java.util.StringJoiner;
import javax.xml.bind.annotation.XmlElement;

import com.github.robozonky.api.remote.enums.Resolution;

public class ResolutionRequest extends BaseEntity {

    private long reservationId;
    private Resolution resolution;

    public ResolutionRequest(final long reservationId, final Resolution resolution) {
        super();
        this.reservationId = reservationId;
        this.resolution = resolution;
    }

    ResolutionRequest() {
        // for JAXB
        super();
    }

    @XmlElement
    public long getReservationId() {
        return reservationId;
    }

    @XmlElement
    public Resolution getResolution() {
        return resolution;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", ResolutionRequest.class.getSimpleName() + "[", "]")
                .add("reservationId=" + reservationId)
                .add("resolution=" + resolution)
                .toString();
    }
}
