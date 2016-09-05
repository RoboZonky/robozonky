package com.github.triceo.robozonky.app;

import com.github.triceo.robozonky.Investor;
import com.github.triceo.robozonky.app.authentication.AuthenticationHandler;
import com.github.triceo.robozonky.authentication.Authentication;
import com.github.triceo.robozonky.push.SecureClientSocket;
import com.github.triceo.robozonky.remote.Investment;
import com.github.triceo.robozonky.remote.ZonkyApi;
import com.github.triceo.robozonky.remote.ZotifyApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.BadRequestException;
import java.math.BigDecimal;
import java.util.Collection;

/**
 * Created by hampl on 8/10/16.
 */
public class ZonkyCore {
    private static final Logger LOG = LoggerFactory.getLogger(SecureClientSocket.class);

    private final AppContext ctx;
    private final AuthenticationHandler auth;
   private final String pushKey;

    public ZonkyCore(AppContext ctx, AuthenticationHandler auth, String pushKey) {
        this.ctx = ctx;
        this.auth = auth;
        this.pushKey = pushKey;
    }

    public boolean work() {
        initPush(pushKey);
        while (true) {
            try {
                core();
                Thread.sleep(900000);
            } catch (InterruptedException e) {
                LOG.error("thread sleep Error", e);
            }
        }
    }

    private  boolean core() {
        LOG.info("===== RoboZonky at your service! =====");
        final boolean isDryRun = ctx.isDryRun();
        if (isDryRun) {
            LOG.info("RoboZonky is doing a dry run. It will simulate investing, but not invest any real money.");
        }
        final Authentication login;
        try { // catch this exception here, so that anything coming from the invest() method can be thrown separately
            login = auth.login();
        } catch (final BadRequestException ex) {
            LOG.error("Failed authenticating with Zonky.", ex);
            return false;
        }
        final Collection<Investment> result = invest( login.getZonkyApi(), login.getZotifyApi());
        try { // log out and ignore any resulting error
            auth.logout(login);
        } catch (final RuntimeException ex) {
            LOG.warn("Failed logging out of Zonky.", ex);
        }
        App.storeInvestmentsMade(result, isDryRun);
        LOG.info("RoboZonky {}invested into {} loans.", isDryRun ? "would have " : "", result.size());
        return true;
    }

    private Collection<Investment> invest(final ZonkyApi zonky, final ZotifyApi zotify) {
        final BigDecimal balance = App.getAvailableBalance(ctx, zonky);
        final Investor i = new Investor(zonky, zotify, ctx.getInvestmentStrategy(), balance);
        return App.getInvestingFunction(ctx).apply(i);
    }

    private void initPush(String pushKey) {
        new SecureClientSocket(pushKey,() -> core()).start();
    }
}
