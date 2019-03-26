package com.github.robozonky.installer;

import java.net.URL;

import com.izforge.izpack.panels.userinput.processorclient.ProcessingClient;
import com.izforge.izpack.panels.userinput.validator.Validator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

class UrlValidator implements Validator {

    protected static final Logger LOGGER = LogManager.getLogger();

    @Override
    public boolean validate(final ProcessingClient client) {
        final String text = client.getText();
        try {
            final URL url = new URL(text);
            return true;
        } catch (final Exception ex) {
            LOGGER.error("Wrong URL: {}.", text, ex);
            return false;
        }
    }
}
