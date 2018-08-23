/*
 * Copyright 2018 The RoboZonky Project
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
package com.github.robozonky.common.remote;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import com.github.robozonky.internal.api.ApiConstants;
import com.github.robozonky.internal.api.Defaults;

@Path("/")
@Produces(Defaults.MEDIA_TYPE)
@Consumes(Defaults.MEDIA_TYPE)
public interface ExportApi {

    @POST
    @Path(ApiConstants.WALLET_EXPORT)
    void requestWalletExport();

    @POST
    @Path(ApiConstants.INVESTMENTS_EXPORT)
    void requestInvestmentsExport();

    @GET
    @Path(ApiConstants.WALLET_EXPORT)
    Response.Status getWalletExportStatus();

    @GET
    @Path(ApiConstants.INVESTMENTS_EXPORT)
    Response.Status getInvestmentsExportStatus();

    @GET
    @Path(ApiConstants.WALLET_EXPORT + "/data")
    Response downloadWalletExport();

    @GET
    @Path(ApiConstants.INVESTMENTS_EXPORT + "/data")
    Response downloadInvestmentsExport();

}

