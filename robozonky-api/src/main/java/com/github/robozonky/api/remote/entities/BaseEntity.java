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

package com.github.robozonky.api.remote.entities;

import java.util.HashSet;
import java.util.Set;
import javax.xml.bind.annotation.XmlTransient;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.github.robozonky.internal.util.ToStringBuilder;
import io.vavr.Lazy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * All JAX-RS entity classes in this package should extend this class in order to be able to gracefully handle
 * missing JSON properties. This happens occasionally when Zonky deploys a new version of the API and the app was not
 * yet updated with the changes.
 */
abstract class BaseEntity {

    private static final Set<String> CHANGES_ALREADY_NOTIFIED = new HashSet<>(0);
    private static final Logger LOGGER = LogManager.getLogger(BaseEntity.class);
    @XmlTransient
    private final Lazy<String> toString = Lazy.of(() -> ToStringBuilder.createFor(this, "toString"));

    private boolean hasBeenCalledBefore(final String key) {
        final String id = this.getClass().getCanonicalName() + ":" + key;
        return !CHANGES_ALREADY_NOTIFIED.add(id);
    }

    @JsonAnyGetter
    void handleUnknownGetter(final String key) {
        if (!hasBeenCalledBefore(key)) {
            LOGGER.info("Trying to get value of unknown property '{}' on {}. Indicates an unexpected API change.", key,
                        this.getClass());
        }
    }

    @JsonAnySetter
    void handleUnknownSetter(final String key, final Object value) {
        if (!hasBeenCalledBefore(key)) {
            LOGGER.info("Trying to set value '{}' to unknown property '{}' on {}. Indicates an unexpected API change.",
                        value, key, this.getClass());
        }
    }

    @Override
    public String toString() {
        return toString.get();
    }
}
