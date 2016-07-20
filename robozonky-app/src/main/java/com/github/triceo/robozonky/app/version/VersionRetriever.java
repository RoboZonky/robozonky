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

package com.github.triceo.robozonky.app.version;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.StringJoiner;
import java.util.concurrent.Callable;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * Retrieve latest released version from Maven Central. By default will check
 * https://repo1.maven.org/maven2/com/github/triceo/robozonky/robozonky-app/maven-metadata.xml.
 */
class VersionRetriever implements Callable<String> {

    private static final Logger LOGGER = LoggerFactory.getLogger(VersionRetriever.class);

    private static final String GROUP_ID = VersionRetriever.class.getPackage().getImplementationVendor();
    private static final String ARTIFACT_ID = VersionRetriever.class.getPackage().getSpecificationTitle();
    private static final Pattern COMPILE = Pattern.compile("\\Q.\\E");

    private final String groupId, artifactId;

    // tests only; VersionCheck.class.getPackage() does not contain anything then
    VersionRetriever(final String groupId, final String artifactId) {
        this.groupId = groupId == null ? "com.github.triceo.robozonky" : groupId;
        this.artifactId = artifactId == null ? "robozonky-app" : artifactId;
    }

    public VersionRetriever() {
        this(VersionRetriever.GROUP_ID, VersionRetriever.ARTIFACT_ID);
    }

    /**
     * Assemble the Maven Central metadata URL from the given groupId and artifactId.
     * @param groupId Group ID in question.
     * @param artifactId Artifact ID in question.
     * @return URL to the Maven Central metadata file.
     * @throws MalformedURLException Malformed URL. No real reason why that would happen.
     */
    private static URL getMavenCentralUrl(final String groupId, final String artifactId) throws MalformedURLException {
        final String urlSeparator = "/";
        final StringJoiner joiner = new StringJoiner(urlSeparator);
        joiner.add("https://repo1.maven.org/maven2");
        joiner.add(Arrays.stream(VersionRetriever.COMPILE.split(groupId)).collect(Collectors.joining(urlSeparator)));
        joiner.add(artifactId);
        joiner.add("maven-metadata.xml");
        return new URL(joiner.toString());
    }

    /**
     * Parse XML using XPath and retrieve the version string.
     * @param xml XML in question.
     * @return The version string.
     * @throws ParserConfigurationException Failed parsing XML.
     * @throws IOException Failed I/O.
     * @throws SAXException Failed reading XML.
     * @throws XPathExpressionException XPath parsing problem.
     */
    private static String parseVersionString(final InputStream xml) throws ParserConfigurationException, IOException,
            SAXException, XPathExpressionException {
        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        final DocumentBuilder builder = factory.newDocumentBuilder();
        final Document doc = builder.parse(xml);
        final XPathFactory xPathfactory = XPathFactory.newInstance();
        final XPath xpath = xPathfactory.newXPath();
        final XPathExpression expr = xpath.compile("/metadata/versioning/latest");
        return (String)expr.evaluate(doc, XPathConstants.STRING);
    }

    @Override
    public String call() throws Exception {
        final URL url = VersionRetriever.getMavenCentralUrl(this.groupId, this.artifactId);
        VersionRetriever.LOGGER.trace("RoboZonky version check starting from {}.", url);
        try (final InputStream urlStream = url.openStream()) {
            return VersionRetriever.parseVersionString(urlStream);
        } finally {
            VersionRetriever.LOGGER.trace("RoboZonky update check finished.");
        }
    }
}
