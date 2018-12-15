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

package com.github.robozonky.util;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * Thread factory giving threads RoboZonky-specific names, assigning them to RoboZonky-specific {@link ThreadGroup}s.
 * No other changes are made, although in the future we may decide to alter thread priorities here.
 * <p>
 * {@link ThreadGroup}s are autoamtically destroyed when all their threads are stopped. For testing purposes, this is
 * very inconvenient. Therefore, when such a situation is detected, the code will create a new thread group supplied by
 * {@link RoboZonkyThreadFactory#RoboZonkyThreadFactory(Supplier)}.
 */
public class RoboZonkyThreadFactory implements ThreadFactory {

    private final AtomicInteger nextThreadNumber = new AtomicInteger(1);
    private final ManuallyReloadable<ThreadGroup> reloadableThreadGroup;

    public RoboZonkyThreadFactory(final Supplier<ThreadGroup> group) {
        this.reloadableThreadGroup = Reloadable.of(group::get);
    }

    private synchronized ThreadGroup getThreadGroup() {
        final ThreadGroup tg = reloadableThreadGroup.get().getOrNull();
        if (tg == null || tg.isDestroyed()) {
            reloadableThreadGroup.clear();
            return getThreadGroup();
        }
        return tg;
    }

    @Override
    public Thread newThread(final Runnable runnable) {
        final ThreadGroup threadGroup = getThreadGroup();
        final String name = threadGroup.getName() + "-" + nextThreadNumber.getAndIncrement();
        final Thread thread = new Thread(threadGroup, runnable, name);
        thread.setPriority(threadGroup.getMaxPriority()); // use the max priority allowed by the group
        thread.setDaemon(threadGroup.isDaemon());
        return thread;
    }
}
