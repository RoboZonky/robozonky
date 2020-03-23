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

package com.github.robozonky.internal.tenant;

import java.time.Instant;
import java.util.Optional;

public interface Availability {

    /**
     * @return The next time when the app should check for availability. In the meantime, no operations should
     *         be attempted.
     */
    Instant nextAvailabilityCheck();

    /**
     * Whether or not the app is available.
     * 
     * @return False if the app is considered in downtime.
     */
    boolean isAvailable();

    /**
     * Report that the app's operation has finished successfully.
     * 
     * @return Empty if {@link #isAvailable()} is true, or when it is decided that the unavailability should continue.
     *         Otherwise {@link #isAvailable()} becomes true.
     */
    Optional<Instant> registerSuccess();

    /**
     * Report an error during the app's operation.
     * 
     * @param ex The exception that caused the unavailability.
     * @return True if {@link #isAvailable()} changed its value as a result of this call.
     */
    boolean registerException(final Exception ex);
}
