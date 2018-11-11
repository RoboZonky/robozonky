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

package com.github.robozonky.common.state;

import java.time.OffsetDateTime;
import java.util.Set;

import com.github.robozonky.api.SessionInfo;
import com.github.robozonky.common.Tenant;
import com.github.robozonky.common.jobs.TenantPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.stream.Collectors.toSet;

final class StateCleaner implements TenantPayload {

    private static final Logger LOGGER = LoggerFactory.getLogger(StateCleaner.class);

    private final OffsetDateTime threshold;

    public StateCleaner() {
        this(OffsetDateTime.now().minusMonths(3));
    }

    StateCleaner(final OffsetDateTime threshold) {
        this.threshold = threshold;
    }

    private static boolean isOutdated(final StateStorage storage, final String section,
                                      final OffsetDateTime threshold) {
        return storage.getValue(section, Constants.LAST_UPDATED_KEY.getValue())
                .map(date -> OffsetDateTime.parse(date).isBefore(threshold))
                .orElse(true);
    }

    @Override
    public void accept(final Tenant tenant) {
        final String username = tenant.getSessionInfo().getUsername();
        final TenantState state = TenantState.of(new SessionInfo(username));
        final StateStorage storage = state.getStateStorage();
        LOGGER.debug("Starting state cleanup for '{}'.", username);
        synchronized (storage) { // write operations synchronized for the tenant across the application
            final Set<String> toRemove = storage.getSections()
                    .filter(section -> isOutdated(storage, section, threshold))
                    .peek(section -> LOGGER.debug("Will remove section '{}'.", section))
                    .collect(toSet());
            toRemove.forEach(storage::unsetValues);
            final boolean result = storage.store();
            LOGGER.debug("Cleaned up state: {}.", result);
        }
    }

}
