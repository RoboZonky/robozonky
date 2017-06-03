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
package com.github.triceo.robozonky.api.remote;

import java.util.Collection;
import java.util.List;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import com.github.triceo.robozonky.api.remote.entities.BlockedAmount;
import com.github.triceo.robozonky.api.remote.entities.Investment;
import com.github.triceo.robozonky.api.remote.entities.Loan;
import com.github.triceo.robozonky.api.remote.entities.Statistics;
import com.github.triceo.robozonky.api.remote.entities.Wallet;
import com.github.triceo.robozonky.api.remote.enums.InvestmentStatuses;
import com.github.triceo.robozonky.api.remote.enums.Ratings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/")
@Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
@Consumes(MediaType.APPLICATION_JSON + ";charset=UTF-8")
public interface ZonkyApi extends Api {

    Logger LOGGER = LoggerFactory.getLogger(ZonkyApi.class);

    String LOANS = "/loans";
    String MARKETPLACE = LOANS + "/marketplace";
    String ME = "/users/me";
    String WALLET = ME + "/wallet";
    String INVESTMENTS = ME + "/investments";

    @GET
    @Path(ZonkyApi.WALLET)
    Wallet getWallet();

    /**
     * Retrieve blocked amounts in the wallet, that either represent a pending investment, or investor's fee. This
     * query is paginated.
     *
     * @param pageSize How many items should be listed on the page.
     * @param pageNo Number of the page to show, where 0 is the first page.
     * @return Blocked amount on that particular page.
     */
    @GET
    @Path(ZonkyApi.WALLET + "/blocked-amounts")
    List<BlockedAmount> getBlockedAmounts(@HeaderParam("X-Size") int pageSize, @HeaderParam("X-Page") int pageNo);

    @GET
    @Path(ZonkyApi.LOANS + "/{loanId}")
    Loan getLoan(@PathParam("loanId") int loanId);

    @GET
    @Path(ZonkyApi.MARKETPLACE)
    @Override
    List<Loan> getLoans();

    @GET
    @Path(ZonkyApi.ME + "/logout")
    void logout();

    @GET
    @Path(ZonkyApi.INVESTMENTS + "/statistics")
    Statistics getStatistics();

    @GET
    @Path(ZonkyApi.INVESTMENTS)
    Collection<Investment> getInvestments();

    @POST
    @Path("/marketplace/investment")
    void invest(Investment investment);
}

