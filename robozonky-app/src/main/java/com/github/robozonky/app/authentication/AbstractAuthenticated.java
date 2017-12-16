/*
 * Copyright 2017 The RoboZonky Project
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

package com.github.robozonky.app.authentication;

import java.time.Duration;
import java.time.Instant;

import com.github.robozonky.api.remote.entities.Restrictions;
import com.github.robozonky.common.remote.Zonky;

abstract class AbstractAuthenticated implements Authenticated {

    private Instant lastRestrictionsUpdate = Instant.EPOCH;
    private Restrictions restrictions = null;

    @Override
    public Restrictions getRestrictions() {
        final boolean needsUpdate = lastRestrictionsUpdate.plus(Duration.ofMinutes(5)).isBefore(Instant.now());
        if (needsUpdate) {
            synchronized (this) {
                restrictions = call(Zonky::getRestrictions);
                lastRestrictionsUpdate = Instant.now();
            }
        }
        return restrictions;
    }
}
