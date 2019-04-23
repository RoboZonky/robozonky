/*
 * Copyright 2019 The RoboZonky Project
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
    void parsing() throws ParserConfigurationException, IOException, SAXException {
        final DocumentBuilderFactory f = XmlUtil.getDocumentBuilderFactory();
        final ByteArrayInputStream bais = new ByteArrayInputStream("<xml />".getBytes(Defaults.CHARSET));
        final Document document = f.newDocumentBuilder().parse(bais);
        assertThat(document.getDocumentElement().getNodeName()).isEqualTo("xml");
    }

    @Test
    void singleton() {
        final DocumentBuilderFactory f = XmlUtil.getDocumentBuilderFactory();
        final DocumentBuilderFactory f2 = XmlUtil.getDocumentBuilderFactory();
        assertThat(f).isSameAs(f2);
    }
}
