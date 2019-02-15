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

package com.github.robozonky.api.remote.entities.sanitized;

import java.net.URL;
import java.util.Optional;

import com.github.robozonky.api.remote.entities.MyInvestment;
import com.github.robozonky.api.remote.entities.RawLoan;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@SuppressWarnings("unchecked")
abstract class AbstractMutableLoanImpl<T extends MutableMarketplaceLoan<T>> extends AbstractBaseLoanImpl<T>
        implements MutableMarketplaceLoan<T> {

    private static final Logger LOGGER = LogManager.getLogger(AbstractMutableLoanImpl.class);
    private URL url;
    private MyInvestment myInvestment;

    AbstractMutableLoanImpl() {
    }

    AbstractMutableLoanImpl(final RawLoan original) {
        super(original);
        LOGGER.trace("Sanitizing loan #{}.", original.getId());
        this.myInvestment = original.getMyInvestment();
        this.url = Util.getUrlSafe(original);
        setInsuranceHistory(original.getInsuranceHistory());
    }

    @Override
    public URL getUrl() {
        return url;
    }

    @Override
    public T setUrl(final URL url) {
        this.url = url;
        return (T) this;
    }

    @Override
    public Optional<MyInvestment> getMyInvestment() {
        return Optional.ofNullable(myInvestment);
    }

    @Override
    public T setMyInvestment(final MyInvestment myInvestment) {
        this.myInvestment = myInvestment;
        return (T) this;
    }

}
