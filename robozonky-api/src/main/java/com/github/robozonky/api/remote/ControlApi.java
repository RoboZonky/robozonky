/*
 * Copyright 2019 The RoboZonky Project
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

import com.github.robozonky.api.remote.entities.*;
import com.github.robozonky.internal.ApiConstants;
import com.github.robozonky.internal.Defaults;

import javax.ws.rs.*;

@Path("/")
@Produces(Defaults.MEDIA_TYPE)
@Consumes(Defaults.MEDIA_TYPE)
public interface ControlApi {

    @GET
    @Path(ApiConstants.ME + "/logout")
    void logout();

    @GET
    @Path(ApiConstants.INVESTOR_ME + "/restrictions")
    Restrictions restrictions();

    @POST
    @Path("/marketplace/investment")
    void invest(Investment investment);

    @POST
    @Path(ApiConstants.ME + "/traded-investments")
    void offer(SellRequest sellRequest);

    @POST
    @Path("/smp/investments/{id}/shares")
    void purchase(@PathParam("id") long id, PurchaseRequest purchaseRequest);

    @DELETE
    @Path("/traded-investments/{id}")
    void cancel(@PathParam("id") long id);

    @PATCH
    @Path("/loans/marketplace/reservations/my-reservations")
    void accept(Resolutions resolutions);

    @PATCH
    @Path("/loans/marketplace/reservations/my-preferences")
    void setReservationPreferences(ReservationPreferences preferences);

}

