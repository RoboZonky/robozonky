package com.github.robozonky.api.remote.entities;

import javax.xml.bind.annotation.adapters.XmlAdapter;

import com.github.robozonky.api.remote.enums.OAuthScopes;

class OAuthScopeAdapter extends XmlAdapter<String, OAuthScopes> {

    @Override
    public OAuthScopes unmarshal(final String v) {
        return OAuthScopes.valueOf(v);
    }

    @Override
    public String marshal(final OAuthScopes v) {
        return v.toString();
    }
}
