package com.github.robozonky.internal.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import com.github.robozonky.internal.api.Defaults;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import static org.assertj.core.api.Assertions.*;

class XmlUtilTest {

    @Test
    void factory() throws ParserConfigurationException, IOException, SAXException {
        final DocumentBuilderFactory f = XmlUtil.getDocumentBuilderFactory();
        final ByteArrayInputStream bais = new ByteArrayInputStream("<xml />".getBytes(Defaults.CHARSET));
        final Document document = f.newDocumentBuilder().parse(bais);
        assertThat(document.getDocumentElement().getNodeName()).isEqualTo("xml");
    }

}
