module com.github.robozonky.app {
    requires java.ws.rs;
    requires java.xml;
    requires com.fasterxml.jackson.databind;
    requires info.picocli;
    requires io.vavr;
    requires jdk.jfr;
    requires maven.artifact;
    requires org.apache.commons.io;
    requires org.apache.commons.lang3;
    requires org.apache.logging.log4j;
    requires com.github.robozonky.api;
    requires com.github.robozonky.common;

    provides com.github.robozonky.common.jobs.JobService with com.github.robozonky.app.events.EventFiringJobService,
            com.github.robozonky.app.version.VersionDetectionJobService,
            com.github.robozonky.app.daemon.SellingJobService,
            com.github.robozonky.app.daemon.ReservationsJobService,
            com.github.robozonky.app.delinquencies.DelinquencyNotificationJobService,
            com.github.robozonky.app.transactions.TransactionProcessingJobService;

    opens com.github.robozonky.app.configuration to info.picocli;
    opens com.github.robozonky.app.events.impl to org.apache.commons.lang3;
}
