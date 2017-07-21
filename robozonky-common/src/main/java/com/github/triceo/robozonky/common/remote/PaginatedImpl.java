/*
 * Copyright 2017 Lukáš Petrovický
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

package com.github.triceo.robozonky.common.remote;

import java.util.Collection;
import java.util.Collections;

import com.github.triceo.robozonky.api.remote.EntityCollectionApi;
import com.github.triceo.robozonky.internal.api.Settings;

final class PaginatedImpl<T> implements Paginated<T> {

    private final PaginatedApi<T, ? extends EntityCollectionApi<T>> api;
    private final int pageSize;
    private final Sort<T> ordering;
    private PaginatedResult<T> itemsOnPage;

    PaginatedImpl(final PaginatedApi<T, ? extends EntityCollectionApi<T>> api) {
        this(api, Sort.unspecified(), Settings.INSTANCE.getDefaultApiPageSize()); // for testing purposes
    }

    public PaginatedImpl(final PaginatedApi<T, ? extends EntityCollectionApi<T>> api, final Sort<T> ordering,
                         final int pageSize) {
        this.ordering = ordering;
        this.api = api;
        this.pageSize = pageSize;
        this.nextPage();
    }

    @Override
    public boolean nextPage() {
        this.itemsOnPage = api.execute(EntityCollectionApi::items, this.ordering, this.getPageId() + 1, this.pageSize);
        return this.itemsOnPage != null && !this.itemsOnPage.getPage().isEmpty();
    }

    @Override
    public Collection<T> getItemsOnPage() {
        return this.itemsOnPage == null ? Collections.emptyList() : this.itemsOnPage.getPage();
    }

    @Override
    public int getPageId() {
        return this.itemsOnPage == null ? -1 : this.itemsOnPage.getCurrentPageId();
    }

    @Override
    public int getExpectedPageSize() {
        return this.pageSize;
    }

    @Override
    public int getTotalItemCount() {
        return this.itemsOnPage == null ? 0 : this.itemsOnPage.getTotalResultCount();
    }
}
