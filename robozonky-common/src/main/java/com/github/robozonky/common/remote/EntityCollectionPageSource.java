/*
 * Copyright 2018 The RoboZonky Project
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
import java.util.function.LongConsumer;

import com.github.robozonky.api.remote.EntityCollectionApi;
import com.github.rutledgepaulv.pagingstreams.PageSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class EntityCollectionPageSource<T> implements PageSource<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(EntityCollectionPageSource.class);

    private final PaginatedApi<T, ? extends EntityCollectionApi<T>> api;
    private final Select select;
    private final Sort<T> ordering;
    private final int pageSize;

    public EntityCollectionPageSource(final PaginatedApi<T, ? extends EntityCollectionApi<T>> api, final Select select,
                                      final Sort<T> ordering, final int pageSize) {
        this.api = api;
        this.select = select;
        this.ordering = ordering;
        this.pageSize = pageSize;
    }

    @Override
    public List<T> fetch(final long offset, final long limit, final LongConsumer totalSizeSink) {
        LOGGER.trace("Requested with offset {}, limit {}.", offset, limit);
        final int pageId = offset < 1 ? 0 : (int) (offset / pageSize);
        // limit is ignored, as the page size determines the page number; offset+limit is not supported by Zonky
        final PaginatedResult<T> result = api.execute(EntityCollectionApi::items, this.select, this.ordering, pageId,
                                                      pageSize);
        totalSizeSink.accept(result.getTotalResultCount());
        return result.getPage();
    }
}
