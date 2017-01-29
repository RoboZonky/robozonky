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

package com.github.triceo.robozonky.marketplaces;

import java.util.Collection;
import java.util.function.Consumer;
import javax.ws.rs.NotAllowedException;
import javax.ws.rs.ProcessingException;

import com.github.triceo.robozonky.api.marketplaces.ExpectedTreatment;
import com.github.triceo.robozonky.api.remote.entities.Loan;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.Assume;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

public class ZotifyMarketplaceTest {

    @Test
    public void retrieval() {
        final Consumer<Collection<Loan>> consumer = Mockito.mock(Consumer.class);
        try (final ZotifyMarketplace market = new ZotifyMarketplace()) {
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(market.specifyExpectedTreatment()).isEqualTo(ExpectedTreatment.POLLING);
                softly.assertThat(market.registerListener(consumer)).isTrue();
            });
            market.run();
        } catch (final ProcessingException | NotAllowedException e) {
            Assume.assumeTrue(false); // Zotify is not available, test makes no sense
        } catch (final Exception e) {
            Assertions.fail("Unexpected exception.", e);
        }
        Mockito.verify(consumer, Mockito.times(1)).accept(ArgumentMatchers.any());
    }

}
