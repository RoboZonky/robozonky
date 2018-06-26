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

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import com.github.robozonky.api.remote.CollectionsApi;
import com.github.robozonky.api.remote.ControlApi;
import com.github.robozonky.api.remote.LoanApi;
import com.github.robozonky.api.remote.ParticipationApi;
import com.github.robozonky.api.remote.PortfolioApi;
import com.github.robozonky.api.remote.TransactionApi;
import com.github.robozonky.api.remote.WalletApi;
import com.github.robozonky.api.remote.entities.BlockedAmount;
import com.github.robozonky.api.remote.entities.Participation;
import com.github.robozonky.api.remote.entities.PurchaseRequest;
import com.github.robozonky.api.remote.entities.RawDevelopment;
import com.github.robozonky.api.remote.entities.RawInvestment;
import com.github.robozonky.api.remote.entities.RawLoan;
import com.github.robozonky.api.remote.entities.Restrictions;
import com.github.robozonky.api.remote.entities.SellRequest;
import com.github.robozonky.api.remote.entities.Statistics;
import com.github.robozonky.api.remote.entities.Transaction;
import com.github.robozonky.api.remote.entities.Wallet;
import com.github.robozonky.api.remote.entities.sanitized.Development;
import com.github.robozonky.api.remote.entities.sanitized.Investment;
import com.github.robozonky.api.remote.entities.sanitized.Loan;
import com.github.robozonky.api.remote.entities.sanitized.MarketplaceLoan;
import com.github.robozonky.api.remote.enums.TransactionCategory;
import com.github.robozonky.internal.api.Settings;
import com.github.rutledgepaulv.pagingstreams.PagingStreams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents an instance of Zonky API that is fully authenticated and ready to perform operations on behalf of the
 * user. Consider {@link #logout()} when done.
 */
public class Zonky {

    private static final Logger LOGGER = LoggerFactory.getLogger(Zonky.class);

    private final Api<ControlApi> controlApi;
    private final PaginatedApi<RawLoan, LoanApi> loanApi;
    private final PaginatedApi<Participation, ParticipationApi> participationApi;
    private final PaginatedApi<RawInvestment, PortfolioApi> portfolioApi;
    private final PaginatedApi<BlockedAmount, WalletApi> walletApi;
    private final PaginatedApi<Transaction, TransactionApi> transactions;
    private final PaginatedApi<RawDevelopment, CollectionsApi> collectionsApi;

    Zonky(final Api<ControlApi> control, final PaginatedApi<RawLoan, LoanApi> loans,
          final PaginatedApi<Participation, ParticipationApi> participations,
          final PaginatedApi<RawInvestment, PortfolioApi> portfolio,
          final PaginatedApi<BlockedAmount, WalletApi> wallet,
          final PaginatedApi<Transaction, TransactionApi> transactions,
          final PaginatedApi<RawDevelopment, CollectionsApi> collections) {
        if (control == null || loans == null || participations == null || portfolio == null || wallet == null ||
                transactions == null || collections == null) {
            throw new IllegalArgumentException("No API may be null.");
        }
        this.controlApi = control;
        this.loanApi = loans;
        this.participationApi = participations;
        this.portfolioApi = portfolio;
        this.walletApi = wallet;
        this.transactions = transactions;
        this.collectionsApi = collections;
    }

    private static <T, S> Stream<T> getStream(final PaginatedApi<T, S> api, final Function<S, List<T>> function) {
        return Zonky.getStream(api, function, Settings.INSTANCE.getDefaultApiPageSize());
    }

    private static <T, S> Stream<T> getStream(final PaginatedApi<T, S> api, final Function<S, List<T>> function,
                                              final int pageSize) {
        return getStream(api, function, new Select(), pageSize);
    }

    private static <T, S> Stream<T> getStream(final PaginatedApi<T, S> api, final Function<S, List<T>> function,
                                              final Select select) {
        return Zonky.getStream(api, function, select, Settings.INSTANCE.getDefaultApiPageSize());
    }

    private static <T, S> Stream<T> getStream(final PaginatedApi<T, S> api, final Function<S, List<T>> function,
                                              final Select select, final int pageSize) {
        return PagingStreams.streamBuilder(new EntityCollectionPageSource<>(api, function, select, pageSize))
                .pageSize(pageSize)
                .build();
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
        return Zonky.getStream(walletApi, WalletApi::items);
    }

    /**
     * Retrieve investments from user's portfolio via {@link PortfolioApi}, in a given order.
     * @param select Rules to filter the selection by.
     * @return All items from the remote API, lazy-loaded.
     */
    public Stream<Investment> getInvestments(final Select select) {
        final Function<Investment, OffsetDateTime> investmentDateSupplier = (i) -> {
            /*
             * Zonky makes it very difficult to figure out when any particular investment was made. this code attempts
             * to figure it out.
             *
             * we find the first payment from past transactions. if there's no first payment, we use the expected date.
             * in the very rare situation where both are missing, we just use the present date.
             *
             * we subtract a month from that value to find out the approximate date when this loan was created.
             */
            final Supplier<OffsetDateTime> expectedPayment =
                    () -> i.getNextPaymentDate().orElse(OffsetDateTime.now());
            final OffsetDateTime lastPayment = getTransactions(i)
                    .filter(t -> t.getCategory() == TransactionCategory.PAYMENT)
                    .map(Transaction::getTransactionDate)
                    .sorted()
                    .findFirst()
                    .orElseGet(expectedPayment);
            final OffsetDateTime d = lastPayment.minusMonths(1);
            LOGGER.debug("Date for investment #{} (loan #{}) was determined to be {}.", i.getId(), i.getLoanId(), d);
            return d;
        };
        return Zonky.getStream(portfolioApi, PortfolioApi::items, select)
                .map(raw -> Investment.sanitized(raw, investmentDateSupplier));
    }

    public Loan getLoan(final int id) {
        return Loan.sanitized(loanApi.execute(api -> api.item(id)));
    }

    public Optional<Investment> getInvestment(final int id) {
        final Select s = new Select().equals("id", id);
        return getInvestments(s).findFirst();
    }

    public Optional<Investment> getInvestment(final Loan loan) {
        final Select s = new Select().equals("loan.id", loan.getId());
        return getInvestments(s).findFirst();
    }

    /**
     * Retrieve loans from marketplace via {@link LoanApi}.
     * @param select Rules to filter the selection by.
     * @return All items from the remote API, lazy-loaded.
     */
    public Stream<MarketplaceLoan> getAvailableLoans(final Select select) {
        return Zonky.getStream(loanApi, LoanApi::items, select).map(MarketplaceLoan::sanitized);
    }

    /**
     * Retrieve transactions from the wallet via {@link TransactionApi}.
     * @param select Rules to filter the selection by.
     * @return All items from the remote API, lazy-loaded, filtered.
     */
    public Stream<Transaction> getTransactions(final Select select) {
        return Zonky.getStream(transactions, TransactionApi::items, select);
    }

    /**
     * Retrieve transactions from the wallet via {@link TransactionApi}.
     * @param investment Investment to filter the selection by.
     * @return All items from the remote API, lazy-loaded, filtered for the specific investment.
     */
    public Stream<Transaction> getTransactions(final Investment investment) {
        final Select select = new Select().equals("investment.id", investment.getId());
        return getTransactions(select);
    }

    /**
     * Retrieve loan collections information via {@link CollectionsApi}.
     * @return All items from the remote API, lazy-loaded.
     */
    public Stream<Development> getDevelopments(final Loan loan) {
        return Zonky.getStream(collectionsApi, a -> a.items(loan.getId())).map(Development::sanitized);
    }

    /**
     * Retrieve participations from secondary marketplace via {@link ParticipationApi}.
     * @param select Rules to filter the selection by.
     * @return All items from the remote API, lazy-loaded.
     */
    public Stream<Participation> getAvailableParticipations(final Select select) {
        return Zonky.getStream(participationApi, ParticipationApi::items, select);
    }

    public Restrictions getRestrictions() {
        return controlApi.execute(ControlApi::restrictions);
    }

    public Statistics getStatistics() {
        return portfolioApi.execute(PortfolioApi::item);
    }

    public void logout() {
        controlApi.execute(ControlApi::logout);
    }
}
