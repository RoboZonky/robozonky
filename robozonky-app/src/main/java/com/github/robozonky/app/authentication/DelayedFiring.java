/*
 * Copyright 2018 The RoboZonky Project
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

package com.github.robozonky.app.authentication;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicBoolean;

import io.vavr.Lazy;

final class DelayedFiring implements Runnable {

    private final AtomicBoolean isOver = new AtomicBoolean(false);
    private final CyclicBarrier triggersEventFiring = new CyclicBarrier(2);
    private final Lazy<CompletableFuture<Void>> blocksUntilAllUnblock = Lazy.of(() -> CompletableFuture.runAsync(() -> {
        try {
            triggersEventFiring.await();
        } catch (final InterruptedException | BrokenBarrierException ex) {
            throw new IllegalStateException("Interrupted while waiting for transaction commit.");
        }
    }));

    private void ensureNotOver() {
        if (isOver.get()) {
            throw new IllegalStateException("Already run.");
        }
    }

    public boolean isPending() {
        return blocksUntilAllUnblock.isEvaluated() && !isOver.get();
    }

    public CompletableFuture<Void> delay(final Runnable runnable) {
        ensureNotOver();
        return blocksUntilAllUnblock.get().thenRun(runnable);
    }

    public void cancel() {
        ensureNotOver();
        blocksUntilAllUnblock.get().cancel(true);
        isOver.set(true);
    }

    @Override
    public void run() {
        ensureNotOver();
        try {
            triggersEventFiring.await();
        } catch (final InterruptedException | BrokenBarrierException e) {
            throw new IllegalStateException("Failed firing events in a transaction.", e);
        } finally {
            isOver.set(true);
        }
    }
}
