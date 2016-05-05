package net.petrovicky.zonkybot.api.remote;

import javax.xml.bind.annotation.XmlElement;

public class Photo {

    private String name;
    private String url;

    @XmlElement
    public String getName() {
        return name;
    }

    @XmlElement
    public String getUrl() {
        return url;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Photo{");
        sb.append("name='").append(name).append('\'');
        sb.append(", url='").append(url).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
