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

package com.github.triceo.robozonky.installer.panels;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.github.triceo.robozonky.api.confirmations.ConfirmationProvider;
import com.github.triceo.robozonky.api.confirmations.RequestId;
import com.github.triceo.robozonky.api.remote.ZonkyApi;
import com.github.triceo.robozonky.api.remote.entities.Loan;
import com.github.triceo.robozonky.common.extensions.ConfirmationProviderLoader;
import com.github.triceo.robozonky.common.remote.ApiProvider;
import com.izforge.izpack.api.data.InstallData;
import com.izforge.izpack.api.installer.DataValidator;

public class ZonkoidSettingsValidator implements DataValidator {

    private static final Logger LOGGER = Logger.getLogger(ZonkoidSettingsValidator.class.getCanonicalName());
    private static final Comparator<Loan> SUBCOMPARATOR =
            Comparator.comparing(Loan::getRemainingInvestment).reversed();
    private static final Comparator<Loan> COMPARATOR =
            Comparator.comparing(Loan::getInterestRate).thenComparing(ZonkoidSettingsValidator.SUBCOMPARATOR);

    static Optional<Loan> getOneLoanFromMarketplace(final Supplier<ApiProvider> provider) {
        try (final ApiProvider p = provider.get()) { // test login with the credentials
            final ApiProvider.ApiWrapper<ZonkyApi> oauth = p.anonymous();
            final Collection<Loan> loans = oauth.execute((Function<ZonkyApi, List<Loan>>) ZonkyApi::getLoans);
            /*
             * find a loan that is likely to stay on the marketplace for so long that the Zonkoid notification will
             * successfully come through.
             */
            return loans.stream().sorted(ZonkoidSettingsValidator.COMPARATOR).findFirst();
        } catch (final Throwable t) {
            ZonkoidSettingsValidator.LOGGER.log(Level.WARNING, "Failed obtaining a loan.", t);
            return Optional.empty();
        }
    }

    private final Supplier<ApiProvider> apiSupplier;
    private final Supplier<Optional<ConfirmationProvider>> zonkoidSupplier;

    /**
     * Default constructor for the IZPack.
     */
    public ZonkoidSettingsValidator() {
        this(ApiProvider::new);
    }

    /**
     * This constructor exists for testing purposes.
     * @param apiSupplier Will provide the APIs for this class.
     */
    ZonkoidSettingsValidator(final Supplier<ApiProvider> apiSupplier) {
        this(apiSupplier, () -> ConfirmationProviderLoader.load("zonkoid"));
    }

    /**
     * This constructor exists for testing purposes.
     * @param apiSupplier Will provide the APIs for this class.
     * @param zonkoidSupplier Confirmation provider for this class.
     */
    ZonkoidSettingsValidator(final Supplier<ApiProvider> apiSupplier,
                             final Supplier<Optional<ConfirmationProvider>> zonkoidSupplier) {
        this.apiSupplier = apiSupplier;
        this.zonkoidSupplier = zonkoidSupplier;
    }

    static DataValidator.Status notifyZonkoid(final Loan loan, final ConfirmationProvider zonkoid,
                                              final InstallData data) {
        final String username = Variables.ZONKY_USERNAME.getValue(data);
        final String token = Variables.ZONKOID_TOKEN.getValue(data);
        final RequestId id = new RequestId(username, token.toCharArray());
        return zonkoid.requestConfirmation(id, loan.getId(), 200)
                .map(c -> {
                    switch (c.getType()) {
                        case DELEGATED:
                            return DataValidator.Status.OK;
                        default:
                            return DataValidator.Status.WARNING;
                    }
                }).orElse(DataValidator.Status.ERROR);
    }

    @Override
    public DataValidator.Status validateData(final InstallData installData) {
        return ZonkoidSettingsValidator.getOneLoanFromMarketplace(apiSupplier)
                .map(l -> zonkoidSupplier.get()
                        .map(z -> ZonkoidSettingsValidator.notifyZonkoid(l, z, installData))
                        .orElse(DataValidator.Status.ERROR))
                .orElse(DataValidator.Status.WARNING);
    }

    @Override
    public String getErrorMessageId() {
        return "Nepodařilo se odeslat notifikaci do mobilní aplikace. " +
                "Kod nebylo možné ověřit.";
    }

    @Override
    public String getWarningMessageId() {
        return "Došlo k chybě při kontaktování serveru Zonkoid. " +
                "Budete-li pokračovat, mobilní notifikace nemusí fungovat správně.";
    }

    @Override
    public boolean getDefaultAnswer() {
        return false;
    }
}
