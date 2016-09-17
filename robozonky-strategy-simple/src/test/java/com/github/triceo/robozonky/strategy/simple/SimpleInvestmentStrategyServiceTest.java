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

package com.github.triceo.robozonky.strategy.simple;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Arrays;

import com.github.triceo.robozonky.strategy.InvestmentStrategyParseException;
import com.github.triceo.robozonky.util.IoTestUtil;
import org.assertj.core.api.Assertions;
import org.junit.Test;

public class SimpleInvestmentStrategyServiceTest {

    private static final InputStream PROPER =
            SimpleInvestmentStrategyServiceTest.class.getResourceAsStream("strategy-sample.cfg");
    private static final InputStream IMPROPER =
            SimpleInvestmentStrategyServiceTest.class.getResourceAsStream("strategy-sample.badext");
    private static final InputStream WRONG_SHARES =
            SimpleInvestmentStrategyServiceTest.class.getResourceAsStream("strategy-wrongshares.cfg");
    private static final InputStream WRONG_TERMS =
            SimpleInvestmentStrategyServiceTest.class.getResourceAsStream("strategy-wrongterms.cfg");
    private static final InputStream WRONG_ASKS =
            SimpleInvestmentStrategyServiceTest.class.getResourceAsStream("strategy-wrongasks.cfg");
    private static final File NONEXISTENT = new File("strategy-nonexistent.cfg");

    @Test
    public void proper() throws InvestmentStrategyParseException, IOException {
        final File f = IoTestUtil.streamToFile(SimpleInvestmentStrategyServiceTest.PROPER, ".cfg");
        final SimpleInvestmentStrategyService s = new SimpleInvestmentStrategyService();
        Assertions.assertThat(s.isSupported(f)).isTrue();
        Assertions.assertThat(s.parse(f)).isNotNull();
    }

    @Test(expected = InvestmentStrategyParseException.class)
    public void nonexistent() throws InvestmentStrategyParseException {
        final SimpleInvestmentStrategyService s = new SimpleInvestmentStrategyService();
        Assertions.assertThat(s.isSupported(SimpleInvestmentStrategyServiceTest.NONEXISTENT)).isTrue();
        s.parse(SimpleInvestmentStrategyServiceTest.NONEXISTENT);
    }

    @Test
    public void improper() throws InvestmentStrategyParseException, IOException {
        final File f = IoTestUtil.streamToFile(SimpleInvestmentStrategyServiceTest.IMPROPER);
        final SimpleInvestmentStrategyService s = new SimpleInvestmentStrategyService();
        Assertions.assertThat(s.isSupported(f)).isFalse();
    }

    @Test
    public void wrongShares() throws InvestmentStrategyParseException, IOException {
        final File f = IoTestUtil.streamToFile(SimpleInvestmentStrategyServiceTest.WRONG_SHARES);
        final SimpleInvestmentStrategyService s = new SimpleInvestmentStrategyService();
        Assertions.assertThat(s.isSupported(f)).isFalse();
    }

    @Test(expected = InvestmentStrategyParseException.class)
    public void wrongTerms() throws InvestmentStrategyParseException, IOException {
        final File f = IoTestUtil.streamToFile(SimpleInvestmentStrategyServiceTest.WRONG_TERMS, ".cfg");
        final SimpleInvestmentStrategyService s = new SimpleInvestmentStrategyService();
        s.parse(f);
    }

    @Test(expected = InvestmentStrategyParseException.class)
    public void wrongAsks() throws InvestmentStrategyParseException, IOException {
        final File f = IoTestUtil.streamToFile(SimpleInvestmentStrategyServiceTest.WRONG_ASKS, ".cfg");
        final SimpleInvestmentStrategyService s = new SimpleInvestmentStrategyService();
        s.parse(f);
    }


    @Test
    public void whitespace() throws InvestmentStrategyParseException, IOException {
        // my IDE keeps removing whitespace at the end of lines in files, so let's generate a file on the run
        final String[] lines = new String[] {
            "minimumBalance                = 200 ", "maximumInvestment             = 20000\t",
                "   maximumShare.default          = 1", "targetShare.default           = 0.125   ",
                "minimumTerm.default           = \t0  ", "maximumTerm.default           = -1",
                "minimumAsk.default            = 0", "maximumAsk.default            = -1   ",
                "minimumLoanAmount.default     = 200", "maximumLoanAmount.default     = 400",
                "minimumLoanShare.default      = 0", "maximumLoanShare.default      = 0.01",
                "preferLongerTerms.default     = false   ",
        };
        final File f = File.createTempFile("robozonky-", ".cfg");
        Files.write(f.toPath(), Arrays.asList(lines));
        // make sure that the file reads properly
        final SimpleInvestmentStrategyService s = new SimpleInvestmentStrategyService();
        s.parse(f);
    }
}
