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

package com.github.robozonky.common.tenant;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import com.github.robozonky.api.SessionInfo;
import com.github.robozonky.api.remote.entities.Restrictions;
import com.github.robozonky.api.remote.entities.sanitized.Loan;
import com.github.robozonky.api.remote.enums.OAuthScope;
import com.github.robozonky.api.strategies.InvestmentStrategy;
import com.github.robozonky.api.strategies.PurchaseStrategy;
import com.github.robozonky.api.strategies.ReservationStrategy;
import com.github.robozonky.api.strategies.SellStrategy;
import com.github.robozonky.common.remote.Zonky;
import com.github.robozonky.common.state.InstanceState;
import com.github.robozonky.util.StreamUtil;

/**
 * Base tenant functionality. All changes made via these methods will be immediately persisted, unless the instance
 * also implements the {@link TransactionalTenant} interface.
 */
public interface Tenant extends AutoCloseable {

    /**
     * Execute an operation using on the Zonky server, using the default scope.
     * @param operation Operation to execute. Should be stateless. Should also only contain the blocking operation and
     * nothing else, since the underlying code will block the thread for the entire duration of that operation.
     * @param <T> Return type of the operation.
     * @return Whatever the operation returned.
     */
    default <T> T call(final Function<Zonky, T> operation) {
        return call(operation, OAuthScope.SCOPE_APP_WEB);
    }

    /**
     * Execute an operation using on the Zonky server.
     * @param operation Operation to execute. Should be stateless. Should also only contain the blocking operation and
     * nothing else, since the underlying code will block the thread for the entire duration of that operation.
     * @param scope The scope of access to request with the Zonky server.
     * @param <T> Return type of the operation.
     * @return Whatever the operation returned.
     */
    <T> T call(Function<Zonky, T> operation, OAuthScope scope);

    /**
     * Execute an operation using on the Zonky server, using the default scope.
     * @param operation Operation to execute. Should be stateless. Should also only contain the blocking operation and
     * nothing else, since the underlying code will block the thread for the entire duration of that operation.
     */
    default void run(final Consumer<Zonky> operation) {
        run(operation, OAuthScope.SCOPE_APP_WEB);
    }

    /**
     * Execute an operation using on the Zonky server.
     * @param operation Operation to execute. Should be stateless. Should also only contain the blocking operation and
     * nothing else, since the underlying code will block the thread for the entire duration of that operation.
     * @param scope The scope of access to request with the Zonky server.
     */
    default void run(final Consumer<Zonky> operation, final OAuthScope scope) {
        call(StreamUtil.toFunction(operation), scope);
    }

    /**
     * Check that the tenant can be operated on, using the default scope.
     * @return False in cases such as when the user's authentication credentials are being refreshed and therefore
     * the present authentication may already be invalid, without the new one being available yet.
     */
    default boolean isAvailable() {
        return isAvailable(OAuthScope.SCOPE_APP_WEB);
    }

    /**
     * Check that the tenant can be operated on.
     * @param scope The scope of access with the Zonky server.
     * @return False in cases such as when the user's authentication credentials are being refreshed and therefore
     * the present authentication may already be invalid, without the new one being available yet.
     */
    boolean isAvailable(OAuthScope scope);

    /**
     * Provides all relevant data representing user portfolio, such as blocked amounts and wallet balance. This may be
     * cached for a period of time, but it is very important that the data is all loaded at the same time - otherwise
     * the robot will have been operating over an inconsistent view of the data, where a sum of blocked amounts doesn't
     * fully match the available balance.
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
     * Retrieve a {@link Loan} from Zonky, possibly caching it in the process. If you don't wish to cache the loan,
     * simply use {@link #call(Function)} to get to {@link Zonky#getLoan(int)}.
     * @param loanId
     * @return
     */
    Loan getLoan(final int loanId);

    <T> InstanceState<T> getState(final Class<T> clz);
}
