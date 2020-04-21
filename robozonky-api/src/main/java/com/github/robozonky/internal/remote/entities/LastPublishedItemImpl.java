/*
 * Copyright 2020 The RoboZonky Project
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

package com.github.robozonky.internal.remote.entities;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.StringJoiner;

import com.github.robozonky.api.remote.entities.LastPublishedItem;
import com.github.robozonky.internal.test.DateUtil;

public class LastPublishedItemImpl implements LastPublishedItem {

    private long id;
    // Expensive to deserialize, do it on-demand.
    private String datePublished;

    public LastPublishedItemImpl() {
        // For JSON-B.
    }

    public LastPublishedItemImpl(final long id) {
        this(id, DateUtil.offsetNow());
    }

    public LastPublishedItemImpl(final long id, final OffsetDateTime datePublished) {
        this.id = id;
        this.datePublished = datePublished.toString();
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public OffsetDateTime getDatePublished() {
        return OffsetDateTime.parse(datePublished);
    }

    public void setId(final long id) {
        this.id = id;
    }

    public void setDatePublished(final OffsetDateTime datePublished) {
        this.datePublished = datePublished.toString();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || !Objects.equals(getClass(), o.getClass())) {
            return false;
        }
        final LastPublishedItemImpl that = (LastPublishedItemImpl) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", LastPublishedItemImpl.class.getSimpleName() + "[", "]")
            .add("id=" + id)
            .add("datePublished=" + datePublished)
            .toString();
    }
}
