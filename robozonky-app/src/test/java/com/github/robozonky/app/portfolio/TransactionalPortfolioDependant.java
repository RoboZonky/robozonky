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

package com.github.robozonky.app.portfolio;

import com.github.robozonky.app.configuration.daemon.TransactionalPortfolio;

final class TransactionalPortfolioDependant implements PortfolioDependant {

    private final PortfolioDependant parent;

    private TransactionalPortfolioDependant(final PortfolioDependant repayments) {
        this.parent = repayments;
    }

    @Override
    public void accept(final TransactionalPortfolio transactionalPortfolio) {
        parent.accept(transactionalPortfolio);
        transactionalPortfolio.run();
    }

    public static PortfolioDependant create(final PortfolioDependant repayments) {
        return new TransactionalPortfolioDependant(repayments);
    }
}
