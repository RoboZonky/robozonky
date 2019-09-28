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

import com.github.robozonky.api.remote.entities.LastPublishedLoan;
import com.github.robozonky.api.remote.entities.Loan;
import com.github.robozonky.internal.Defaults;

import javax.ws.rs.*;
import java.util.List;

@Path("/loans")
@Produces(Defaults.MEDIA_TYPE)
@Consumes(Defaults.MEDIA_TYPE)
public interface LoanApi extends EntityCollectionApi<Loan> {

    /**
     * @return Every single loan that ever was.
     * @see <a href="https://zonky.docs.apiary.io/#introduction/pagination,-sorting-and-filtering">Filtering Zonky.</a>
     */
    @GET
    @Path("marketplace")
    @Override
    List<Loan> items();

    @GET
    @Path("last-published")
    LastPublishedLoan lastPublished();

    @GET
    @Path("{loanId}")
    Loan item(@PathParam("loanId") int id);
}
