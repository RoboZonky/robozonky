/*
 *
 *  * Copyright 2016 Lukáš Petrovický
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 * /
 */
package com.github.triceo.robozonky.remote;

import java.util.Collection;
import java.util.List;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    List<Loan> getLoans(
            @QueryParam("rating__in") Ratings ratings,
            @QueryParam("remainingInvestment__gt") @DefaultValue("0") int leastRemainingInvestment,
            @QueryParam("termInMonths__gte") @DefaultValue("0") int leastPossibleTermInMonths,
            @QueryParam("termInMonths__lte") int mostPossibleTermInMonths);

    @GET
    @Path(ZonkyApi.MARKETPLACE)
    List<Loan> getLoans(
            @QueryParam("rating__in") Ratings ratings,
            @QueryParam("remainingInvestment__gt") @DefaultValue("0") int leastRemainingInvestment,
            @QueryParam("termInMonths__gte") @DefaultValue("0") int leastPossibleTermInMonths);

    @GET
    @Path(ZonkyApi.MARKETPLACE)
    List<Loan> getLoans(
            @QueryParam("rating__in") Ratings ratings,
            @QueryParam("remainingInvestment__gt") @DefaultValue("0") int leastRemainingInvestment);

    @GET
    @Path(ZonkyApi.MARKETPLACE)
    List<Loan> getLoans(@QueryParam("rating__in") Ratings ratings);

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
    Collection<Investment> getInvestments(
            @QueryParam("loan.status__in") InvestmentStatuses statuses,
            @QueryParam("loan.dpd__gt") @DefaultValue("0") int daysPastDueGreaterThan);

    @GET
    @Path(ZonkyApi.INVESTMENTS)
    Collection<Investment> getInvestments(@QueryParam("loan.status__in") InvestmentStatuses statuses);

}

