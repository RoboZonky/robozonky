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

interface Paginated<T> {

    /**
     * Load another page in, executing a query to the remote API.
     *
     * @return True if the next page has been retrieved and contains results.
     */
    boolean nextPage();

    /**
     *
     * @return Items retrieved via the last {@link #nextPage()} call.
     */
    Collection<T> getItemsOnPage();

    /**
     * Number of the current page.
     *
     * @return 0 is first page. -1 when {@link #nextPage()} not yet called.
     */
    int getPageId();

    /**
     *
     * @return Maximum expected amount of items in {@link #getItemsOnPage()}.
     */
    int getExpectedPageSize();

    /**
     *
     * @return Total number of items available in all pages.
     */
    int getTotalItemCount();

}
