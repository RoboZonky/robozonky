package net.petrovicky.zonkybot.remote;

import java.util.Collection;
import java.util.List;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

/*
 * Copyright 2016 Lukáš Petrovický
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@Path("/")
@Produces(MediaType.APPLICATION_JSON + ";charset=UTF-8")
@Consumes(MediaType.APPLICATION_JSON + ";charset=UTF-8")
public interface ZonkyAPI {

    String MARKETPLACE = "/loans/marketplace";
    String ME = "/users/me";
    String INVESTMENTS = ME + "/investments";

    @GET
    @Path(ZonkyAPI.ME + "/wallet")
    Wallet getWallet();

    @GET
    @Path(ZonkyAPI.MARKETPLACE)
    List<Loan> getLoans(
            @QueryParam("rating.type__in") Ratings ratings,
            @QueryParam("remainingInvestment__gt") @DefaultValue("0") int leastRemainingInvestment,
            @QueryParam("termInMonths__gte") @DefaultValue("0") int leastPossibleTermInMonths,
            @QueryParam("termInMonths__lte") int mostPossibleTermInMonths);

    @GET
    @Path(ZonkyAPI.MARKETPLACE)
    List<Loan> getLoans(
            @QueryParam("rating.type__in") Ratings ratings,
            @QueryParam("remainingInvestment__gt") @DefaultValue("0") int leastRemainingInvestment,
            @QueryParam("termInMonths__gte") @DefaultValue("0") int leastPossibleTermInMonths);

    @GET
    @Path(ZonkyAPI.MARKETPLACE)
    List<Loan> getLoans(
            @QueryParam("rating.type__in") Ratings ratings,
            @QueryParam("remainingInvestment__gt") @DefaultValue("0") int leastRemainingInvestment);

    @GET
    @Path(ZonkyAPI.MARKETPLACE)
    List<Loan> getLoans(@QueryParam("rating.type__in") Ratings ratings);

    @GET
    @Path(ZonkyAPI.ME + "/logout")
    List<Loan> logout();

    @GET
    @Path(ZonkyAPI.INVESTMENTS + "/statistics")
    Statistics getStatistics();

    @GET
    @Path(ZonkyAPI.INVESTMENTS)
    Collection<Investment> getInvestments(
            @QueryParam("loan.status__in") InvestmentStatuses statuses,
            @QueryParam("loan.dpd__gt") @DefaultValue("0") int daysPastDueGreaterThan);

    @GET
    @Path(ZonkyAPI.INVESTMENTS)
    Collection<FullInvestment> getInvestments(@QueryParam("loan.status__in") InvestmentStatuses statuses);

    @POST
    @Path(ZonkyAPI.ME + "/investment")
    void invest(Investment investment);

}

