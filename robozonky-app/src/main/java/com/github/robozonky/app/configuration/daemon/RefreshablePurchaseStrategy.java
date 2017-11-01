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

package com.github.robozonky.app.configuration.daemon;

import java.util.Optional;

import com.github.robozonky.api.strategies.PurchaseStrategy;
import com.github.robozonky.common.extensions.StrategyLoader;

final class RefreshablePurchaseStrategy extends RefreshableStrategy<PurchaseStrategy> {

    public RefreshablePurchaseStrategy(final String target) {
        super(target);
    }

    @Override
    protected Optional<PurchaseStrategy> transform(final String source) {
        return StrategyLoader.toPurchase(source);
    }
}
