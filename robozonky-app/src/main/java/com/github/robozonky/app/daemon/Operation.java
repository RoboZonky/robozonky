package com.github.robozonky.app.daemon;

import java.util.Collection;

import com.github.robozonky.api.remote.entities.sanitized.Investment;
import com.github.robozonky.app.tenant.PowerTenant;

@FunctionalInterface
interface Operation<B, C> {

    Collection<Investment> apply(PowerTenant a, Collection<B> b, C c);

}
