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

package com.github.robozonky.api.remote;

import java.util.List;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import com.github.robozonky.api.remote.entities.Investment;
import com.github.robozonky.api.remote.entities.SellInfo;
import com.github.robozonky.api.remote.entities.Statistics;
import com.github.robozonky.internal.ApiConstants;
import com.github.robozonky.internal.Defaults;

@Produces(Defaults.MEDIA_TYPE)
@Consumes(Defaults.MEDIA_TYPE)
public interface PortfolioApi extends EntityCollectionApi<Investment> {

    @Path(ApiConstants.INVESTMENTS)
    @GET
    @Override
    List<Investment> items();

    @Path("/statistics/me/public-overview")
    @GET
    Statistics getStatistics();

    @Path(ApiConstants.INVESTMENTS + "/{investmentId}/smpSellInfo")
    @GET
    SellInfo getSellInfo(@PathParam("investmentId") long investmentId);

}
