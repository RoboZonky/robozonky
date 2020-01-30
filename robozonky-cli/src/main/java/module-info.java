module com.github.robozonky.cli {
    requires info.picocli;
    requires org.apache.logging.log4j;
    requires com.github.robozonky.api;

    opens com.github.robozonky.cli to info.picocli;

    exports com.github.robozonky.cli;
}
