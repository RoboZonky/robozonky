package com.github.robozonky.notifications.templates;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Locale;

import freemarker.cache.TemplateLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

// for whatever reason, the original template loader from Freemarker does not work on the module path
final class CustomTemplateLoader implements TemplateLoader {

    private static final Logger LOGGER = LogManager.getLogger();
    private final Locale locale;
    private final Class<?> templateRoot;

    public CustomTemplateLoader(final Class<?> templateRoot, final Locale locale) {
        LOGGER.trace("Creating template loader for {} in {}.", locale, templateRoot);
        this.locale = locale;
        this.templateRoot = templateRoot;
    }

    @Override
    public Object findTemplateSource(final String name) {
        return name;
    }

    @Override
    public long getLastModified(final Object templateSource) {
        return -1;
    }

    @Override
    public Reader getReader(final Object templateSource, final String encoding) throws IOException {
        final String actualName = ((String)templateSource).replaceFirst("\\Q_" + locale + ".ftl\\E", ".ftl");
        LOGGER.trace("Converted {} to {}.", templateSource, actualName);
        final InputStream is = templateRoot.getResourceAsStream(actualName);
        if (is == null) {
            return null;
        }
        final InputStreamReader isr = new InputStreamReader(is, encoding);
        return new BufferedReader(isr);
    }

    @Override
    public void closeTemplateSource(final Object templateSource) {
        // nothing to do here
    }
}
