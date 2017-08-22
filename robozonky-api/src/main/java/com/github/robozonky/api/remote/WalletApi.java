/*
 * Copyright 2017 The RoboZonky Project
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
import javax.ws.rs.Produces;

import com.github.robozonky.api.remote.entities.BlockedAmount;
import com.github.robozonky.api.remote.entities.Wallet;
import com.github.robozonky.internal.api.Defaults;

@Path(ControlApi.ME + "/wallet")
@Produces(Defaults.MEDIA_TYPE)
@Consumes(Defaults.MEDIA_TYPE)
public interface WalletApi extends EntityCollectionApi<BlockedAmount> {

    @GET
    Wallet wallet();

    /**
     * Retrieve blocked amounts from user's wallet.
     * @return Zonky API will only return the first page of results. (20 items?) Use pagination API to retrieve more
     * than that.
     */
    @GET
    @Path("blocked-amounts")
    @Override
    List<BlockedAmount> items();
}
