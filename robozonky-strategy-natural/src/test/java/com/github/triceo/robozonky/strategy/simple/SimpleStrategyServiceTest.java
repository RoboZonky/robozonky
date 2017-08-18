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

package com.github.triceo.robozonky.strategy.simple;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

import com.github.triceo.robozonky.api.strategies.InvestmentStrategy;
import com.github.triceo.robozonky.internal.api.Defaults;
import org.apache.commons.io.IOUtils;
import org.assertj.core.api.Assertions;
import org.junit.Test;

public class SimpleStrategyServiceTest {

    private static final InputStream REAL_LIFE =
            SimpleStrategyServiceTest.class.getResourceAsStream("strategy-complex.cfg");
    private static final InputStream PROPER =
            SimpleStrategyServiceTest.class.getResourceAsStream("strategy-sample.cfg");
    private static final InputStream PARTIAL =
            SimpleStrategyServiceTest.class.getResourceAsStream("strategy-partial.cfg");
    private static final InputStream IMPROPER =
            SimpleStrategyServiceTest.class.getResourceAsStream("strategy-sample.badext");
    private static final InputStream WRONG_SHARES =
            SimpleStrategyServiceTest.class.getResourceAsStream("strategy-wrongshares.cfg");
    private static final InputStream WRONG_TERMS =
            SimpleStrategyServiceTest.class.getResourceAsStream("strategy-wrongterms.cfg");
    private static final InputStream WRONG_ASKS =
            SimpleStrategyServiceTest.class.getResourceAsStream("strategy-wrongasks.cfg");

    private static String toString(final InputStream s) throws IOException {
        return IOUtils.readLines(s, Defaults.CHARSET).stream().collect(Collectors.joining(System.lineSeparator()));
    }

    @Test
    public void shareSumsUnder100Percent() throws IOException {
        final SimpleStrategyService s = new SimpleStrategyService();
        Assertions.assertThat(s.toInvest(toString(SimpleStrategyServiceTest.PARTIAL))).isPresent();
    }

    @Test
    public void proper() throws IOException {
        final SimpleStrategyService s = new SimpleStrategyService();
        Assertions.assertThat(s.toInvest(toString(SimpleStrategyServiceTest.PROPER))).isPresent();
    }

    @Test
    public void improper() throws IOException {
        final SimpleStrategyService s = new SimpleStrategyService();
        Assertions.assertThat(s.toInvest(toString(SimpleStrategyServiceTest.IMPROPER))).isEmpty();
    }

    @Test
    public void wrongShares() throws IOException {
        final SimpleStrategyService s = new SimpleStrategyService();
        Assertions.assertThat(s.toInvest(toString(SimpleStrategyServiceTest.WRONG_SHARES))).isEmpty();
    }

    @Test
    public void wrongTerms() throws IOException {
        final SimpleStrategyService s = new SimpleStrategyService();
        final Optional<InvestmentStrategy> result = s.toInvest(toString(SimpleStrategyServiceTest.WRONG_TERMS));
        Assertions.assertThat(result).isEmpty();
    }

    @Test
    public void wrongAsks() throws IOException {
        final SimpleStrategyService s = new SimpleStrategyService();
        final Optional<InvestmentStrategy> result = s.toInvest(toString(SimpleStrategyServiceTest.WRONG_ASKS));
        Assertions.assertThat(result).isEmpty();
    }

    @Test
    public void complex() throws IOException {
        final SimpleStrategyService s = new SimpleStrategyService();
        Assertions.assertThat(s.toInvest(toString(SimpleStrategyServiceTest.REAL_LIFE))).isPresent();
        Assertions.assertThat(s.toSell(toString(SimpleStrategyServiceTest.REAL_LIFE))).isEmpty();
        Assertions.assertThat(s.toPurchase(toString(SimpleStrategyServiceTest.REAL_LIFE))).isEmpty();
    }

    @Test
    public void whitespace() throws IOException {
        // my IDE keeps removing whitespace at the end of lines in files, so let's generate a file on the run
        final String[] lines = new String[]{
                "minimumBalance                = 200 ", "maximumInvestment             = 20000\t",
                "   maximumShare.default          = 1", "targetShare.default           = 0.125   ",
                "minimumTerm.default           = \t0  ", "maximumTerm.default           = -1",
                "minimumAsk.default            = 0", "maximumAsk.default            = -1   ",
                "minimumLoanAmount.default     = 200", "maximumLoanAmount.default     = 400",
                "minimumLoanShare.default      = 0", "maximumLoanShare.default      = 0.01",
                "preferLongerTerms.default     = false   ",
                "requireConfirmation.default = true"
        };
        final File f = File.createTempFile("robozonky-", ".cfg");
        Files.write(f.toPath(), Arrays.asList(lines));
        // make sure that the file reads properly
        final SimpleStrategyService s = new SimpleStrategyService();
        s.toInvest(toString(f.toURI().toURL().openStream()));
    }
}
