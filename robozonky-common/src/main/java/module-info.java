module com.github.robozonky.common {
    requires java.management;
    requires java.ws.rs;
    requires ini4j;
    requires io.vavr;
    requires jdk.jfr;
    requires org.apache.logging.log4j;
    requires paging.streams;
    requires resteasy.client;
    requires com.github.robozonky.api;

    exports com.github.robozonky.common.async;
    exports com.github.robozonky.common.extensions;
    exports com.github.robozonky.common.jobs;
    exports com.github.robozonky.common.management;
    exports com.github.robozonky.common.remote;
    exports com.github.robozonky.common.secrets;
    exports com.github.robozonky.common.state;
    exports com.github.robozonky.common.tenant;

    provides com.github.robozonky.common.jobs.JobService with com.github.robozonky.common.state.StateCleanerJobService;
}
