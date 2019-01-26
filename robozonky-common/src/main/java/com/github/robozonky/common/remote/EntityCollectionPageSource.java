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

package com.github.robozonky.common.remote;

import java.util.List;
import java.util.function.Function;
import java.util.function.LongConsumer;

import com.github.rutledgepaulv.pagingstreams.PageSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

class EntityCollectionPageSource<T, S> implements PageSource<T> {

    private static final Logger LOGGER = LogManager.getLogger(EntityCollectionPageSource.class);

    private final PaginatedApi<T, S> api;
    private final Function<S, List<T>> function;
    private final Select select;
    private final int pageSize;

    public EntityCollectionPageSource(final PaginatedApi<T, S> api, final Function<S, List<T>> function,
                                      final Select select, final int pageSize) {
        this.api = api;
        this.function = function;
        this.select = select;
        this.pageSize = pageSize;
    }

    @Override
    public List<T> fetch(final long offset, final long limit, final LongConsumer totalSizeSink) {
        LOGGER.trace("Requested with offset {}, limit {}.", offset, limit);
        final int pageId = offset < 1 ? 0 : (int) (offset / pageSize);
        // limit is ignored, as the page size determines the page number; offset+limit is not supported by Zonky
        final PaginatedResult<T> result = api.execute(function, this.select, pageId, pageSize);
        totalSizeSink.accept(result.getTotalResultCount());
        return result.getPage();
    }
}
