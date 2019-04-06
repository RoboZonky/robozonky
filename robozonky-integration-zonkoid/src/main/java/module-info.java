module com.github.robozonky.integration.zonkoid {
    requires io.vavr;
    requires org.apache.httpcomponents.httpclient;
    requires org.apache.httpcomponents.httpcore;
    requires org.apache.logging.log4j;
    requires com.github.robozonky.api;

    provides com.github.robozonky.api.confirmations.ConfirmationProviderService with com.github.robozonky.integrations.zonkoid.ZonkoidConfirmationProviderService;

}
