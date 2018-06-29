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

package com.github.robozonky.api.notifications;

import com.github.robozonky.api.SessionInfo;

/**
 * Implement in user code to allow handling @{@link Event}s. Different listeners for the same event may be executed
 * by different threads and concurrently. If multiple listeners are somehow interconnected, synchronization between
 * them must be maintained by the listeners themselves.
 * @param <E> Event type to handle.
 */
public interface EventListener<E extends Event> {

    /**
     * Implementation must be thread-safe. Never include any time-consuming logic, since RoboZonky core will block
     * until all of this code is executed. If you need to submit a long-running task, do it asynchronously and do not
     * block on the result.
     * @param event Event that is being listened to.
     * @param sessionInfo Information about the user firing the event.
     */
    void handle(E event, SessionInfo sessionInfo);
}
