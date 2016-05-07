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
import java.net.URL;
import java.net.URLClassLoader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Properties;

import net.petrovicky.zonkybot.Operations;
import net.petrovicky.zonkybot.OperationsContext;
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

class ZonkyBot {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZonkyBot.class);
    protected static final String ZONKY_VERSION_UNDETECTED = "UNDETECTED";
    protected static final String ZONKY_VERSION_UNKNOWN = "UNKNOWN";

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

    protected static String getZonkyBotVersion() {
        try {
            final URLClassLoader cl = (URLClassLoader) ZonkyBot.class.getClassLoader();
            final URL url = cl.findResource("META-INF/maven/net.petrovicky.zonkybot/zonkybot-app/pom.properties");
            final Properties props = new Properties();
            props.load(url.openStream());
            return props.getProperty("version", ZonkyBot.ZONKY_VERSION_UNKNOWN);
        } catch (Exception ex) {
            return ZonkyBot.ZONKY_VERSION_UNDETECTED;
        }
    }

    private static void printHelpAndExit(final Options options, final String message, final boolean exitWithError) {
        final HelpFormatter formatter = new HelpFormatter();
        // FIXME needs to use run.bat / run.sh
        formatter.printHelp("ZonkyBot", null, options, exitWithError ? "Error: " + message : message, true);
        System.exit(exitWithError ? 1 : 0);
    }

    public static void main(final String... args) {
        final OptionGroup og = new OptionGroup();
        og.setRequired(true);
        og.addOption(ZonkyBot.OPTION_HELP);
        og.addOption(ZonkyBot.OPTION_STRATEGY);
        final Options options = new Options();
        options.addOptionGroup(og);
        options.addOption(ZonkyBot.OPTION_DRY_RUN);
        options.addOption(ZonkyBot.OPTION_PASSWORD);
        options.addOption(ZonkyBot.OPTION_USERNAME);

        final CommandLineParser parser = new DefaultParser();
        CommandLine cmd = null;
        try {
            cmd = parser.parse(options, args);
        } catch (final Exception ex) { // for some reason, the CLI could not be parsed
            ZonkyBot.printHelpAndExit(options, ex.getMessage(), true);
        }
        if (cmd.hasOption(ZonkyBot.OPTION_HELP.getOpt())) { // user requested help
            ZonkyBot.printHelpAndExit(options, "", false);
        }
        LOGGER.info("ZonkyBot v{} loading.", ZonkyBot.getZonkyBotVersion());
        // standard workflow
        if (!cmd.hasOption(ZonkyBot.OPTION_USERNAME.getOpt())) {
            ZonkyBot.printHelpAndExit(options, "Username must be provided.", true);
        } else if (!cmd.hasOption(ZonkyBot.OPTION_PASSWORD.getOpt())) {
            ZonkyBot.printHelpAndExit(options, "Password must be provided.", true);
        }
        final File strategyConfig = new File(cmd.getOptionValue(ZonkyBot.OPTION_STRATEGY.getOpt()));
        if (!strategyConfig.canRead()) {
            ZonkyBot.printHelpAndExit(options, "Investment strategy file must be readable.", true);
        }
        InvestmentStrategy strategy = null;
        try {
            strategy = StrategyParser.parse(strategyConfig);
        } catch (final Exception e) {
            ZonkyBot.printHelpAndExit(options, "Failed parsing strategy: " + e.getMessage(), true);
        }
        ZonkyBot.letsGo(cmd, strategy); // and start actually working with Zonky
    }

    private static void storeInvestmentsMade(final Collection<Investment> result, final boolean dryRun) {
        final String suffix = dryRun ? "dry" : "invested";
        final LocalDateTime now = LocalDateTime.now();
        final String filename =
                "zonkybot." + DateTimeFormatter.ofPattern("yyyyMMddHHmm").format(now) + '.' + suffix;
        try (final BufferedWriter bw = new BufferedWriter(new FileWriter(new File(filename)))) {
            for (final Investment i : result) {
                bw.write(i.getLoanId() + "('" + i.getLoan().getName() + "', " + i.getLoan().getRating() + "): "
                        + i.getAmount() + " CZK");
                bw.newLine();
            }
            ZonkyBot.LOGGER.info("Investments made by ZonkyBot during the session were stored in file '{}'.", filename);
        } catch (final IOException ex) {
            ZonkyBot.LOGGER.warn("Failed writing out the list of investments made in this session.", ex);
        }
    }

    private static void letsGo(final CommandLine cmd, final InvestmentStrategy strategy) {
        ZonkyBot.LOGGER.info("===== ZonkyBot at your service! =====");
        final String username = cmd.getOptionValue(ZonkyBot.OPTION_USERNAME.getOpt());
        final String password = cmd.getOptionValue(ZonkyBot.OPTION_PASSWORD.getOpt());
        final boolean dryRun = cmd.hasOption(ZonkyBot.OPTION_DRY_RUN.getOpt());
        final int startingBalance = Integer.valueOf(cmd.getOptionValue(ZonkyBot.OPTION_DRY_RUN.getOpt(), "-1")); // FIXME throws
        if (dryRun) {
            ZonkyBot.LOGGER.info("ZonkyBot is doing a dry run. It will simulate investing, but not invest any real money.");
        }
        final Collection<Investment> result = ZonkyBot.operate(username, password, strategy, dryRun, startingBalance);
        if (result.size() == 0) {
            ZonkyBot.LOGGER.info("ZonkyBot did not invest.");
        } else {
            ZonkyBot.storeInvestmentsMade(result, dryRun);
            if (dryRun) {
                ZonkyBot.LOGGER.info("ZonkyBot pretended to invest into {} loans.", result.size());
            } else {
                ZonkyBot.LOGGER.info("ZonkyBot invested into {} loans.", result.size());
            }
        }
        ZonkyBot.LOGGER.info("===== ZonkyBot out. =====");
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
