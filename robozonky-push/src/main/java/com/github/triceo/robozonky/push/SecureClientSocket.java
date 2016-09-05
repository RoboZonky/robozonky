package com.github.triceo.robozonky.push; /**
 * Created by hampl on 7/26/16.
 */


import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.*;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.concurrent.Future;

@WebSocket
public class SecureClientSocket {
    private static final Logger LOG = LoggerFactory.getLogger(SecureClientSocket.class);

    private final VoidCallable callable;
    private final String PUSH_BULLET_URL= "wss://stream.pushbullet.com/websocket/" ;
    private final String pushKey;

    public SecureClientSocket(String pushKey, VoidCallable callable) {
        this.callable = callable;
        this.pushKey = pushKey;
    }

    public void start() {
        SslContextFactory sslContextFactory = new SslContextFactory();
        sslContextFactory.setTrustAll(true);

        WebSocketClient client = new WebSocketClient(sslContextFactory);
        try {
            client.start();
            Future<Session> fut = client.connect(this, URI.create(PUSH_BULLET_URL  + pushKey));
            Session session = fut.get();
        } catch (Throwable t) {
            LOG.error("push initialization error", t);
        }
    }

    @OnWebSocketConnect
    public void onConnect(Session sess) {
        LOG.info("onConnect({})", sess);
    }

    @OnWebSocketClose
    public void onClose(int statusCode, String reason) {
        LOG.info("onClose({}, {})", statusCode, reason);
    }

    @OnWebSocketError
    public void onError(Throwable cause) {
        LOG.error("push error", cause);
    }

    @OnWebSocketMessage
    public void onMessage(String msg) {
        if (!"{\"type\": \"nop\"}".equals(msg)) {
            LOG.info("onMessage() - {}", msg);
            try {
                callable.call();
            } catch (Exception e) {
                LOG.error("error while calling callback", e);
            }
        }
    }
}