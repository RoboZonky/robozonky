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

import java.util.Objects;
import java.util.StringJoiner;

import com.github.robozonky.api.remote.entities.Instalments;

public class InstalmentsImpl implements Instalments {

    private int total;
    private int unpaid;

    public InstalmentsImpl() {
        // For JSON-B.
    }

    public InstalmentsImpl(int total) {
        this(total, total);
    }

    public InstalmentsImpl(int total, int unpaid) {
        this.total = total;
        this.unpaid = unpaid;
    }

    @Override
    public int getTotal() {
        return total;
    }

    public void setTotal(final int total) {
        this.total = total;
    }

    @Override
    public int getUnpaid() {
        return unpaid;
    }

    public void setUnpaid(final int unpaid) {
        this.unpaid = unpaid;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", InstalmentsImpl.class.getSimpleName() + "[", "]")
            .add("total=" + total)
            .add("unpaid=" + unpaid)
            .toString();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || !Objects.equals(getClass(), o.getClass())) {
            return false;
        }
        final InstalmentsImpl that = (InstalmentsImpl) o;
        return total == that.total &&
                unpaid == that.unpaid;
    }

    @Override
    public int hashCode() {
        return Objects.hash(total, unpaid);
    }
}
