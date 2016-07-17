/*
 * Copyright 2016 Lukáš Petrovický
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

package com.github.triceo.robozonky.operations;

import com.github.triceo.robozonky.remote.InvestingZonkyApi;
import com.github.triceo.robozonky.remote.Investment;
import com.github.triceo.robozonky.remote.ZonkyApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sends the investment command to the Zonky API. If the API is not instance of {@link InvestingZonkyApi}, we are doing
 * a dry run and no actual investments will be made.
 */
public class InvestOperation extends BiOperation<ZonkyApi, Investment, Investment> {

    private static final Logger LOGGER = LoggerFactory.getLogger(InvestOperation.class);

    @Override
    protected Logger getLogger() {
        return InvestOperation.LOGGER;
    }

    @Override
    protected Investment perform(final ZonkyApi api, final Investment i) {
        if (api instanceof InvestingZonkyApi) {
            ((InvestingZonkyApi)api).invest(i);
            InvestOperation.LOGGER.info("Invested {} CZK into loan {}.", i.getAmount(), i.getLoanId());
        } else {
            InvestOperation.LOGGER.info("Dry run. Otherwise would have invested {} CZK into loan {}.", i.getAmount(),
                    i.getLoanId());
        }
        return i;
    }

}
