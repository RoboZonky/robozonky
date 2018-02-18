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

package com.github.robozonky.common.remote;

import java.util.Spliterator;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.github.robozonky.api.remote.ControlApi;
import com.github.robozonky.api.remote.EntityCollectionApi;
import com.github.robozonky.api.remote.LoanApi;
import com.github.robozonky.api.remote.ParticipationApi;
import com.github.robozonky.api.remote.PortfolioApi;
import com.github.robozonky.api.remote.WalletApi;
import com.github.robozonky.api.remote.entities.BlockedAmount;
import com.github.robozonky.api.remote.entities.Participation;
import com.github.robozonky.api.remote.entities.PurchaseRequest;
import com.github.robozonky.api.remote.entities.RawInvestment;
import com.github.robozonky.api.remote.entities.RawLoan;
import com.github.robozonky.api.remote.entities.Restrictions;
import com.github.robozonky.api.remote.entities.SellRequest;
import com.github.robozonky.api.remote.entities.Wallet;
import com.github.robozonky.api.remote.entities.sanitized.Investment;
import com.github.robozonky.api.remote.entities.sanitized.Loan;
import com.github.robozonky.api.remote.entities.sanitized.MarketplaceLoan;
import com.github.robozonky.internal.api.Settings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents an instance of Zonky API that is fully authenticated and ready to perform operations on behalf of the
 * user. Consider {@link #logout()} when done, followed by {@link #close()}.
 */
public class Zonky implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(Zonky.class);

    private final Api<ControlApi> controlApi;
    private final PaginatedApi<RawLoan, LoanApi> loanApi;
    private final PaginatedApi<Participation, ParticipationApi> participationApi;
    private final PaginatedApi<RawInvestment, PortfolioApi> portfolioApi;
    private final PaginatedApi<BlockedAmount, WalletApi> walletApi;

    Zonky(final Api<ControlApi> control, final PaginatedApi<RawLoan, LoanApi> loans,
          final PaginatedApi<Participation, ParticipationApi> participations,
          final PaginatedApi<RawInvestment, PortfolioApi> portfolio,
          final PaginatedApi<BlockedAmount, WalletApi> wallet) {
        if (control == null || loans == null || participations == null || portfolio == null || wallet == null) {
            throw new IllegalArgumentException("No API may be null.");
        }
        this.controlApi = control;
        this.loanApi = loans;
        this.participationApi = participations;
        this.portfolioApi = portfolio;
        this.walletApi = wallet;
    }

    private static <T, S extends EntityCollectionApi<T>> Stream<T> getStream(final PaginatedApi<T, S> api) {
        return Zonky.getStream(api, Sort.unspecified());
    }

    private static <T, S extends EntityCollectionApi<T>> Stream<T> getStream(final PaginatedApi<T, S> api,
                                                                             final Sort<T> ordering) {
        return Zonky.getStream(api, Settings.INSTANCE.getDefaultApiPageSize(), ordering);
    }

    private static <T, S extends EntityCollectionApi<T>> Stream<T> getStream(final PaginatedApi<T, S> api,
                                                                             final int pageSize,
                                                                             final Sort<T> ordering) {
        return getStream(api, new Select(), pageSize, ordering);
    }

    private static <T, S extends EntityCollectionApi<T>> Stream<T> getStream(final PaginatedApi<T, S> api,
                                                                             final Select select) {
        return Zonky.getStream(api, select, Sort.unspecified());
    }

    private static <T, S extends EntityCollectionApi<T>> Stream<T> getStream(final PaginatedApi<T, S> api,
                                                                             final Select select,
                                                                             final Sort<T> ordering) {
        return Zonky.getStream(api, select, Settings.INSTANCE.getDefaultApiPageSize(), ordering);
    }

    private static <T, S extends EntityCollectionApi<T>> Stream<T> getStream(final PaginatedApi<T, S> api,
                                                                             final Select select,
                                                                             final int pageSize,
                                                                             final Sort<T> ordering) {
        final Paginated<T> p = new PaginatedImpl<>(api, select, ordering, pageSize);
        final Spliterator<T> s = new EntitySpliterator<>(p);
        return StreamSupport.stream(s, false);
    }

    public void invest(final Investment investment) {
        LOGGER.info("Investing into loan #{}.", investment.getLoanId());
        controlApi.execute(api -> {
            api.invest(new RawInvestment(investment));
        });
    }

    public void cancel(final Investment investment) {
        LOGGER.info("Cancelling offer to sell investment in loan #{}.", investment.getLoanId());
        controlApi.execute(api -> {
            api.cancel(investment.getId());
        });
    }

    public void purchase(final Participation participation) {
        LOGGER.info("Purchasing participation #{} in loan #{}.", participation.getId(), participation.getLoanId());
        controlApi.execute(api -> {
            api.purchase(participation.getId(), new PurchaseRequest(participation));
        });
    }

    public void sell(final Investment investment) {
        LOGGER.info("Offering to sell investment in loan #{}.", investment.getLoanId());
        controlApi.execute(api -> {
            api.offer(new SellRequest(new RawInvestment(investment)));
        });
    }

    public Wallet getWallet() {
        return walletApi.execute(WalletApi::wallet);
    }

    /**
     * Retrieve blocked amounts from user's wallet via {@link WalletApi}.
     * @return All items from the remote API, lazy-loaded.
     */
    public Stream<BlockedAmount> getBlockedAmounts() {
        return Zonky.getStream(walletApi);
    }

    /**
     * Retrieve investments from user's portfolio via {@link PortfolioApi}.
     * @return All items from the remote API, lazy-loaded. Does not include investments represented by blocked amounts.
     */
    public Stream<Investment> getInvestments() {
        return getInvestments(Sort.unspecified());
    }

    /**
     * Retrieve investments from user's portfolio via {@link PortfolioApi}, in a given order.
     * @param ordering Ordering in which the results should be returned.
     * @return All items from the remote API, lazy-loaded. Does not include investments represented by blocked amounts.
     */
    public Stream<Investment> getInvestments(final Sort<RawInvestment> ordering) {
        return Zonky.getStream(portfolioApi, ordering).map(Investment::sanitized);
    }

    public Loan getLoan(final int id) {
        return Loan.sanitized(loanApi.execute(api -> {
            return api.item(id);
        }));
    }

    /**
     * Retrieve loans from marketplace via {@link LoanApi}.
     * @return All items from the remote API, lazy-loaded.
     */
    public Stream<MarketplaceLoan> getAvailableLoans() {
        return Zonky.getStream(loanApi).map(MarketplaceLoan::sanitized);
    }

    /**
     * Retrieve loans from marketplace via {@link LoanApi}.
     * @param select Rules to filter the selection by.
     * @return All items from the remote API, lazy-loaded.
     */
    public Stream<MarketplaceLoan> getAvailableLoans(final Select select) {
        return Zonky.getStream(loanApi, select).map(MarketplaceLoan::sanitized);
    }

    /**
     * Retrieve participations from secondary marketplace via {@link ParticipationApi}.
     * @return All items from the remote API, lazy-loaded.
     */
    public Stream<Participation> getAvailableParticipations() {
        return Zonky.getStream(participationApi);
    }

    /**
     * Retrieve loans from marketplace via {@link LoanApi}, in a given order.
     * @param ordering Ordering in which the results should be returned.
     * @return All items from the remote API, lazy-loaded.
     */
    public Stream<MarketplaceLoan> getAvailableLoans(final Sort<RawLoan> ordering) {
        return Zonky.getStream(loanApi, ordering).map(MarketplaceLoan::sanitized);
    }

    public Restrictions getRestrictions() {
        return controlApi.execute(ControlApi::restrictions);
    }

    public void logout() {
        controlApi.execute(ControlApi::logout);
    }

    @Override
    public void close() {
        this.controlApi.close();
    }
}
