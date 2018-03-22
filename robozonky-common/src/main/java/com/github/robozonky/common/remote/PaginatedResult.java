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

package com.github.robozonky.common.remote;

import java.util.Collections;
import java.util.List;

final class PaginatedResult<S> {

    private final List<S> result;
    private final int totalSize;

    public PaginatedResult(final List<S> result, final int totalSize) {
        this.result = result;
        this.totalSize = totalSize;
    }

    public List<S> getPage() {
        return Collections.unmodifiableList(result);
    }

    public int getTotalResultCount() {
        return totalSize;
    }
}
