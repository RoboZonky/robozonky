module com.github.robozonky.notifications {
    requires java.mail;
    requires commons.email;
    requires freemarker;
    requires io.vavr;
    requires org.apache.commons.lang3;
    requires org.apache.logging.log4j;
    requires com.github.robozonky.api;

    // hidden freemarker dependencies
    requires java.sql;

    // to load resources
    opens com.github.robozonky.notifications.templates.html to freemarker;
    opens com.github.robozonky.notifications.templates.plaintext to freemarker;

    provides com.github.robozonky.api.notifications.ListenerService with com.github.robozonky.notifications.NotificationListenerService;
}
