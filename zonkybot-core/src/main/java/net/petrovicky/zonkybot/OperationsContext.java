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
package net.petrovicky.zonkybot;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import net.petrovicky.zonkybot.remote.ZonkyAPI;
import net.petrovicky.zonkybot.strategy.InvestmentStrategy;

public final class OperationsContext {

    private final ZonkyAPI api;
    private final InvestmentStrategy strategy;
    private final boolean dryRun;
    private final int dryRunInitialBalance;
    private final ExecutorService backgroundExecutor;

    public OperationsContext(final ZonkyAPI api, final InvestmentStrategy strategy, final boolean dryRun,
                             final int dryRunInitialBalance, final int maxNumberParallelHttpConnections) {
        this.api = api;
        this.strategy = strategy;
        this.dryRun = dryRun;
        this.dryRunInitialBalance = dryRunInitialBalance;
        this.backgroundExecutor = Executors.newFixedThreadPool(maxNumberParallelHttpConnections - 1);
    }

    protected ExecutorService getBackgroundExecutor() {
        return this.backgroundExecutor;
    }

    protected ZonkyAPI getAPI() {
        if (backgroundExecutor.isShutdown()) {
            throw new IllegalStateException("OperationsContext already disposed of.");
        }
        return this.api;
    }

    public InvestmentStrategy getStrategy() {
        return this.strategy;
    }

    public boolean isDryRun() {
        return this.dryRun;
    }

    public int getDryRunInitialBalance() {
        return this.dryRunInitialBalance;
    }

    protected void dispose() {
        backgroundExecutor.shutdownNow();
    }
}
