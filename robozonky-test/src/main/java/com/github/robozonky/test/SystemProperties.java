package com.github.robozonky.test;

import java.util.Properties;

enum SystemProperties {

    INSTANCE;

    private Properties originalProperties;

    public void save() {
        originalProperties = System.getProperties();
        System.setProperties(copyOf(originalProperties));
    }

    private Properties copyOf(final Properties source) {
        final Properties copy = new Properties();
        copy.putAll(source);
        return copy;
    }

    public void restore() {
        System.setProperties(originalProperties);
    }
}
