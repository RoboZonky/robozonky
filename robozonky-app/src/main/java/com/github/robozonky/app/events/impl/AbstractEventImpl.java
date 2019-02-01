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

package com.github.robozonky.app.events.impl;

import java.time.OffsetDateTime;
import java.util.stream.Stream;

import com.github.robozonky.api.notifications.Event;
import com.github.robozonky.common.async.Reloadable;
import com.github.robozonky.internal.util.DateUtil;
import com.github.robozonky.internal.util.ToStringBuilder;

/**
 * Mandatory parent for any event that may be fired any time during RoboZonky's runtime.
 * <p>
 * Subclasses must make sure that their class name ends with "Event", or else the default constructor of this class
 * will throw an exception.
 */
abstract class AbstractEventImpl implements Event {

    private final OffsetDateTime creationDateTime = DateUtil.offsetNow();
    private final Reloadable<String> toString;
    private OffsetDateTime conceptionDateTime = creationDateTime;

    protected AbstractEventImpl(final String... toStringIgnoredFields) {
        final String[] ignored = Stream.concat(Stream.of("toString"), Stream.of(toStringIgnoredFields))
                .toArray(String[]::new);
        this.toString = Reloadable.with(() -> ToStringBuilder.createFor(this, ignored))
                .build();
    }

    public OffsetDateTime getCreatedOn() {
        return creationDateTime;
    }

    @Override
    public OffsetDateTime getConceivedOn() {
        return conceptionDateTime;
    }

    public void setConceivedOn(final OffsetDateTime offsetDateTime) {
        conceptionDateTime = offsetDateTime;
        toString.clear(); // toString() will have changed by this
    }

    @Override
    public final String toString() {
        return toString.get().getOrElse("ERROR");
    }
}
