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

package com.github.robozonky.notifications.samples;

import com.github.robozonky.api.notifications.WeeklySummaryEvent;
import com.github.robozonky.api.strategies.ExtendedPortfolioOverview;

public final class MyWeeklySummaryEvent extends AbstractEvent implements WeeklySummaryEvent {

    private final ExtendedPortfolioOverview extendedPortfolioOverview = Util.randomizeExtendedPortfolioOverview();

    @Override
    public ExtendedPortfolioOverview getPortfolioOverview() {
        return extendedPortfolioOverview;
    }
}
