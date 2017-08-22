/*
 * Copyright 2017 The RoboZonky Project
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

/**
 * Thread factory giving threads RoboZonky-specific names, assigning them to RoboZonky-specific {@link ThreadGroup}s.
 * No other changes are made, although in the future we may decide to alter thread priorities here.
 */
public class RoboZonkyThreadFactory implements ThreadFactory {

    private final ThreadGroup threadGroup;
    private final AtomicInteger nextThreadNumber = new AtomicInteger(1);

    public RoboZonkyThreadFactory(final ThreadGroup group) {
        this.threadGroup = group;
    }

    @Override
    public Thread newThread(final Runnable runnable) {
        final String name = threadGroup.getName() + "-thread-" + nextThreadNumber.getAndIncrement();
        return new Thread(threadGroup, runnable, name);
    }
}
