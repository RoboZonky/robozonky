module com.github.robozonky.integration.stonky {
    requires java.ws.rs;
    requires google.api.client;
    requires google.api.services.drive.v3.rev153;
    requires google.api.services.sheets.v4.rev565;
    requires google.http.client;
    requires google.http.client.jackson2;
    requires google.oauth.client;
    requires google.oauth.client.java6;
    requires google.oauth.client.jetty;
    requires io.vavr;
    requires org.apache.commons.io;
    requires org.apache.logging.log4j;
    requires com.github.robozonky.api;
    requires com.github.robozonky.common;

    opens com.github.robozonky.integrations.stonky to org.apache.commons.lang3;

    exports com.github.robozonky.integrations.stonky;

    provides com.github.robozonky.common.jobs.JobService with com.github.robozonky.integrations.stonky.StonkyJobService;
}
