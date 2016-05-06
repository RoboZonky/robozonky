/*
 * Copyright 2016 Lukáš Petrovický
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.petrovicky.zonkybot.remote;

import javax.xml.bind.annotation.XmlElement;

public class Token {

    private String accessToken, refreshToken, tokenType;
    private int expiresIn;
    private String scope;

    @XmlElement(name = "access_token")
    public String getAccessToken() {
        return accessToken;
    }

    @XmlElement(name = "refresh_token")
    public String getRefreshToken() {
        return refreshToken;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Token{");
        sb.append("accessToken='").append(accessToken).append('\'');
        sb.append(", refreshToken='").append(refreshToken).append('\'');
        sb.append(", tokenType='").append(tokenType).append('\'');
        sb.append(", expiresIn=").append(expiresIn);
        sb.append(", scope='").append(scope).append('\'');
        sb.append('}');
        return sb.toString();
    }

    @XmlElement(name = "token_type")
    public String getTokenType() {
        return tokenType;
    }

    @XmlElement(name = "expires_in")
    public int getExpiresIn() {
        return expiresIn;
    }

    @XmlElement
    public String getScope() {
        return scope;
    }
}
