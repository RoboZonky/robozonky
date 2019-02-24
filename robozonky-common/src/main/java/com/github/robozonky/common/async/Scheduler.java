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

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledFuture;

@SuppressWarnings("rawtypes")
public interface Scheduler extends AutoCloseable {

    ScheduledFuture<?> submit(final Runnable toSchedule, final Duration delayInBetween);

    ScheduledFuture<?> submit(final Runnable toSchedule, final Duration delayInBetween, final Duration firstDelay);

    boolean isClosed();

    ExecutorService getExecutor();
}
