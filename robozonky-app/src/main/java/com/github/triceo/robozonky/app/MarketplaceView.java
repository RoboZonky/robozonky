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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import com.github.triceo.robozonky.remote.Loan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Decides whether or not the application should fall asleep because of general marketplace inactivity. Uses two sources
 * of data to make the decision: the marketplace, and the app's internal state concerning the last time the marketplace
 * was checked.
 */
public class MarketplaceView {

    private static final Logger LOGGER = LoggerFactory.getLogger(MarketplaceView.class);

    private final int closedSeasonInSeconds;
    private final int sleepIntervalInMinutes;
    private final Path state;
    private final Marketplace marketplace;

    MarketplaceView(final AppContext ctx, final Marketplace marketplace, final Path state) {
        this.closedSeasonInSeconds = ctx.getCaptchaDelayInSeconds();
        this.sleepIntervalInMinutes = ctx.getSleepPeriodInMinutes();
        this.marketplace = marketplace;
        this.state = state;
    }

    private Instant getLatestMarketplaceAction() {
        if (!this.state.toFile().exists()) {
            return Instant.EPOCH;
        }
        try (final BufferedReader reader = Files.newBufferedReader(this.state, StandardCharsets.UTF_8)) {
            final String instantString = reader.readLine();
            MarketplaceView.LOGGER.debug("Marketplace last checked on {},", instantString);
            return Instant.parse(instantString);
        } catch (final IOException ex) {
            MarketplaceView.LOGGER.debug("Failed read marketplace timestamp.", ex);
            return Instant.EPOCH;
        }
    }

    private Instant getLastActionableLoanPublishing() {
        return this.marketplace.getLoansOlderThan(closedSeasonInSeconds).stream()
                .findFirst()
                .map(Loan::getDatePublished)
                .orElse(Instant.now());
    }

    /**
     * Retrieves loans that are available for robotic investing, ie. not protected by CAPTCHA.
     *
     * @return Loans ordered by their time of publishing, descending.
     */
    public List<Loan> getAvailableLoans() {
        return this.marketplace.getLoansOlderThan(closedSeasonInSeconds);
    }

    /**
     * Whether or not the application should fall asleep and not make any further contact with API.
     *
     * @return True if no further contact should be made during this run of the app.
     */
    public boolean shouldSleep() {
        final Instant lastKnownMarketplaceAction = this.getLatestMarketplaceAction();
        boolean shouldSleep = true;
        if (!this.marketplace.getLoansNewerThan(lastKnownMarketplaceAction).isEmpty()) {
            // try investing since there are loans we haven't seen yet
            MarketplaceView.LOGGER.debug("Will not sleep due to new loans.");
            shouldSleep = false;
        } else if (lastKnownMarketplaceAction.plus(sleepIntervalInMinutes, ChronoUnit.MINUTES).isAfter(Instant.now())) {
            // try investing since we haven't tried in a while; maybe we have some more funds now
            MarketplaceView.LOGGER.debug("Will not sleep due to already sleeping too much.");
            shouldSleep = false;
        }
        if (shouldSleep) {
            /*
             * only persist (= change marketplace check timestamp) when we're intending to execute some actual
             * investing, otherwise the pre-configured awakening would never happen.
             */
            this.persist(lastKnownMarketplaceAction);
        }
        return shouldSleep;
    }

    private void persist(final Instant stateBasedLastAction) {
        try (final BufferedWriter writer = Files.newBufferedWriter(this.state, StandardCharsets.UTF_8)) {
            final Instant marketplaceBasedLastAction = this.getLastActionableLoanPublishing();
            final Instant moreRecent = marketplaceBasedLastAction.isAfter(stateBasedLastAction)
                    ? marketplaceBasedLastAction : stateBasedLastAction;
            final String result = moreRecent.toString();
            MarketplaceView.LOGGER.debug("New marketplace last checked time is {},", result);
            writer.write(result);
        } catch (final IOException ex) {
            MarketplaceView.LOGGER.info("Failed write marketplace timestamp, sleep feature will be disabled.", ex);
            try { // failed to write new data, try to delete stale data
                Files.delete(this.state);
            } catch (final IOException ex2) {
                MarketplaceView.LOGGER.warn("Failed delete marketplace timestamp, sleep feature may malfunction.", ex2);
            }
        }
    }
}
