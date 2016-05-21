/*
 *
 *  * Copyright 2016 Lukáš Petrovický
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 * /
 */
package com.github.triceo.robozonky.app;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;

import com.github.triceo.robozonky.Operations;
import com.github.triceo.robozonky.OperationsContext;
import com.github.triceo.robozonky.remote.Investment;
import com.github.triceo.robozonky.remote.Loan;
import com.github.triceo.robozonky.strategy.InvestmentStrategy;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class App {

    private static final Logger LOGGER = LoggerFactory.getLogger(App.class);

    private static final Option OPTION_STRATEGY = Option.builder("s").hasArg().longOpt("strategy")
            .argName("Investment strategy").desc("Points to a file that holds the investment strategy configuration.")
            .build();
    private static final Option OPTION_USERNAME = Option.builder("u").hasArg().longOpt("username")
            .argName("Zonky username").desc("Used to connect to the Zonky server.").build();
    private static final Option OPTION_PASSWORD = Option.builder("p").hasArg().longOpt("password")
            .argName("Zonky password").desc("Used to connect to the Zonky server.").build();
    private static final Option OPTION_DRY_RUN = Option.builder("d").hasArg().optionalArg(true).
            argName("Dry run balance").longOpt("dry").desc("Simulate the investments, but never actually spend money.")
            .build();
    private static final Option OPTION_HELP = Option.builder("h").longOpt("help").argName("Show help")
            .desc("Show this help message and quit.").build();

    private static void printHelpAndExit(final Options options, final String message, final boolean exitWithError) {
        final HelpFormatter formatter = new HelpFormatter();
        final String scriptName = System.getProperty("os.name").contains("Windows") ? "robozonky.bat" : "robozonky.sh";
        formatter.printHelp(scriptName, null, options, exitWithError ? "Error: " + message : message, true);
        System.exit(exitWithError ? 1 : 0);
    }

    public static void main(final String... args) {
        final OptionGroup og = new OptionGroup();
        og.setRequired(true);
        og.addOption(App.OPTION_HELP);
        og.addOption(App.OPTION_STRATEGY);
        final Options options = new Options();
        options.addOptionGroup(og);
        options.addOption(App.OPTION_DRY_RUN);
        options.addOption(App.OPTION_PASSWORD);
        options.addOption(App.OPTION_USERNAME);

        final CommandLineParser parser = new DefaultParser();
        CommandLine cmd = null;
        try {
            cmd = parser.parse(options, args);
        } catch (final Exception ex) { // for some reason, the CLI could not be parsed
            App.printHelpAndExit(options, ex.getMessage(), true);
        }
        if (cmd.hasOption(App.OPTION_HELP.getOpt())) { // user requested help
            App.printHelpAndExit(options, "", false);
        }
        LOGGER.info("RoboZonky v{} loading.", Operations.getRoboZonkyVersion());
        // standard workflow
        if (!cmd.hasOption(App.OPTION_USERNAME.getOpt())) {
            App.printHelpAndExit(options, "Username must be provided.", true);
        } else if (!cmd.hasOption(App.OPTION_PASSWORD.getOpt())) {
            App.printHelpAndExit(options, "Password must be provided.", true);
        }
        final File strategyConfig = new File(cmd.getOptionValue(App.OPTION_STRATEGY.getOpt()));
        if (!strategyConfig.canRead()) {
            App.printHelpAndExit(options, "Investment strategy file must be readable.", true);
        }
        InvestmentStrategy strategy = null;
        try {
            strategy = StrategyParser.parse(strategyConfig);
        } catch (final Exception e) {
            App.printHelpAndExit(options, "Failed parsing strategy: " + e.getMessage(), true);
        }
        App.letsGo(cmd, strategy); // and start actually working with Zonky
    }

    private static void storeInvestmentsMade(final Collection<Investment> result, final boolean dryRun) {
        final String suffix = dryRun ? "dry" : "invested";
        final LocalDateTime now = LocalDateTime.now();
        final String filename =
                "robozonky." + DateTimeFormatter.ofPattern("yyyyMMddHHmm").format(now) + '.' + suffix;
        try (final BufferedWriter bw = new BufferedWriter(new FileWriter(new File(filename)))) {
            for (final Investment i : result) {
                final Loan l = i.getLoan();
                bw.write('#' + i.getLoanId() + " ('" + l.getName() + "', " + l.getRating().getDescription() + "): "
                        + i.getAmount() + " CZK");
                bw.newLine();
            }
            App.LOGGER.info("Investments made by RoboZonky during the session were stored in file '{}'.", filename);
        } catch (final IOException ex) {
            App.LOGGER.warn("Failed writing out the list of investments made in this session.", ex);
        }
    }

    private static void letsGo(final CommandLine cmd, final InvestmentStrategy strategy) {
        App.LOGGER.info("===== RoboZonky at your service! =====");
        final String username = cmd.getOptionValue(App.OPTION_USERNAME.getOpt());
        final String password = cmd.getOptionValue(App.OPTION_PASSWORD.getOpt());
        final boolean dryRun = cmd.hasOption(App.OPTION_DRY_RUN.getOpt());
        final int startingBalance = Integer.valueOf(cmd.getOptionValue(App.OPTION_DRY_RUN.getOpt(), "-1")); // FIXME throws
        if (dryRun) {
            App.LOGGER.info("RoboZonky is doing a dry run. It will simulate investing, but not invest any real money.");
        }
        final Collection<Investment> result = App.operate(username, password, strategy, dryRun, startingBalance);
        if (result.size() == 0) {
            App.LOGGER.info("RoboZonky did not invest.");
        } else {
            App.storeInvestmentsMade(result, dryRun);
            if (dryRun) {
                App.LOGGER.info("RoboZonky pretended to invest into {} loans.", result.size());
            } else {
                App.LOGGER.info("RoboZonky invested into {} loans.", result.size());
            }
        }
        App.LOGGER.info("===== RoboZonky out. =====");
        System.exit(0); // make sure the app actually quits
    }

    private static Collection<Investment> operate(final String username, final String password,
                                                  final InvestmentStrategy strategy, final boolean dryRun,
                                                  final int startingBalance) {
        final OperationsContext oc = Operations.login(username, password, strategy, dryRun, startingBalance);
        final Collection<Investment> result = Operations.invest(oc);
        Operations.logout(oc);
        return result;
    }

}
