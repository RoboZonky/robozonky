/*
 *
 *  * Copyright 2016 Lukáš Petrovický
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 * /
 */
package com.github.triceo.robozonky;

import java.math.BigDecimal;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.github.triceo.robozonky.authentication.Authentication;
import com.github.triceo.robozonky.strategy.InvestmentStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Carries information to enable {@link Operations} to function, such as an authenticated Zonky API.
 * <p>
 * When the class instance is no longer needed, it must be {@link #dispose()}d - otherwise the #ExecutorService may
 * prevent the application from ending.
 */
public class OperationsContext {

    private static final Logger LOGGER = LoggerFactory.getLogger(OperationsContext.class);

    private final Authentication authenticationInfo;
    private final InvestmentStrategy strategy;
    private final boolean dryRun;
    private final BigDecimal dryRunInitialBalance;
    private final ExecutorService backgroundExecutor;

    public OperationsContext(final Authentication authenticated, final InvestmentStrategy strategy,
                             final boolean dryRun, final int dryRunInitialBalance, final int maxNumberParallelHttpConnections) {
        this.authenticationInfo = authenticated;
        this.strategy = strategy;
        this.dryRun = dryRun;
        this.dryRunInitialBalance = BigDecimal.valueOf(dryRunInitialBalance);
        this.backgroundExecutor = Executors.newFixedThreadPool(maxNumberParallelHttpConnections - 1);
        OperationsContext.LOGGER.debug("OperationsContext initialized.");
    }

    /**
     * Retrieve executor service to be used for background network communication. Filling this service with non-related
     * tasks will prevent RoboZonky from querying Zonky API.
     * @return
     */
    protected ExecutorService getBackgroundExecutor() {
        return this.backgroundExecutor;
    }

    public Authentication getAuthentication() {
        return authenticationInfo;
    }

    public InvestmentStrategy getStrategy() {
        return this.strategy;
    }

    public boolean isDryRun() {
        return this.dryRun;
    }

    public BigDecimal getDryRunInitialBalance() {
        return this.dryRunInitialBalance;
    }

    protected void dispose() {
        backgroundExecutor.shutdownNow();
        OperationsContext.LOGGER.debug("OperationsContext disposed of.");
    }
}
