/*
 *
 *  * Copyright 2016 Lukáš Petrovický
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 * /
 */
package com.github.triceo.robozonky.remote;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Objects;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "token")
@XmlAccessorType(XmlAccessType.FIELD)
public class ZonkyApiToken {

    public static ZonkyApiToken unmarshal(final InputStream token) throws JAXBException {
        final JAXBContext ctx = JAXBContext.newInstance(ZonkyApiToken.class);
        final Unmarshaller u = ctx.createUnmarshaller();
        return (ZonkyApiToken)u.unmarshal(token);
    }

    public static String marshal(final ZonkyApiToken token) throws JAXBException {
        final JAXBContext ctx = JAXBContext.newInstance(ZonkyApiToken.class);
        final Marshaller m = ctx.createMarshaller();
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        m.marshal(token, baos);
        return baos.toString();
    }

    @XmlElement(name="access_token")
    private String accessToken;
    @XmlElement(name="refresh_token")
    private String refreshToken;
    @XmlElement(name = "token_type")
    private String type;
    @XmlElement
    private String scope;
    @XmlElement(name = "expires_in")
    private int expiresIn;

    ZonkyApiToken() {
        // for JAXB
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public String getType() {
        return type;
    }

    public int getExpiresIn() {
        return expiresIn;
    }

    public String getScope() {
        return scope;
    }

    @Override
    public boolean equals(Object o) {
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
        sb.append("accessToken='").append(accessToken).append('\'');
        sb.append(", refreshToken='").append(refreshToken).append('\'');
        sb.append(", type='").append(type).append('\'');
        sb.append(", expiresIn=").append(expiresIn);
        sb.append(", scope='").append(scope).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
