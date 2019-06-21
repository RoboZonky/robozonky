module com.github.robozonky.cli {
    requires google.api.services.drive.v3.rev153;
    requires google.api.services.sheets.v4.rev565;
    requires google.http.client;
    requires google.oauth.client;
    requires info.picocli;
    requires io.vavr;
    requires org.apache.logging.log4j;
    requires com.github.robozonky.api;
    requires com.github.robozonky.integration.stonky;

    opens com.github.robozonky.cli to info.picocli;

    exports com.github.robozonky.cli;
}
