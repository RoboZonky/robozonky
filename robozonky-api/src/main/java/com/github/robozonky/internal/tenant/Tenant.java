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

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import com.github.robozonky.api.SessionInfo;
import com.github.robozonky.api.remote.entities.Loan;
import com.github.robozonky.api.remote.entities.Restrictions;
import com.github.robozonky.api.remote.entities.SellInfo;
import com.github.robozonky.api.strategies.InvestmentStrategy;
import com.github.robozonky.api.strategies.PurchaseStrategy;
import com.github.robozonky.api.strategies.ReservationStrategy;
import com.github.robozonky.api.strategies.SellStrategy;
import com.github.robozonky.internal.remote.Zonky;
import com.github.robozonky.internal.state.InstanceState;
import com.github.robozonky.internal.util.StreamUtil;

/**
 * Base tenant functionality. All changes made via these methods will be immediately persisted, unless the instance
 * also implements the {@link TransactionalTenant} interface.
 */
public interface Tenant extends AutoCloseable {

    /**
     * Execute an operation using on the Zonky server.
     * 
     * @param operation Operation to execute. It is expected to be stateless and limited solely to the remote call.
     * @param <T>       Return type of the operation.
     * @return Whatever the operation returned.
     */
    <T> T call(final Function<Zonky, T> operation);

    /**
     * Execute an operation using the Zonky server.
     * 
     * @param operation Operation to execute. It is expected to be stateless and limited solely to the remote call.
     */
    default void run(final Consumer<Zonky> operation) {
        call(StreamUtil.toFunction(operation));
    }

    Availability getAvailability();

    /**
     * Provides all relevant data representing user portfolio, such as blocked amounts and wallet balance. This may be
     * cached for a period of time, but it is very important that the data is all loaded at the same time - otherwise
     * the robot will have been operating over an inconsistent view of the data, where a sum of blocked amounts doesn't
     * fully match the available balance.
     * 
     * @return
     */
    RemotePortfolio getPortfolio();

    Restrictions getRestrictions();

    SessionInfo getSessionInfo();

    Optional<InvestmentStrategy> getInvestmentStrategy();

    Optional<SellStrategy> getSellStrategy();

    Optional<PurchaseStrategy> getPurchaseStrategy();

    Optional<ReservationStrategy> getReservationStrategy();

    /**
     * Retrieve a {@link Loan} from Zonky, possibly caching it in the process. If you don't wish to cache it,
     * simply use {@link #call(Function)} to get to {@link Zonky#getLoan(int)}.
     * 
     * @param loanId
     * @return
     */
    default Loan getLoan(final int loanId) {
        return call(zonky -> zonky.getLoan(loanId));
    }

    /**
     * Retrieve a {@link SellInfo} from Zonky, possibly caching it in the process. If you don't wish to cache it,
     * simply use {@link #call(Function)} to get to {@link Zonky#getSellInfo(long)}.
     * 
     * @param investmentId
     * @return
     */
    default SellInfo getSellInfo(final long investmentId) {
        return call(zonky -> zonky.getSellInfo(investmentId));
    }

    <T> InstanceState<T> getState(final Class<T> clz);
}
