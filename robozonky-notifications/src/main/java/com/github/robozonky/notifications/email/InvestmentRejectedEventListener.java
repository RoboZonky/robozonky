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

package com.github.robozonky.notifications.email;

import java.util.Map;

import com.github.robozonky.api.notifications.InvestmentRejectedEvent;

class InvestmentRejectedEventListener extends AbstractEmailingListener<InvestmentRejectedEvent> {

    public InvestmentRejectedEventListener(final ListenerSpecificNotificationProperties properties) {
        super(properties);
    }

    @Override
    String getSubject(final InvestmentRejectedEvent event) {
        return "Investice zamítnuta - " + event.getRecommendation().amount().intValue() + ",- Kč, " +
                "půjčka č. " + event.getRecommendation().descriptor().item().getId();
    }

    @Override
    String getTemplateFileName() {
        return "investment-rejected.ftl";
    }

    @Override
    protected Map<String, Object> getData(final InvestmentRejectedEvent event) {
        final Map<String, Object> result = Util.getLoanData(event.getRecommendation());
        result.put("newBalance", event.getBalance());
        return result;
    }
}
