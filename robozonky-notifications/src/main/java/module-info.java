module com.github.robozonky.notifications {
    requires jakarta.mail;
    requires commons.email;
    requires freemarker;
    requires io.vavr;
    requires jdk.jfr;
    requires org.apache.commons.lang3;
    requires org.apache.logging.log4j;
    requires com.github.robozonky.api;
    requires com.github.robozonky.common;

    // hidden freemarker dependencies
    requires java.sql;

    provides com.github.robozonky.api.notifications.ListenerService with com.github.robozonky.notifications.NotificationListenerService;
}
