package com.github.robozonky.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class BootstrapUtil {

    private BootstrapUtil() {
        // no instances
    }

    /**
     * Transfers java.util.logging to Log4j. Make sure no {@link Logger} is instantiated before this method is called.
     */
    public static void configureLogging() {
        System.getProperty("java.util.logging.manager", "org.apache.logging.log4j.jul.LogManager");
        LogManager.getLogger(BootstrapUtil.class).debug("Attempted to forward java.util.logging to Log4j.");
    }
}
