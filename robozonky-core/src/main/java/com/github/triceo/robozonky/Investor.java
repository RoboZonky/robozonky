/*
 * Copyright 2016 Lukáš Petrovický
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
package com.github.triceo.robozonky;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.triceo.robozonky.remote.BaseInvestment;
import com.github.triceo.robozonky.remote.BlockedAmount;
import com.github.triceo.robozonky.remote.InvestingZonkyApi;
import com.github.triceo.robozonky.remote.Investment;
import com.github.triceo.robozonky.remote.Loan;
import com.github.triceo.robozonky.remote.Statistics;
import com.github.triceo.robozonky.remote.ZonkyApi;
import com.github.triceo.robozonky.strategy.InvestmentStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controls the investments based on the strategy, user portfolio and balance.
 */
public class Investor {

    private static final Logger LOGGER = LoggerFactory.getLogger(Investor.class);

    /**
     * Determine whether or not a given loan is present among existing investments.
     *
     * @param loan Loan in question.
     * @param investments Known investments.
     * @return True if present.
     */
    private static boolean isLoanPresent(final Loan loan, final Collection<Investment> investments) {
        return investments.stream().filter(i -> loan.getId() == i.getLoanId()).findFirst().isPresent();
    }

    /**
     * Merge two collections of investments with one another. Every investment will only by represented once, no matter
     * how many times it was present in source collections. No order guarantees are made.
     *
     * @param left First collection of investments to be merged with the second.
     * @param right Second collection of investments to be merged with the first.
     * @return Unmodifiable collection containing all the investments from both arguments.
     */
    static Collection<Investment> mergeInvestments(final Collection<Investment> left,
                                                   final Collection<Investment> right) {
        return Collections.unmodifiableCollection(Stream.concat(left.stream(), right.stream())
                .distinct()
                .collect(Collectors.toList()));
    }

    /**
     * The core investing call. Receives a particular loan, checks if the user has enough money to invest, and sends the
     * command to the Zonky API.
     *
     * @param api Authenticated Zonky API, ready for investing.
     * @param loanId Loan to invest into. Will be fetched fresh to make sure it is really available.
     * @param amount Amount to invest into the loan.
     * @param balance How much usable cash the user has in the wallet.
     * @return Present if input valid and operation succeeded, empty otherwise.
     */
    private static Optional<Investment> invest(final ZonkyApi api, final int loanId, final int amount,
                                               final int balance) {
        final Loan l = api.getLoan(loanId);
        if (amount < InvestmentStrategy.MINIMAL_INVESTMENT_ALLOWED) {
            Investor.LOGGER.info("Not investing into loan '{}', since investment ({} CZK) less than bare minimum.",
                    l, amount);
            return Optional.empty();
        } else if (amount > balance) { // strategy should not allow this
            Investor.LOGGER.info("Not investing into loan '{}', {} CZK to invest is more than {} CZK balance.",
                    l, amount, balance);
            return Optional.empty();
        } else if (amount > l.getRemainingInvestment()) {
            Investor.LOGGER.info("Not investing into loan '{}', {} CZK to invest is more than {} CZK loan remaining.",
                    l, amount, l.getRemainingInvestment());
            return Optional.empty();
        }
        final Investment investment = new Investment(l, amount);
        if (api instanceof InvestingZonkyApi) {
            ((InvestingZonkyApi)api).invest(investment);
            Investor.LOGGER.info("Invested {} CZK into loan {}.", investment.getAmount(), investment.getLoanId());
        } else {
            Investor.LOGGER.info("Dry run. Otherwise would have invested {} CZK into loan {}.", investment.getAmount(),
                    investment.getLoanId());
        }
        return Optional.of(investment);
    }

    private static Future<Loan> scheduleLoanRetrieval(final ZonkyApi api, final int loanId,
                                                      final ExecutorService executor) {
        Investor.LOGGER.trace("Scheduling retrieval of loan #{} from Zonky.", loanId);
        return executor.submit(() -> {
            Investor.LOGGER.trace("Retrieving loan #{} from Zonky.", loanId);
            final Loan loan = api.getLoan(loanId);
            Investor.LOGGER.trace("Retrieved loan #{} from Zonky.", loanId);
            return loan;
        });
    }

    private static Loan retrieveLoan(final Future<Loan> future) {
        try {
            return future.get();
        } catch (final InterruptedException | ExecutionException ex) {
            throw new IllegalStateException("Failed retrieving loan data from remote API.", ex);
        }
    }

    /**
     * Blocked amounts represent loans in various stages. Either the user has invested and the loan has not yet been
     * funded to 100 % ("na tržišti"), or the user invested and the loan has been funded ("na cestě"). In the latter
     * case, the loan has already disappeared from the marketplace, which means that it will not be available for
     * investing any more. As far as I know, the next stage is "v pořádku", the blocked amount is cleared and the loan
     * becomes an active investment.
     *
     * Based on that, this method deals with the first case - when the loan is still available for investing, but we've
     * already invested as evidenced by the blocked amount. It also unnecessarily deals with the second case, since
     * that is represented by a blocked amount as well. But that does no harm.
     *
     * In case user has made repeated investments into a particular loan, this will show up as multiple blocked amounts.
     * The method needs to handle this as well.
     *
     * @param api Authenticated API that will be used to retrieve the user's blocked amounts from the wallet.
     * @param e Executor service to execute background threads fetching the loans.
     * @return Every blocked amount represents a future investment. This method returns such investments.
     */
    static List<Investment> retrieveInvestmentsRepresentedByBlockedAmounts(final ZonkyApi api, final ExecutorService e) {
        final Collection<BlockedAmount> amounts = api.getBlockedAmounts(99, 0).stream()
                .filter(blocked -> blocked.getLoanId() > 0) // 0 == Zonky investors' fee
                .collect(Collectors.toList());
        // cache loan instances in case multiple blocked amounts relate to a single loan, not to make extra API queries
        final Map<Integer, Future<Loan>> loans = new HashMap<>(amounts.size());
        amounts.forEach(amount -> {
            final int loanAmount = amount.getAmount();
            final int loanId = amount.getLoanId();
            Investor.LOGGER.debug("{} CZK is being blocked by loan {}.", loanAmount, loanId);
            /*
             * read actual loan data from remote API. this is necessary since we'll need to know loan ratings later when
             * we're calculating rating shares.
             */
            loans.compute(loanId, (k, v) -> v == null ? Investor.scheduleLoanRetrieval(api, k, e) : v);
        });
        // sum blocked amounts per loan
        final Map<Loan, Integer> amountsBlockedByLoans = new LinkedHashMap<>(amounts.size());
        amounts.forEach(amount -> {
            final Loan l = Investor.retrieveLoan(loans.get(amount.getLoanId()));
            final int a = amount.getAmount();
            amountsBlockedByLoans.compute(l, (k, v) -> v == null ? a : v + a);
        });
        // finally return the investments representing the overall sum of blocked amounts
        return Collections.unmodifiableList(amountsBlockedByLoans.entrySet().stream()
                .map(entry -> new Investment(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList()));
    }

    /**
     * Zonky API may return {@link ZonkyApi#getStatistics()} as null if the account has no previous investments.
     *
     * @param api API to execute the operation.
     * @return Either what the API returns, or an empty object.
     */
    static Statistics retrieveStatistics(final ZonkyApi api) {
        final Statistics returned = api.getStatistics();
        return returned == null ? new Statistics() : returned;
    }

    private final ZonkyApi zonkyApi;
    private final BigDecimal initialBalance;
    private final ExecutorService executor;

    /**
     * Standard constructor.
     *
     * @param zonky Authenticated API ready to retrieve user information.
     * @param initialBalance How much available cash the user has in their wallet.
     * @param executor The executor that will be used for API operations that can be performed in parallel.
     */
    public Investor(final ZonkyApi zonky, final BigDecimal initialBalance, final ExecutorService executor) {
        this.zonkyApi = zonky;
        this.initialBalance = initialBalance;
        this.executor = executor;
        Investor.LOGGER.info("RoboZonky starting account balance is {} CZK.", this.initialBalance);
    }

    /**
     * Prepares a list of loans that are suitable for investment by asking the strategy. Then goes over that list one
     * by one, in the order prescribed by the strategy, and attempts to invest into these loans. The first such
     * investment operation that succeeds will return.
     *
     * @param strategy The investment strategy to pick loans from the list.
     * @param availableLoans Loans available to be chosen from.
     * @param balance How much money the user has in the wallet that can be used for investing.
     * @param stats User's portfolio coming from the Zonky API.
     * @param investmentsAlreadyMade Loans already invested into that have not yet disappeared from marketplace.
     * @return The first {@link #invest(ZonkyApi, int, int, int)} which succeeds, or empty if none have.
     */
    Optional<Investment> investOnce(final InvestmentStrategy strategy, final List<Loan> availableLoans,
                                    final BigDecimal balance, final Statistics stats,
                                    final Collection<Investment> investmentsAlreadyMade) {
        final PortfolioOverview portfolio = PortfolioOverview.calculate(balance, stats, investmentsAlreadyMade);
        Investor.LOGGER.debug("Current share of unpaid loans with a given rating is: {}.",
                portfolio.getSharesOnInvestment());
        final Collection<Loan> loans = strategy.getMatchingLoans(availableLoans, portfolio).stream()
                .filter(l -> !Investor.isLoanPresent(l, investmentsAlreadyMade))
                .collect(Collectors.toList());
        Investor.LOGGER.debug("Strategy recommends the following unseen loans: {}.", loans);
        return loans.stream()
                .map(l -> {
                    final int invest = strategy.recommendInvestmentAmount(l, portfolio);
                    return Investor.invest(this.zonkyApi, l.getId(), invest, portfolio.getCzkAvailable());
                })
                .flatMap(o -> o.isPresent() ? Stream.of(o.get()) : Stream.empty())
                .findFirst();
    }

    /**
     * One of the two entry points to the investment API. This takes the strategy, determines suitable loans, and tries
     * to invest in as many of them as the balance allows.
     *
     * @param strategy The investment strategy to use.
     * @param loans Loans that are available on the marketplace. These must not include CAPTCHA-protected loans.
     * @return Investments made in the session.
     */
    public Collection<Investment> invest(final InvestmentStrategy strategy, final Collection<Loan> loans) {
        Investor.LOGGER.info("The following loans are available for robotic investing: {}.",
                loans.stream().map(Loan::getId).collect(Collectors.toList()));
        // make sure we have enough money to invest
        final BigDecimal minimumInvestmentAmount = BigDecimal.valueOf(InvestmentStrategy.MINIMAL_INVESTMENT_ALLOWED);
        BigDecimal balance = this.initialBalance;
        if (balance.compareTo(minimumInvestmentAmount) < 0) {
            return Collections.emptyList(); // no need to do anything else
        }
        Collection<Investment> investments =
                Investor.retrieveInvestmentsRepresentedByBlockedAmounts(this.zonkyApi, this.executor);
        Investor.LOGGER.debug("The following loans are coming from the API as already invested into: {}",
                investments.stream().map(BaseInvestment::getLoanId).collect(Collectors.toList()));
        final Statistics stats = Investor.retrieveStatistics(this.zonkyApi);
        Investor.LOGGER.debug("The sum total of principal remaining on active loans is {} CZK.",
                stats.getCurrentOverview().getPrincipalLeft());
        // and start investing
        final List<Loan> availableLoans = new ArrayList<>(loans);
        final Collection<Investment> investmentsMade = new ArrayList<>();
        do {
            final Optional<Investment> investment =
                    this.investOnce(strategy, availableLoans, balance, stats, investments);
            if (!investment.isPresent()) { // there is nothing to invest into; RoboZonky is finished now
                break;
            }
            final Investment i = investment.get();
            investmentsMade.add(i);
            investments = Investor.mergeInvestments(investments, Collections.singletonList(i));
            balance = balance.subtract(BigDecimal.valueOf(i.getAmount()));
            Investor.LOGGER.info("New account balance is {} CZK.", balance);
        } while (balance.compareTo(minimumInvestmentAmount) >= 0);
        return Collections.unmodifiableCollection(investmentsMade);
    }

    /**
     * One of the two entry points to the investment API. Takes a particular loan and invests a given amount into it.
     *
     * @param loanId ID of the loan that should be invested into.
     * @param loanAmount Amount in CZK to be invested.
     * @return Present if investment succeeded, empty otherwise.
     */
    public Optional<Investment> invest(final int loanId, final int loanAmount) {
        return Investor.invest(this.zonkyApi, loanId, loanAmount, this.initialBalance.intValue());
    }

}
