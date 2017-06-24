/*
 * Copyright 2017 Lukáš Petrovický
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

package com.github.triceo.robozonky.app.investing;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.Semaphore;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import com.github.triceo.robozonky.api.remote.entities.Investment;
import com.github.triceo.robozonky.api.remote.entities.Loan;
import com.github.triceo.robozonky.api.strategies.LoanDescriptor;
import com.github.triceo.robozonky.app.authentication.AuthenticationHandler;
import com.github.triceo.robozonky.common.remote.ApiProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class AbstractInvestmentMode implements InvestmentMode {

    protected final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

    private final boolean isFaultTolerant;
    private final AuthenticationHandler authenticationHandler;
    private final Investor.Builder investorBuilder;

    protected AbstractInvestmentMode(final AuthenticationHandler authenticationHandler,
                                     final Investor.Builder builder,
                                     final boolean isFaultTolerant) {
        this.authenticationHandler = authenticationHandler;
        this.investorBuilder = builder;
        this.isFaultTolerant = isFaultTolerant;
    }

    @Override
    public boolean isFaultTolerant() {
        return isFaultTolerant;
    }

    @Override
    public String getUsername() {
        return this.authenticationHandler.getSecretProvider().getUsername();
    }

    @Override
    public boolean isDryRun() {
        return this.investorBuilder.isDryRun();
    }

    protected Investor.Builder getInvestorBuilder() {
        return investorBuilder;
    }

    protected AuthenticationHandler getAuthenticationHandler() {
        return authenticationHandler;
    }

    protected abstract Optional<Collection<Investment>> execute(final ApiProvider apiProvider);

    @Override
    public Optional<Collection<Investment>> get() {
        return this.execute(new ApiProvider());
    }

    protected boolean wasSuddenDeath() {
        return false; // only daemon can suddenly die
    }

    /**
     * Start marketplace which will be putting marketplace into a given buffer.
     *
     * @param target The buffer to start putting marketplace into.
     */
    protected abstract void openMarketplace(Consumer<Collection<Loan>> target);

    /**
     * Provide the investing algorithm.
     *
     * @param apiProvider API provider to use when constructing the investing mechanism.
     * @return Investments made by the algorithm.
     */
    protected abstract Function<Collection<LoanDescriptor>, Collection<Investment>> getInvestor(ApiProvider apiProvider);

    /**
     * Execute the algorithm and give it a circuit breaker which, when turning true, tells the orchestration to
     * finish the operation and terminate.
     *
     * @param apiProvider The API provider to use when constructing the investing mechanism.
     * @param circuitBreaker Release in order to have this method stop and return.
     * @return Investments made while this method was running, or empty if failure.
     */
    protected Optional<Collection<Investment>> execute(final ApiProvider apiProvider, final Semaphore circuitBreaker) {
        LOGGER.trace("Executing.");
        try {
            final ResultTracker buffer = new ResultTracker();
            final Consumer<Collection<Loan>> investor = (loans) -> {
                final Collection<LoanDescriptor> descriptors = buffer.acceptLoansFromMarketplace(loans);
                final Collection<Investment> result = getInvestor(apiProvider).apply(descriptors);
                buffer.acceptInvestmentsFromRobot(result);
            };
            openMarketplace(investor);
            LOGGER.trace("Will wait for request to stop.");
            circuitBreaker.acquireUninterruptibly(Math.max(1, circuitBreaker.availablePermits()));
            LOGGER.trace("Request to stop received.");
            circuitBreaker.release();
            if (this.wasSuddenDeath()) {
                throw new SuddenDeathException();
            }
            return Optional.of(buffer.getInvestmentsMade());
        } catch (final SuddenDeathException ex) {
            LOGGER.error("Thread stack traces:");
            Thread.getAllStackTraces().forEach((key, value) -> {
                LOGGER.error("Stack trace for thread {}:", key);
                Stream.of(value).forEach(ste -> LOGGER.error("{}", ste));
            });
            throw new IllegalStateException(ex);
        } catch (final Exception ex) {
            LOGGER.error("Failed executing investments.", ex);
            return Optional.empty();
        }
    }

}
