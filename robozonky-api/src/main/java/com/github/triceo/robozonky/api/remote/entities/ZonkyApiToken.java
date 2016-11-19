/*
 * Copyright 2016 Lukáš Petrovický
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.triceo.robozonky.api.remote.entities;

import java.io.Reader;
import java.io.StringWriter;
import java.util.Objects;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * OAuth access token for Zonky API.
 *
 * Knowledge of this token will allow anyone to access the service as if they were the authenticated user. This is
 * therefore highly sensitive information and should never be kept in memory for longer than necessary.
 */
@XmlRootElement(name = "token")
@XmlAccessorType(XmlAccessType.FIELD)
public class ZonkyApiToken implements BaseEntity {

    public static ZonkyApiToken unmarshal(final Reader token) throws JAXBException {
        final JAXBContext ctx = JAXBContext.newInstance(ZonkyApiToken.class);
        final Unmarshaller u = ctx.createUnmarshaller();
        return (ZonkyApiToken)u.unmarshal(token);
    }

    public static String marshal(final ZonkyApiToken token) throws JAXBException {
        final JAXBContext ctx = JAXBContext.newInstance(ZonkyApiToken.class);
        final Marshaller m = ctx.createMarshaller();
        final StringWriter w = new StringWriter();
        m.marshal(token, w);
        return w.toString();
    }

    @XmlElement(name="access_token")
    private char[] accessToken;
    @XmlElement(name="refresh_token")
    private char[] refreshToken;
    @XmlElement(name = "token_type")
    private String type;
    @XmlElement
    private String scope;
    @XmlElement(name = "expires_in")
    private int expiresIn;

    ZonkyApiToken() {
        // for JAXB
    }

    public ZonkyApiToken(final String accessToken, final String refreshToken, final int expiresIn) {
        this(accessToken, refreshToken, expiresIn, "refresh_token", "SCOPE_APP_WEB");
    }

    public ZonkyApiToken(final String accessToken, final String refreshToken, final int expiresIn, final String type,
                         final String scope) {
        this.accessToken = accessToken.toCharArray();
        this.refreshToken = refreshToken.toCharArray();
        this.expiresIn = expiresIn;
        this.type = type;
        this.scope = scope;
    }

    public char[] getAccessToken() {
        return accessToken;
    }

    public char[] getRefreshToken() {
        return refreshToken;
    }

    public String getType() {
        return type;
    }

    /**
     * Interval in seconds in which the token will expire.
     * @return Time left before token expiration, in seconds, at the time token was retrieved.
     */
    public int getExpiresIn() {
        return expiresIn;
    }

    public String getScope() {
        return scope;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final ZonkyApiToken that = (ZonkyApiToken) o;
        return Objects.equals(accessToken, that.accessToken) &&
                Objects.equals(refreshToken, that.refreshToken) &&
                Objects.equals(scope, that.scope);
    }

    @Override
    public int hashCode() {
        return Objects.hash(accessToken, refreshToken, scope);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ZonkyApiToken{");
        sb.append("type='").append(type).append('\'');
        sb.append(", expiresIn=").append(expiresIn);
        sb.append(", scope='").append(scope).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
