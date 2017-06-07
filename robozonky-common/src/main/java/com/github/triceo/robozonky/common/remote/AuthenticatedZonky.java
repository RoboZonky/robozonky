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

import java.util.Spliterator;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.github.triceo.robozonky.api.remote.ControlApi;
import com.github.triceo.robozonky.api.remote.EntityCollectionApi;
import com.github.triceo.robozonky.api.remote.LoanApi;
import com.github.triceo.robozonky.api.remote.PortfolioApi;
import com.github.triceo.robozonky.api.remote.WalletApi;
import com.github.triceo.robozonky.api.remote.entities.BlockedAmount;
import com.github.triceo.robozonky.api.remote.entities.Investment;
import com.github.triceo.robozonky.api.remote.entities.Loan;
import com.github.triceo.robozonky.api.remote.entities.Statistics;
import com.github.triceo.robozonky.api.remote.entities.Wallet;

public class AuthenticatedZonky implements AutoCloseable {

    private final Api<ControlApi> controlApi;
    private final PaginatedApi<Loan, LoanApi> loanApi;
    private final PaginatedApi<Investment, PortfolioApi> portfolioApi;
    private final PaginatedApi<BlockedAmount, WalletApi> walletApi;

    AuthenticatedZonky(final Api<ControlApi> control, final PaginatedApi<Loan, LoanApi> loans,
                       final PaginatedApi<Investment, PortfolioApi> portfolio,
                       final PaginatedApi<BlockedAmount, WalletApi> wallet) {
        if (control == null || loans == null || portfolio == null || wallet == null) {
            throw new IllegalArgumentException("No API may be null.");
        }
        this.controlApi = control;
        this.loanApi = loans;
        this.portfolioApi = portfolio;
        this.walletApi = wallet;
    }

    public void invest(final Investment investment) {
        controlApi.execute(api -> {
            api.invest(investment);
        });
    }

    public Wallet getWallet() {
        return walletApi.execute(WalletApi::wallet);
    }

    private static <T, S extends EntityCollectionApi<T>> Stream<T> getStream(final PaginatedApi<T, S> api) {
        final Paginated<T> p = new PaginatedImpl<>(api);
        final Spliterator<T> s = new EntitySpliterator<>(p);
        return StreamSupport.stream(s, false);
    }

    public Stream<BlockedAmount> getBlockedAmounts() {
        return AuthenticatedZonky.getStream(walletApi);
    }

    public Statistics getStatistics() {
        return portfolioApi.execute(PortfolioApi::statistics);
    }

    public Stream<Investment> getInvestments() {
        return AuthenticatedZonky.getStream(portfolioApi);
    }

    public Stream<Loan> getAvailableLoans() {
        return AuthenticatedZonky.getStream(loanApi);
    }

    public Loan getLoan(final int id) {
        return loanApi.execute(api -> {
            return api.item(id);
        });
    }

    public void logout() {
        controlApi.execute(ControlApi::logout);
    }

    @Override
    public void close() {
        this.controlApi.close();
    }

}
