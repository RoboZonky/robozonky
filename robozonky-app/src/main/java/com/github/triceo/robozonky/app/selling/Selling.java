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

package com.github.triceo.robozonky.app.selling;

import java.util.function.Consumer;

import com.github.triceo.robozonky.api.Refreshable;
import com.github.triceo.robozonky.api.strategies.SellStrategy;
import com.github.triceo.robozonky.common.remote.Zonky;

public class Selling implements Consumer<Zonky> {

    private final Refreshable<SellStrategy> strategy;
    private final boolean isDryRun;

    public Selling(final Refreshable<SellStrategy> strategy, final boolean isDryRun) {
        this.strategy = strategy;
        this.isDryRun = isDryRun;
    }

    @Override
    public void accept(final Zonky zonky) {

    }
}
