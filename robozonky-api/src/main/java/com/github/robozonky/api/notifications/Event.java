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

import java.time.OffsetDateTime;

import com.github.robozonky.internal.api.ToStringBuilder;

/**
 * Mandatory parent for any event that may be fired any time during RoboZonky's runtime.
 * <p>
 * Subclasses must make sure that their class name ends with "Event", or else the default constructor of this class
 * will throw an exception.
 */
public abstract class Event {

    private final OffsetDateTime creationDateTime = OffsetDateTime.now();

    public Event() {
        if (!this.getClass().getSimpleName().endsWith("Event")) { // guarantee for dependent code
            throw new IllegalStateException("Event subclass' names must end with 'Event'.");
        }
    }

    public OffsetDateTime getCreatedOn() {
        return creationDateTime;
    }

    @Override
    public final String toString() {
        return new ToStringBuilder(this).toString();
    }
}
