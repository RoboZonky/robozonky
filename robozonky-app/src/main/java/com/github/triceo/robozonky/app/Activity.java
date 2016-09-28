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
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import com.github.triceo.robozonky.remote.Api;
import com.github.triceo.robozonky.remote.Loan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Decides whether or not the application should fall asleep because of general marketplace inactivity. Uses two sources
 * of data to make the decision: the marketplace, and the app's internal state concerning the last time the marketplace
 * was checked.
 *
 * In order for the state to be persisted, the App needs to eventually call {@link #settle()} after calling
 * {@link #shouldSleep()}.
 */
class Activity {

    /**
     * Simple abstraction over {@link Api#getLoans()} which provides additional intelligence. Use {@link #from(Api)} as
     * entry point to the API.
     */
    private static class Marketplace {

        /**
         * Instantiate the marketplace.
         *
         * @param api Remote API from which to load all loans.
         * @return Marketplace backed by the API.
         */
        public static Activity.Marketplace from(final Api api) {
            return new Activity.Marketplace(api.getLoans());
        }

        private final List<Loan> recentLoansDescending;

        private Marketplace(final Collection<Loan> loans) { // Zotify occasionally returns null loans for unknown reason
            this.recentLoansDescending = loans == null ? Collections.emptyList() :
                    Collections.unmodifiableList(loans.stream()
                            .filter(l -> l.getRemainingInvestment() > 0)
                            .sorted(Comparator.comparing(Loan::getDatePublished).reversed())
                            .collect(Collectors.toList()));
        }

        /**
         * Retrieve all loans in the marketplace which have not yet been fully funded and which have been published at least
         * a certain time ago.
         * @param delayInSeconds How long ago at the very least should the loans have been published.
         * @return Ordered by publishing time descending.
         */
        public List<Loan> getLoansOlderThan(final int delayInSeconds) {
            return this.recentLoansDescending.stream()
                    .filter(l -> Instant.now().isAfter(l.getDatePublished().plus(delayInSeconds, ChronoUnit.SECONDS)))
                    .collect(Collectors.toList());
        }

        /**
         * Retrieve all loans in the marketplace which have not yet been fully funded and which have been published past a
         * certain point in time.
         * @param instant The earliest point in time for the loans to published on.
         * @return Ordered by publishing time descending.
         */
        public List<Loan> getLoansNewerThan(final Instant instant) {
            return this.recentLoansDescending.stream()
                    .filter(l -> l.getDatePublished().isAfter(instant))
                    .collect(Collectors.toList());
        }

    }

    private static final Logger LOGGER = LoggerFactory.getLogger(Activity.class);

    private final int closedSeasonInSeconds;
    private final int sleepIntervalInMinutes;
    private final Path state;
    private final Activity.Marketplace marketplace;
    private Runnable settler = null;

    Activity(final AppContext ctx, final Api api, final Path state) {
        this.closedSeasonInSeconds = ctx.getCaptchaDelayInSeconds();
        this.sleepIntervalInMinutes = ctx.getSleepPeriodInMinutes();
        this.marketplace = Activity.Marketplace.from(api);
        this.state = state;
    }

    private boolean wasMarketplaceCheckedBefore() {
        return this.state.toFile().exists();
    }

    private Instant getLatestMarketplaceAction() {
        if (!this.wasMarketplaceCheckedBefore()) {
            return Instant.EPOCH;
        }
        try (final BufferedReader reader = Files.newBufferedReader(this.state, StandardCharsets.UTF_8)) {
            final String instantString = reader.readLine();
            return Instant.parse(instantString);
        } catch (final IOException ex) {
            Activity.LOGGER.debug("Failed read marketplace timestamp.", ex);
            return Instant.EPOCH;
        }
    }

    private List<Loan> getUnactionableLoans() {
        return this.marketplace.getLoansNewerThan(Instant.now().minus(this.closedSeasonInSeconds, ChronoUnit.SECONDS));
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
        final boolean hasUnactionableLoans = !this.getUnactionableLoans().isEmpty();
        Activity.LOGGER.debug("Marketplace last checked on {}, has un-actionable loans: {}.",
                lastKnownMarketplaceAction, hasUnactionableLoans);
        boolean shouldSleep = true;
        if (!this.marketplace.getLoansNewerThan(lastKnownMarketplaceAction).isEmpty()) {
            // try investing since there are loans we haven't seen yet
            Activity.LOGGER.debug("Will not sleep due to new loans.");
            shouldSleep = false;
        } else if (lastKnownMarketplaceAction.plus(sleepIntervalInMinutes, ChronoUnit.MINUTES).isBefore(Instant.now())) {
            // try investing since we haven't tried in a while; maybe we have some more funds now
            Activity.LOGGER.debug("Will not sleep due to already sleeping too much.");
            shouldSleep = false;
        }
        synchronized (this) { // do not allow concurrent modification of the settler variable
            if (this.settler != null) {
                Activity.LOGGER.warn("Scrapping unsettled activity.");
            }
            if (!shouldSleep || hasUnactionableLoans) {
            /*
             * only persist (= change marketplace check timestamp) when we're intending to execute some actual
             * investing.
             */
                this.settler = () -> this.persist(hasUnactionableLoans);
            } else {
                this.settler = null;
            }
        }
        return shouldSleep;
    }

    /**
     * Persists the new marketplace state following a {@link #shouldSleep()} call.
     */
    public synchronized void settle() {
        if (this.settler == null) {
            Activity.LOGGER.debug("No activity to settle.");
        } else {
            this.settler.run();
            this.settler = null;
        }
    }

    private void persist(final boolean hasUnactionableLoans) {
        try (final BufferedWriter writer = Files.newBufferedWriter(this.state, StandardCharsets.UTF_8)) {
            // make sure the unactionable loans are never included in the time the marketplace was last checked
            final Instant result = hasUnactionableLoans ?
                    Instant.now().minus(this.closedSeasonInSeconds + 30, ChronoUnit.SECONDS)
                    : Instant.now();
            if (hasUnactionableLoans) {
                Activity.LOGGER.debug("New marketplace last checked time placed before beginning of closed season: {}.",
                        result);
            } else {
                Activity.LOGGER.debug("New marketplace last checked time is {}.", result);
            }
            writer.write(result.toString());
        } catch (final IOException ex) {
            Activity.LOGGER.info("Failed write marketplace timestamp, sleep feature will be disabled.", ex);
            try { // failed to write new data, try to delete stale data
                Files.delete(this.state);
            } catch (final IOException ex2) {
                Activity.LOGGER.warn("Failed delete marketplace timestamp, sleep feature may malfunction.", ex2);
            }
        }
    }
}
