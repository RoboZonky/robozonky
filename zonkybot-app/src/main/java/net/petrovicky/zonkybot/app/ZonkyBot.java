/*
 * Copyright 2016 Lukáš Petrovický
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.petrovicky.zonkybot.app;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;

import net.petrovicky.zonkybot.Operations;
import net.petrovicky.zonkybot.remote.Investment;
import net.petrovicky.zonkybot.strategy.InvestmentStrategy;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ZonkyBot {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZonkyBot.class);

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

    private static void printHelpAndExit(Options options, String message, boolean exitWithError) {
        final HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("ZonkyBot", null, options, exitWithError ? "Error: " + message : message, true);
        System.exit(exitWithError ? 1 : 0);
    }

    public static void main(String[] args) {
        final OptionGroup og = new OptionGroup();
        og.setRequired(true);
        og.addOption(OPTION_HELP);
        og.addOption(OPTION_STRATEGY);
        final Options options = new Options();
        options.addOptionGroup(og);
        options.addOption(OPTION_DRY_RUN);
        options.addOption(OPTION_PASSWORD);
        options.addOption(OPTION_USERNAME);

        final CommandLineParser parser = new DefaultParser();
        CommandLine cmd = null;
        try {
            cmd = parser.parse(options, args);
        } catch (Exception ex) { // for some reason, the CLI could not be parsed
            printHelpAndExit(options, ex.getMessage(), true);
        }
        if (cmd.hasOption(OPTION_HELP.getOpt())) { // user requested help
            printHelpAndExit(options, "", false);
        }
        // standard workflow
        if (!cmd.hasOption(OPTION_USERNAME.getOpt())) {
            printHelpAndExit(options, "Username must be provided.", true);
        } else if (!cmd.hasOption(OPTION_PASSWORD.getOpt())) {
            printHelpAndExit(options, "Password must be provided.", true);
        }
        final File strategyConfig = new File(cmd.getOptionValue(OPTION_STRATEGY.getOpt()));
        if (!strategyConfig.canRead()) {
            printHelpAndExit(options, "Investment strategy file must be readable.", true);
        }
        InvestmentStrategy strategy = null;
        try {
            strategy = StrategyParser.parse(strategyConfig);
        } catch (Exception e) {
            printHelpAndExit(options, "Failed parsing strategy: " + e.getMessage(), true);
        }
        letsGo(cmd, strategy); // and start actually working with Zonky
    }

    private static void letsGo(final CommandLine cmd, final InvestmentStrategy strategy) {
        LOGGER.info("===== ZonkyBot at your service! =====");
        final String username = cmd.getOptionValue(OPTION_USERNAME.getOpt());
        final String password = cmd.getOptionValue(OPTION_PASSWORD.getOpt());
        LOGGER.info("Will communicate with Zonky as user '{}'.", username);
        final boolean dryRun = cmd.hasOption(OPTION_DRY_RUN.getOpt());
        int startingBalance = Integer.valueOf(cmd.getOptionValue(OPTION_DRY_RUN.getOpt(), "-1")); // FIXME throws
        if (dryRun) {
            LOGGER.info("ZonkyBot is doing a dry run. It will simulate investing, but not invest any real money.");
        }
        final List<Investment> result = operate(new Operations(username, password, strategy, dryRun, startingBalance));
        if (result.size() == 0) {
            LOGGER.info("ZonkyBot did not invest.");
        } else if (dryRun) {
            LOGGER.info("ZonkyBot pretended to invest into {} loans.", result);
        } else {
            final String filename = "zonkybot." + DateTimeFormatter.ofPattern("YYYY-MM-dd-HH:mm").format(Instant.now()) + ".invested";
            try (final BufferedWriter bw = new BufferedWriter(new FileWriter(new File(filename)))) {
                for (final Investment i : result) {
                    bw.write(i.getLoanId() + "('" + i.getLoan().getName() + "', " + i.getLoan().getRating() + "): "
                            + i.getInvestedAmount() + " CZK");
                    bw.newLine();
                }
            } catch (IOException ex) {
                LOGGER.warn("Failed writing out the list of investments made in this session.", ex);
            }
            LOGGER.info("ZonkyBot invested into {} loans.", result);
        }
        LOGGER.info("===== ZonkyBot out. =====");
    }

    private static List<Investment> operate(final Operations ops) {
        ops.login();
        final List<Investment> result = ops.invest();
        ops.logout();
        return result;
    }

}
