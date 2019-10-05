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

package com.github.robozonky.notifications.listeners;

import com.github.robozonky.api.notifications.WeeklySummaryEvent;
import com.github.robozonky.api.strategies.ExtendedPortfolioOverview;
import com.github.robozonky.notifications.AbstractTargetHandler;
import com.github.robozonky.notifications.SupportedListener;

import java.util.Map;

import static java.util.Map.entry;

public class WeeklySummaryEventListener extends AbstractListener<WeeklySummaryEvent> {

    public WeeklySummaryEventListener(final SupportedListener listener, final AbstractTargetHandler handler) {
        super(listener, handler);
    }

    @Override
    public String getSubject(final WeeklySummaryEvent event) {
        return "Týdenní přehled informací o Vašem Zonky portfoliu";
    }

    @Override
    public String getTemplateFileName() {
        return "weekly-summary.ftl";
    }

    @Override
    protected Map<String, Object> getData(final WeeklySummaryEvent event) {
        final ExtendedPortfolioOverview summary = event.getPortfolioOverview();
        return Map.ofEntries(
                entry("portfolio", Util.summarizePortfolioStructure(summary))
        );
    }
}
