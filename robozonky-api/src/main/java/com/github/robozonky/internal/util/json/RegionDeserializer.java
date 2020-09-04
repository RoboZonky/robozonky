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

package com.github.robozonky.internal.util.json;

import java.util.Arrays;

import com.github.robozonky.api.remote.enums.Region;

public final class RegionDeserializer extends AbstractDeserializer<Region> {

    public RegionDeserializer() {
        super(s -> Arrays.stream(Region.values())
            .filter(x -> s.equals(x.name())) // Regions in Investment API use their name.
            .findAny()
            .orElseGet(() -> {
                final int actualId = Integer.parseInt(s) - 1; // Regions in Loan API are indexed from 1.
                return Region.values()[actualId];
            }), Region.UNKNOWN);
    }
}
