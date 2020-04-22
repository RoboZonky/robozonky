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

import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

import com.github.robozonky.internal.remote.entities.ZonkyApiTokenImpl;

@Path("/oauth/token")
public interface ZonkyOAuthApi {

    @POST
    ZonkyApiTokenImpl login(
            @FormParam("code") String password,
            @FormParam("redirect_uri") @DefaultValue("https://app.zonky.cz/api/oauth/code") String redirectUri,
            @FormParam("grant_type") @DefaultValue("authorization_code") String grantType);

    @POST
    ZonkyApiTokenImpl refresh(
            @FormParam("refresh_token") String refreshToken,
            @FormParam("grant_type") @DefaultValue("refresh_token") String grantType);
}
