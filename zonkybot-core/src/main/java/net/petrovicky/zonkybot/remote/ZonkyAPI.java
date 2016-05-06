package net.petrovicky.zonkybot.remote;

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
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public interface ZonkyAPI {

    String MARKETPLACE = "/loans/marketplace";
    String ME = "/users/me";

    @GET
    @Path(ME + "/wallet")
    Wallet getWallet();

    @GET
    @Path(MARKETPLACE)
    List<Loan> getLoans(
            @QueryParam("rating.type__in") Ratings ratings,
            @QueryParam("remainingInvestment__gt") @DefaultValue("0") int leastRemainingInvestment,
            @QueryParam("termInMonths__gte") @DefaultValue("0") int leastPossibleTermInMonths,
            @QueryParam("termInMonths__lte") int mostPossibleTermInMonths);

    @GET
    @Path(MARKETPLACE)
    List<Loan> getLoans(
            @QueryParam("rating.type__in") Ratings ratings,
            @QueryParam("remainingInvestment__gt") @DefaultValue("0") int leastRemainingInvestment,
            @QueryParam("termInMonths__gte") @DefaultValue("0") int leastPossibleTermInMonths);

    @GET
    @Path(MARKETPLACE)
    List<Loan> getLoans(
            @QueryParam("rating.type__in") Ratings ratings,
            @QueryParam("remainingInvestment__gt") @DefaultValue("0") int leastRemainingInvestment);

    @GET
    @Path(MARKETPLACE)
    List<Loan> getLoans(
            @QueryParam("rating.type__in") Ratings ratings);

    @GET
    @Path(ME + "/logout")
    List<Loan> logout();

    @GET
    @Path(ME + "/investments/statistics")
    Statistics getStatistics();

    @POST
    @Path(ME + "/investment")
    void invest(Investment investment);

}

