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
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

@Path("/")
@Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
@Consumes(MediaType.APPLICATION_JSON + ";charset=UTF-8")
public interface ZonkyApi extends Api {

    String LOANS = "/loans";
    String MARKETPLACE = LOANS + "/marketplace";
    String ME = "/users/me";
    String WALLET = ME + "/wallet";
    String INVESTMENTS = ME + "/investments";
    String OAUTH = "/oauth/token";

    @POST
    @Path(ZonkyApi.OAUTH)
    ZonkyApiToken login(
            @FormParam("username") String username,
            @FormParam("password") String password,
            @FormParam("grant_type") @DefaultValue("password") String grantType,
            @FormParam("scope") @DefaultValue("SCOPE_APP_WEB") String scope);

    @POST
    @Path(ZonkyApi.OAUTH)
    ZonkyApiToken refresh(
            @FormParam("refresh_token") String refreshToken,
            @FormParam("grant_type") @DefaultValue("refresh_token") String grantType,
            @FormParam("scope") @DefaultValue("SCOPE_APP_WEB") String scope);

    @GET
    @Path(ZonkyApi.WALLET)
    Wallet getWallet();

    @GET
    @Path(ZonkyApi.WALLET + "/blocked-amounts")
    List<BlockedAmount> getBlockedAmounts();

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
    List<Loan> logout();

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

    @POST
    @Path("/marketplace/investment")
    void invest(Investment investment);

}

