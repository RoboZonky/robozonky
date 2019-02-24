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

final class TaskConstants {

    static final ThreadFactory SCHEDULING_THREAD_FACTORY = threadFactory("rzScheduling", Thread.MAX_PRIORITY);
    static final ThreadFactory REALTIME_THREAD_FACTORY = threadFactory("rzRealtime", Thread.MAX_PRIORITY);
    static final ThreadFactory SUPPORTING_THREAD_FACTORY = threadFactory("rzSupporting", Thread.NORM_PRIORITY);
    static final ThreadFactory BACKGROUND_THREAD_FACTORY = threadFactory("rzBackground", Thread.MIN_PRIORITY);

    private TaskConstants() {
        // no instances
    }

    private static ThreadFactory threadFactory(final String name, final int maxPriority) {
        final ThreadGroup threadGroup = new ThreadGroup(name);
        threadGroup.setMaxPriority(maxPriority);
        return new RoboZonkyThreadFactory(threadGroup);
    }
}
