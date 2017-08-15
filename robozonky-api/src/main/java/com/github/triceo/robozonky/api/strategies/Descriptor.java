/*
 * Copyright 2017 Lukáš Petrovický
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

package com.github.triceo.robozonky.api.strategies;

import java.math.BigDecimal;
import java.util.Optional;

import com.github.triceo.robozonky.api.remote.entities.Loan;

public interface Descriptor<T extends Recommended<T, S, X>, S extends Descriptor<T, S, X>, X> {

    X item();

    Loan related();

    Optional<T> recommend(final BigDecimal amount);
}
