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

import java.util.Collection;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public enum Schedulers {

    INSTANCE;

    private final Logger LOGGER = LoggerFactory.getLogger(Schedulers.class);

    private final Collection<Scheduler> schedulers = new CopyOnWriteArraySet<>();

    public Scheduler create(final Integer parallelism, final ThreadFactory threadFactory) {
        final Scheduler scheduler = new Scheduler(parallelism, threadFactory);
        schedulers.add(scheduler);
        LOGGER.trace("Created {}.", scheduler);
        return scheduler;
    }

    public Scheduler create(final Integer parallelism) {
        return create(parallelism, Executors.defaultThreadFactory());
    }

    public Scheduler create() {
        return create(1);
    }

    boolean destroy(final Scheduler scheduler) {
        LOGGER.trace("Destroyed {}.", scheduler);
        return schedulers.remove(scheduler);
    }

    public void pause() {
        schedulers.forEach(Scheduler::pause);
    }

    public void resume() {
        schedulers.forEach(Scheduler::resume);
    }

}
