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

package com.github.robozonky.marketplaces;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.function.Consumer;
import java.util.stream.Stream;

import com.github.robozonky.api.marketplaces.Marketplace;
import com.github.robozonky.api.remote.entities.Loan;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import static org.junit.jupiter.api.DynamicTest.dynamicTest;

class AbstractMarketplaceTest {

    private static void retrieval(final Class<? extends Marketplace> marketClass) throws Exception {
        final Consumer<Collection<Loan>> consumer = Mockito.mock(Consumer.class);
        try (final Marketplace market = marketClass.getConstructor().newInstance()) {
            Mockito.verify(consumer, Mockito.never()).accept(ArgumentMatchers.any());
            Assertions.assertThat(market.registerListener(consumer)).isTrue();
            market.run();
            Mockito.verify(consumer, Mockito.times(1))
                    .accept(ArgumentMatchers.argThat(argument -> argument != null && !argument.isEmpty()));
        } catch (final InvocationTargetException | NoSuchMethodException ex) {
            Assertions.fail("Failed creating marketplace instance.", ex);
        }
    }

    @TestFactory
    public Stream<DynamicTest> marketplaces() {
        return Stream.of(
                dynamicTest("Zonky", () -> retrieval(ZonkyMarketplace.class)),
                dynamicTest("Zotify", () -> retrieval(ZotifyMarketplace.class))
        );
    }
}
