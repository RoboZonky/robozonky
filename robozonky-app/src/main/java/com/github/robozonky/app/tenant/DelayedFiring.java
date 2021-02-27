/*
 * Copyright 2021 The RoboZonky Project
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

package com.github.robozonky.app.tenant;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.robozonky.internal.util.functional.Memoizer;

final class DelayedFiring implements Runnable {

    private static final Logger LOGGER = LogManager.getLogger(DelayedFiring.class);

    private final AtomicBoolean isOver = new AtomicBoolean(false);
    private final CyclicBarrier triggersEventFiring = new CyclicBarrier(2);
    private final Supplier<CompletableFuture<Void>> blocksUntilAllUnblock = Memoizer
        .memoize(() -> CompletableFuture.runAsync(() -> {
            try {
                triggersEventFiring.await();
            } catch (final InterruptedException | BrokenBarrierException ex) {
                Thread.currentThread()
                    .interrupt();
                throw new IllegalStateException("Interrupted while waiting for transaction commit.");
            }
        }));
    private final Collection<CompletableFuture<Void>> all = new ArrayList<>(0);

    private void ensureNotOver() {
        if (isOver.get()) {
            throw new IllegalStateException("Already run.");
        }
    }

    public boolean isPending() {
        return !all.isEmpty() && !isOver.get();
    }

    public CompletableFuture delay(final Runnable runnable) {
        ensureNotOver();
        LOGGER.debug("Delaying {}.", runnable);
        final CompletableFuture<Void> result = blocksUntilAllUnblock.get()
            .thenRunAsync(runnable);
        all.add(result);
        return result;
    }

    public void cancel() {
        ensureNotOver();
        blocksUntilAllUnblock.get()
            .cancel(true);
        triggersEventFiring.reset();
        isOver.set(true);
        LOGGER.debug("Cancelled.");
    }

    @Override
    public void run() {
        ensureNotOver();
        LOGGER.debug("Requesting delayed event firing.");
        try {
            if (all.isEmpty()) {
                return;
            }
            LOGGER.trace("Triggering firing.");
            triggersEventFiring.await();
            LOGGER.trace("Waiting for firing to complete.");
            CompletableFuture.allOf(all.toArray(new CompletableFuture[0]))
                .join();
        } catch (final InterruptedException | BrokenBarrierException e) {
            Thread.currentThread()
                .interrupt();
            throw new IllegalStateException("Failed firing events in a transaction.", e);
        } finally {
            isOver.set(true);
            LOGGER.debug("Firing over.");
        }
    }
}
