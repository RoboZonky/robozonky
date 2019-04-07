module com.github.robozonky.api {
    requires java.ws.rs;
    requires java.xml.bind;
    requires com.fasterxml.jackson.annotation;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;
    requires io.vavr;
    requires org.apache.commons.lang3;
    requires org.apache.logging.log4j;

    exports com.github.robozonky.api;
    exports com.github.robozonky.api.confirmations;
    exports com.github.robozonky.api.notifications;
    exports com.github.robozonky.api.remote;
    exports com.github.robozonky.api.remote.entities;
    exports com.github.robozonky.api.remote.entities.sanitized;
    exports com.github.robozonky.api.remote.enums;
    exports com.github.robozonky.api.strategies;
    exports com.github.robozonky.internal.api;
    exports com.github.robozonky.internal.test;
    exports com.github.robozonky.internal.util;

    opens com.github.robozonky.api.remote.enums to com.fasterxml.jackson.databind, org.apache.commons.lang3;
    opens com.github.robozonky.api.remote.entities to com.fasterxml.jackson.databind, org.apache.commons.lang3;
    opens com.github.robozonky.api.remote.entities.sanitized to org.apache.commons.lang3;

}
