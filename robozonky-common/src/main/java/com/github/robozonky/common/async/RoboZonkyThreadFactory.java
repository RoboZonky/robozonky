/*
 * Copyright 2019 The RoboZonky Project
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

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Thread factory giving threads RoboZonky-specific names, assigning them to RoboZonky-specific {@link ThreadGroup}s,
 * marking them as daemon threads and assigning thread priorities as high as the {@link ThreadGroup} will allow
 */
final class RoboZonkyThreadFactory implements ThreadFactory {

    private final AtomicInteger nextThreadNumber = new AtomicInteger(1);
    private final ThreadGroup threadGroup;

    public RoboZonkyThreadFactory(final ThreadGroup group) {
        this.threadGroup = group;
    }

    @Override
    public Thread newThread(final Runnable runnable) {
        final String name = threadGroup.getName() + "-" + nextThreadNumber.getAndIncrement();
        final Thread thread = new Thread(threadGroup, runnable, name);
        thread.setPriority(Thread.MAX_PRIORITY); // the actual priority will be determined by the thread group
        thread.setDaemon(true);
        return thread;
    }
}
