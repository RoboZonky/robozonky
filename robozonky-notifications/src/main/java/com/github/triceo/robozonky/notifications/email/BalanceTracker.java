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

package com.github.triceo.robozonky.notifications.email;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.OptionalInt;
import java.util.concurrent.atomic.AtomicInteger;

import com.github.triceo.robozonky.api.Defaults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

enum BalanceTracker {

    INSTANCE; // fast thread-safe singleton

    private static final Logger LOGGER = LoggerFactory.getLogger(BalanceTracker.class);
    final static File BALANCE_STORE = new File("robozonky.lastKnownBalance");

    private final AtomicInteger knownBalance = new AtomicInteger(-1);

    public OptionalInt getLastKnownBalance() {
        if (knownBalance.get() < 0) {
            if (!BalanceTracker.BALANCE_STORE.exists()) {
                BalanceTracker.LOGGER.debug("No last known balance.");
                return OptionalInt.empty();
            } else try {
                final byte[] contents = Files.readAllBytes(BalanceTracker.BALANCE_STORE.toPath());
                final String stringContents = new String(contents, Defaults.CHARSET).trim();
                final int result = Integer.parseInt(stringContents);
                this.knownBalance.set(result);
            } catch (final Exception ex) {
                BalanceTracker.LOGGER.debug("Failed initializing balance.", ex);
                this.reset();
                return OptionalInt.empty();
            }
        }
        return OptionalInt.of(this.knownBalance.get());
    }

    public void setLastKnownBalance(final int newBalance) {
        try {
            Files.write(BalanceTracker.BALANCE_STORE.toPath(), String.valueOf(newBalance).getBytes(Defaults.CHARSET));
            this.knownBalance.set(newBalance);
        } catch (final IOException ex) {
            BalanceTracker.LOGGER.debug("Balance {},- CZK not stored, using {},- CZK.", newBalance,
                    this.knownBalance.get(), ex);
            this.reset();
        }
    }

    boolean reset() {
        return BalanceTracker.BALANCE_STORE.delete();
    }

}
