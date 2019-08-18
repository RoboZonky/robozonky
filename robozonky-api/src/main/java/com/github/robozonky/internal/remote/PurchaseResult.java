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

package com.github.robozonky.internal.remote;

import java.util.stream.Stream;
import javax.ws.rs.ClientErrorException;

public final class PurchaseResult extends Result<PurchaseFailureType> {

    private static final PurchaseResult SUCCESS = new PurchaseResult();

    private PurchaseResult() {
        // just call super constructor
    }

    private PurchaseResult(final ClientErrorException ex) {
        super(ex);
    }

    public static PurchaseResult success() {
        return SUCCESS;
    }

    public static PurchaseResult failure(final ClientErrorException ex) {
        return new PurchaseResult(ex);
    }

    @Override
    protected PurchaseFailureType getForUnknown() {
        return PurchaseFailureType.UNKNOWN;
    }

    @Override
    protected Stream<PurchaseFailureType> getAll() {
        return Stream.of(PurchaseFailureType.values());
    }
}
