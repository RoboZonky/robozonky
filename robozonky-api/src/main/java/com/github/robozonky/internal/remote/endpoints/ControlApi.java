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

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PATCH;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import com.github.robozonky.internal.ApiConstants;
import com.github.robozonky.internal.Defaults;
import com.github.robozonky.internal.remote.entities.ConsentsImpl;
import com.github.robozonky.internal.remote.entities.InvestmentImpl;
import com.github.robozonky.internal.remote.entities.PurchaseRequestImpl;
import com.github.robozonky.internal.remote.entities.ReservationPreferencesImpl;
import com.github.robozonky.internal.remote.entities.ResolutionsImpl;
import com.github.robozonky.internal.remote.entities.RestrictionsImpl;
import com.github.robozonky.internal.remote.entities.SellRequestImpl;

@Produces(Defaults.MEDIA_TYPE)
@Consumes(Defaults.MEDIA_TYPE)
public interface ControlApi {

    @GET
    @Path(ApiConstants.ME + "/logout")
    void logout();

    @GET
    @Path("/investors/me/restrictions")
    RestrictionsImpl restrictions();

    @GET
    @Path(ApiConstants.ME + "/consents")
    ConsentsImpl consents();

    @POST
    @Path("/marketplace/investment")
    void invest(InvestmentImpl investment);

    @POST
    @Path(ApiConstants.ME + "/traded-investments")
    void offer(SellRequestImpl sellRequest);

    @POST
    @Path(ApiConstants.SMP_INVESTMENTS + "/{id}/shares")
    void purchase(@PathParam("id") long id, PurchaseRequestImpl purchaseRequest);

    @DELETE
    @Path("/traded-investments/{id}")
    void cancel(@PathParam("id") long id);

    @PATCH
    @Path(ApiConstants.RESERVATIONS)
    void accept(ResolutionsImpl resolutions);

    @PATCH
    @Path(ApiConstants.RESERVATION_PREFERENCES)
    void setReservationPreferences(ReservationPreferencesImpl preferences);

}
