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

package com.github.triceo.robozonky.common.remote;

import com.github.triceo.robozonky.api.remote.ControlApi;
import com.github.triceo.robozonky.api.remote.LoanApi;
import com.github.triceo.robozonky.api.remote.PortfolioApi;
import com.github.triceo.robozonky.api.remote.WalletApi;
import com.github.triceo.robozonky.api.remote.entities.ZonkyApiToken;

class Api implements AutoCloseable {

    private static <T> T extractApi(final Apis.Wrapper<T> wrapper) {
        return wrapper.execute(a -> a);
    }

    private final Apis.Wrapper<LoanApi> loans;
    private final Apis.Wrapper<WalletApi> wallet;
    private final Apis.Wrapper<PortfolioApi> portfolio;
    private final Apis.Wrapper<ControlApi> control;

    public Api(final Apis apis, final ZonkyApiToken token) {
        this.loans = apis.loans(token);
        this.wallet = apis.wallet(token);
        this.portfolio = apis.portfolio(token);
        this.control = apis.control(token);
    }

    public <T> T execute(final Apis.Executable<T> executable) {
        return executable.execute(Api.extractApi(control), Api.extractApi(loans),
                Api.extractApi(wallet),
                Api.extractApi(portfolio));
    }

    @Override
    public void close() {
        this.loans.close();
        this.wallet.close();
        this.portfolio.close();
        this.control.close();
    }
}
