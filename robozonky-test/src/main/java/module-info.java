module com.github.robozonky.test {
    requires org.apache.logging.log4j;
    requires org.junit.jupiter.api;
    requires org.junit.platform.engine;
    requires org.junit.platform.launcher;
    requires org.mockito;
    requires com.github.robozonky.api;
    requires com.github.robozonky.common;

    provides org.junit.platform.launcher.TestExecutionListener with com.github.robozonky.test.RoboZonkyTestExecutionListener;
}
