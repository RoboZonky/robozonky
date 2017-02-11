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

package com.github.triceo.robozonky.app.investing;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.Semaphore;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.github.triceo.robozonky.api.remote.entities.Investment;
import com.github.triceo.robozonky.api.remote.entities.Loan;
import com.github.triceo.robozonky.api.strategies.LoanDescriptor;
import com.github.triceo.robozonky.app.authentication.ApiProvider;
import com.github.triceo.robozonky.app.authentication.AuthenticationHandler;

public class DirectInvestmentMode extends AbstractInvestmentMode {

    private static final class DirectInvestmentCommand implements InvestmentCommand {

        private final int loanId, loanAmount;

        public DirectInvestmentCommand(final int loanId, final int loanAmount) {
            this.loanId = loanId;
            this.loanAmount = loanAmount;
        }

        @Override
        public Collection<LoanDescriptor> getLoans() {
            return Collections.singletonList(new LoanDescriptor(new Loan(loanId, loanAmount)));
        }

        @Override
        public Collection<Investment> apply(final Investor investor) {
            final Optional<Investment> optional = investor.invest(loanId, loanAmount, ResultTracker.CAPTCHA_DELAY);
            return (optional.isPresent()) ? Collections.singletonList(optional.get()) : Collections.emptyList();
        }

        public int getLoanId() {
            return loanId;
        }

        public int getLoanAmount() {
            return loanAmount;
        }
    }

    private final DirectInvestmentMode.DirectInvestmentCommand investmentCommand;

    public DirectInvestmentMode(final AuthenticationHandler auth, final ZonkyProxy.Builder builder,
                                final boolean isFaultTolerant, final int loanId, final int loanAmount) {
        super(auth, builder, isFaultTolerant);
        this.investmentCommand = new DirectInvestmentMode.DirectInvestmentCommand(loanId, loanAmount);
    }

    Collection<Investment> invest(final ApiProvider apiProvider) {
        return this.getAuthenticationHandler().execute(apiProvider, api -> {
            final ZonkyProxy proxy = getProxyBuilder().build(api);
            return StrategyExecution.invest(proxy, investmentCommand);
        });
    }

    @Override
    protected void openMarketplace(final Consumer<Collection<Loan>> target) {
        final DirectInvestmentMode.DirectInvestmentCommand c = investmentCommand;
        target.accept(Collections.singleton(new Loan(c.getLoanId(), c.getLoanAmount())));
    }

    @Override
    protected Function<Collection<LoanDescriptor>, Collection<Investment>> getInvestor(final ApiProvider apiProvider) {
        return (loans) -> loans.stream()
            .map(ld -> this.invest(apiProvider))
            .flatMap(Collection::stream)
            .collect(Collectors.toList());
    }

    @Override
    protected Optional<Collection<Investment>> execute(final ApiProvider apiProvider) {
        return this.execute(apiProvider, new Semaphore(1));
    }
}
