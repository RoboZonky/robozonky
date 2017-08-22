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

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.Queue;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;

final class EntitySpliterator<T> implements Spliterator<T> {

    private int currentElementId = 0;
    private Queue<T> currentPage;
    private final Paginated<T> paginated;

    /**
     * This spliterator will never query another page.
     * @param lastPage The last page of results.
     */
    EntitySpliterator(final Collection<T> lastPage) {
        this(new Paginated<T>() {
            @Override
            public boolean nextPage() {
                return false;
            }

            @Override
            public Collection<T> getItemsOnPage() {
                return Collections.unmodifiableCollection(lastPage);
            }

            @Override
            public int getPageId() {
                return 0;
            }

            @Override
            public int getExpectedPageSize() {
                return lastPage.size();
            }

            @Override
            public int getTotalItemCount() {
                return this.getExpectedPageSize();
            }
        });
    }

    /**
     * This spliterator will eventually read all items.
     * @param paginated Individual entities are retrieved from here.
     */
    EntitySpliterator(final Paginated<T> paginated) {
        this.currentPage = new ArrayDeque<>(paginated.getItemsOnPage());
        this.paginated = paginated;
    }

    private synchronized boolean hasMore() {
        return currentElementId < this.paginated.getTotalItemCount();
    }

    @Override
    public synchronized boolean tryAdvance(final Consumer consumer) {
        if (currentPage.isEmpty()) {
            if (this.hasMore()) {
                this.readNewPage();
                return this.tryAdvance(consumer);
            } else {
                return false;
            }
        }
        final T element = this.currentPage.remove();
        currentElementId++;
        consumer.accept(element);
        return true;
    }

    /**
     * Read a new page into the spliterator.
     * @return Old page.
     */
    private synchronized Collection<T> readNewPage() {
        final Collection<T> old = Collections.unmodifiableCollection(this.currentPage);
        if (this.paginated.nextPage()) {
            this.currentPage = new ArrayDeque<>(this.paginated.getItemsOnPage());
        } else {
            this.currentPage = new ArrayDeque<>(0);
        }
        return old;
    }

    @Override
    public synchronized Spliterator trySplit() {
        if (currentPage.isEmpty()) {
            if (this.hasMore()) { // read in a new page and then split
                this.readNewPage();
            } else {
                return Spliterators.emptySpliterator();
            }
        }
        return new EntitySpliterator<>(this.readNewPage());
    }

    @Override
    public synchronized long estimateSize() {
        return this.paginated.getTotalItemCount() - this.currentElementId;
    }

    @Override
    public int characteristics() {
        return Spliterator.IMMUTABLE | Spliterator.NONNULL | Spliterator.SIZED;
    }
}
