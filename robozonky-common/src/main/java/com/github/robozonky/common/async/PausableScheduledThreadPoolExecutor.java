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

package com.github.robozonky.common.async;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PausableScheduledThreadPoolExecutor extends ScheduledThreadPoolExecutor implements
                                                                                     PausableScheduledExecutorService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PausableScheduledThreadPoolExecutor.class);

    private final ReentrantLock pauseLock = new ReentrantLock();
    private final Condition unpaused = pauseLock.newCondition();
    private boolean isPaused;

    public PausableScheduledThreadPoolExecutor(final int corePoolSize) {
        super(corePoolSize);
    }

    public PausableScheduledThreadPoolExecutor(final int corePoolSize, final ThreadFactory threadFactory) {
        super(corePoolSize, threadFactory);
    }

    public PausableScheduledThreadPoolExecutor(final int corePoolSize, final RejectedExecutionHandler handler) {
        super(corePoolSize, handler);
    }

    public PausableScheduledThreadPoolExecutor(final int corePoolSize, final ThreadFactory threadFactory,
                                               final RejectedExecutionHandler handler) {
        super(corePoolSize, threadFactory, handler);
    }

    @Override
    protected void beforeExecute(final Thread t, final Runnable r) {
        super.beforeExecute(t, r);
        pauseLock.lock();
        try {
            while (isPaused) {
                unpaused.await();
            }
        } catch (final InterruptedException ex) {
            t.interrupt();
        } finally {
            pauseLock.unlock();
        }
    }

    @Override
    public void pause() {
        pauseLock.lock();
        try {
            isPaused = true;
            LOGGER.trace("Paused {}.", this);
        } finally {
            pauseLock.unlock();
        }
    }

    @Override
    public void resume() {
        pauseLock.lock();
        try {
            isPaused = false;
            unpaused.signalAll();
            LOGGER.trace("Resumed {}.", this);
        } finally {
            pauseLock.unlock();
        }
    }
}
