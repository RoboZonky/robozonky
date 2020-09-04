/*
 * Copyright 2020 The RoboZonky Project
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

package com.github.robozonky.internal.remote.endpoints;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import com.github.robozonky.internal.ApiConstants;
import com.github.robozonky.internal.Defaults;
import com.github.robozonky.internal.remote.entities.InvestmentImpl;
import com.github.robozonky.internal.remote.entities.SellInfoImpl;
import com.github.robozonky.internal.remote.entities.StatisticsImpl;

@Produces(Defaults.MEDIA_TYPE)
@Consumes(Defaults.MEDIA_TYPE)
public interface PortfolioApi extends EntityCollectionApi<InvestmentImpl> {

    @Path(ApiConstants.INVESTMENTS)
    @GET
    @Override
    List<InvestmentImpl> items();

    @Path("/statistics/me/public-overview")
    @GET
    StatisticsImpl getStatistics();

    @Path(ApiConstants.INVESTMENTS + "/{investmentId")
    @GET
    SellInfoImpl getSellInfo(@PathParam("investmentId") long investmentId);

}
