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

package com.github.robozonky.common.jobs;

import java.time.Duration;

import com.github.robozonky.internal.util.RandomUtil;

public interface Job {

    /**
     * How long to wait from when this method is called before first running the payload.
     * @return By default, this returns a random duration of less than 1000 seconds.
     */
    default Duration startIn() {
        final long randomSeconds = RandomUtil.getNextInt(1000);
        return Duration.ofSeconds(randomSeconds);
    }

    /**
     * How much time to leave between one task ending and the other starting.
     * @return
     */
    Duration repeatEvery();

    /**
     * Longest possible time duration that the task will be allowed to run for. A ceiling of 1 hour will be enforced.
     * @return
     */
    default Duration killIn() {
        return Duration.ofMinutes(1);
    }

    /**
     *
     * @return If true, signals to the calling code that this task should be given a priority thread, if possible.
     */
    default boolean prioritize() {
        return false;
    }

}
