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

package com.github.robozonky.app.daemon;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.ToLongFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.logging.log4j.Logger;

import com.github.robozonky.api.strategies.Descriptor;
import com.github.robozonky.api.strategies.Recommended;
import com.github.robozonky.app.tenant.PowerTenant;
import com.github.robozonky.internal.remote.endpoints.ControlApi;

abstract class AbstractSession<T extends Recommended<T, S, X>, S extends Descriptor<T, S, X>, X> {

    protected final PowerTenant tenant;
    protected final Logger logger;
    protected final List<X> result = new ArrayList<>(0);
    private final SessionState<S> discarded;
    private final Collection<S> stillAvailable;

    protected AbstractSession(final Stream<S> originallyAvailable, final PowerTenant tenant,
            final ToLongFunction<S> idSupplier, final String stateId, final Logger logger) {
        this.tenant = tenant;
        this.stillAvailable = originallyAvailable.collect(Collectors.toList());
        this.discarded = new SessionState<>(tenant, stillAvailable, idSupplier, stateId);
        this.logger = logger;
    }

    /**
     * Get items that are available to be evaluated by the strategy. These are items that come from the marketplace,
     * minus items that are already {@link #discard(Descriptor)}ed.
     * 
     * @return Items in the marketplace in which the user could still potentially invest. Unmodifiable.
     */
    Collection<S> getAvailable() {
        stillAvailable.removeIf(discarded::contains);
        return Collections.unmodifiableCollection(stillAvailable);
    }

    protected void discard(final S item) {
        logger.debug("Will not show {} again.", item);
        discarded.put(item);
    }

    protected boolean isBalanceAcceptable(final T item) {
        return item.amount()
            .compareTo(tenant.getKnownBalanceUpperBound()) <= 0;
    }

    /**
     * Request {@link ControlApi} to invest in a given item.
     * 
     * @param recommendation Item to invest into.
     * @return True if investment successful. The investment is reflected in {@link #getResult()} and removed from
     *         {@link #getAvailable()}.
     */
    protected abstract boolean accept(final T recommendation);

    /**
     * Get investments made during this session.
     * 
     * @return Investments made so far during this session. Unmodifiable.
     */
    Stream<X> getResult() {
        return result.stream();
    }
}
