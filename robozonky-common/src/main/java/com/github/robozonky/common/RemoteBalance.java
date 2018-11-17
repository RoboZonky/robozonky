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

package com.github.robozonky.common;

import java.math.BigDecimal;
import java.util.function.Supplier;

/**
 * Available balance register which periodically updates from the remote Zonky API.
 */
public interface RemoteBalance extends Supplier<BigDecimal> {

    /**
     * Request an update of balance from the remote API.
     * @param change May be used within the context of a dry run to reflect a pretend operation.
     * @param clearWithNextReload Whether this amount should be removed during the next remote update.
     */
    void update(BigDecimal change, boolean clearWithNextReload);
}
