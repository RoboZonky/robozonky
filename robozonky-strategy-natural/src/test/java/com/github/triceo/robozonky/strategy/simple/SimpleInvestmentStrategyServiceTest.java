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

package com.github.triceo.robozonky.strategy.simple;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Optional;

import com.github.triceo.robozonky.api.strategies.InvestmentStrategy;
import com.github.triceo.robozonky.util.IoTestUtil;
import org.assertj.core.api.Assertions;
import org.junit.Test;

public class SimpleInvestmentStrategyServiceTest {

    private static final InputStream PROPER =
            SimpleInvestmentStrategyServiceTest.class.getResourceAsStream("strategy-sample.cfg");
    private static final InputStream PARTIAL =
            SimpleInvestmentStrategyServiceTest.class.getResourceAsStream("strategy-partial.cfg");
    private static final InputStream IMPROPER =
            SimpleInvestmentStrategyServiceTest.class.getResourceAsStream("strategy-sample.badext");
    private static final InputStream WRONG_SHARES =
            SimpleInvestmentStrategyServiceTest.class.getResourceAsStream("strategy-wrongshares.cfg");
    private static final InputStream WRONG_TERMS =
            SimpleInvestmentStrategyServiceTest.class.getResourceAsStream("strategy-wrongterms.cfg");
    private static final InputStream WRONG_ASKS =
            SimpleInvestmentStrategyServiceTest.class.getResourceAsStream("strategy-wrongasks.cfg");

    @Test
    public void shareSumsUnder100Percent() throws IOException {
        final File f = IoTestUtil.streamToFile(SimpleInvestmentStrategyServiceTest.PARTIAL, ".cfg");
        final SimpleInvestmentStrategyService s = new SimpleInvestmentStrategyService();
        Assertions.assertThat(s.parse(f.toURI().toURL().openStream())).isPresent();
    }

    @Test
    public void proper() throws IOException {
        final File f = IoTestUtil.streamToFile(SimpleInvestmentStrategyServiceTest.PROPER, ".cfg");
        final SimpleInvestmentStrategyService s = new SimpleInvestmentStrategyService();
        Assertions.assertThat(s.parse(f.toURI().toURL().openStream())).isPresent();
    }

    @Test
    public void improper() throws IOException {
        final File f = IoTestUtil.streamToFile(SimpleInvestmentStrategyServiceTest.IMPROPER);
        final SimpleInvestmentStrategyService s = new SimpleInvestmentStrategyService();
        Assertions.assertThat(s.parse(f.toURI().toURL().openStream())).isEmpty();
    }

    @Test
    public void wrongShares() throws IOException {
        final File f = IoTestUtil.streamToFile(SimpleInvestmentStrategyServiceTest.WRONG_SHARES);
        final SimpleInvestmentStrategyService s = new SimpleInvestmentStrategyService();
        Assertions.assertThat(s.parse(f.toURI().toURL().openStream())).isEmpty();
    }

    @Test
    public void wrongTerms() throws IOException {
        final File f = IoTestUtil.streamToFile(SimpleInvestmentStrategyServiceTest.WRONG_TERMS, ".cfg");
        final SimpleInvestmentStrategyService s = new SimpleInvestmentStrategyService();
        final Optional<InvestmentStrategy> result = s.parse(f.toURI().toURL().openStream());
        Assertions.assertThat(result).isEmpty();
    }

    @Test
    public void wrongAsks() throws IOException {
        final File f = IoTestUtil.streamToFile(SimpleInvestmentStrategyServiceTest.WRONG_ASKS, ".cfg");
        final SimpleInvestmentStrategyService s = new SimpleInvestmentStrategyService();
        final Optional<InvestmentStrategy> result = s.parse(f.toURI().toURL().openStream());
        Assertions.assertThat(result).isEmpty();
    }

    @Test
    public void whitespace() throws IOException {
        // my IDE keeps removing whitespace at the end of lines in files, so let's generate a file on the run
        final String[] lines = new String[] {
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
        final SimpleInvestmentStrategyService s = new SimpleInvestmentStrategyService();
        s.parse(f.toURI().toURL().openStream());
    }
}
