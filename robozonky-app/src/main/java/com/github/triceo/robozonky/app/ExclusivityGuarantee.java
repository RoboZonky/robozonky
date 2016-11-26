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

package com.github.triceo.robozonky.app;

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.function.Consumer;

import com.github.triceo.robozonky.api.ReturnCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Makes sure that RoboZonky only proceeds after all other instances of RoboZonky have terminated.
 */
class ExclusivityGuarantee implements State.Handler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExclusivityGuarantee.class);

    private static final File ROBOZONKY_LOCK = new File(System.getProperty("java.io.tmpdir"), "robozonky.lock");
    private final Exclusivity exclusivity = new Exclusivity(ExclusivityGuarantee.ROBOZONKY_LOCK);

    @Override
    public Optional<Consumer<ReturnCode>> get() {
        try {
            this.exclusivity.ensure();
        } catch (final IOException ex) {
            ExclusivityGuarantee.LOGGER.error("Failed acquiring lock, another RoboZonky process likely running.", ex);
            return Optional.empty();
        }
        return Optional.of((code) -> {
            try { // other RoboZonky instances can now start executing
                this.exclusivity.waive();
            } catch (final IOException ex) {
                ExclusivityGuarantee.LOGGER.warn("Failed releasing lock, new RoboZonky processes may not launch.", ex);
            }
        });
    }
}
