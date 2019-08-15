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

package com.github.robozonky.app.daemon;

import java.util.Optional;

import com.github.robozonky.internal.tenant.Tenant;
import jdk.jfr.Event;

interface OperationDescriptor<T, S> {

    boolean isEnabled(final Tenant tenant);

    Optional<S> getStrategy(final Tenant tenant);

    MarketplaceAccessor<T> newMarketplaceAccessor(final Tenant tenant);

    long identify(final T descriptor);

    Operation<T, S> getOperation();

    Event newJfrEvent();

}
