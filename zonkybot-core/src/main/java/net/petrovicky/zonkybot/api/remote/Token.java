package net.petrovicky.zonkybot.api.remote;

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
