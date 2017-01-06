/*
 * Copyright 2017 Lukáš Petrovický
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
import java.util.Set;
import java.util.SortedSet;
import java.util.StringJoiner;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
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
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Retrieve latest released version from Maven Central. By default will check
 * https://repo1.maven.org/maven2/com/github/triceo/robozonky/robozonky-app/maven-metadata.xml.
 */
class VersionRetriever implements Callable<VersionIdentifier> {

    private static final Logger LOGGER = LoggerFactory.getLogger(VersionRetriever.class);

    private static final String GROUP_ID = VersionRetriever.class.getPackage().getImplementationVendor();
    private static final String ARTIFACT_ID = "robozonky-app";
    private static final Pattern PATTERN_DOT = Pattern.compile("\\Q.\\E");
    private static final Pattern PATTERN_STABLE_VERSION = Pattern.compile("\\A[1-9][0-9]*\\.[0-9]+\\.[0-9]+\\z");

    private final String groupId, artifactId;

    // tests only; VersionCheck.class.getPackage() does not contain anything then
    VersionRetriever(final String groupId, final String artifactId) {
        this.groupId = groupId == null ? "com.github.triceo.robozonky" : groupId;
        this.artifactId = artifactId == null ? VersionRetriever.ARTIFACT_ID : artifactId;
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
        joiner.add(Arrays.stream(VersionRetriever.PATTERN_DOT.split(groupId)).collect(Collectors.joining(urlSeparator)));
        joiner.add(artifactId);
        joiner.add("maven-metadata.xml");
        return new URL(joiner.toString());
    }

    private static boolean isStable(final String version) {
        final Matcher matcher = VersionRetriever.PATTERN_STABLE_VERSION.matcher(version);
        return matcher.find();
    }

    static String findLastStable(final Set<String> versions) {
        return versions.stream()
                .sorted(new VersionComparator().reversed())
                .filter(VersionRetriever::isStable)
                .findFirst().orElseThrow(() -> new IllegalStateException("Impossible."));
    }

    private static VersionIdentifier parseNodeList(final NodeList nodeList) {
        final SortedSet<String> versions = new TreeSet<>(new VersionComparator());
        for (int i = 0; i < nodeList.getLength(); i++) {
            final String version = nodeList.item(i).getTextContent();
            versions.add(version);
        }
        // find latest stable
        final String stable = VersionRetriever.findLastStable(versions);
        // and check if it is followed by any other versions
        final SortedSet<String> tail = versions.tailSet(stable);
        if (tail.isEmpty()) {
            return new VersionIdentifier(stable);
        } else {
            return new VersionIdentifier(stable, tail.last());
        }
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
    private static VersionIdentifier parseVersionString(final InputStream xml) throws ParserConfigurationException,
            IOException, SAXException, XPathExpressionException {
        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        final DocumentBuilder builder = factory.newDocumentBuilder();
        final Document doc = builder.parse(xml);
        final XPathFactory xPathfactory = XPathFactory.newInstance();
        final XPath xpath = xPathfactory.newXPath();
        final XPathExpression expr = xpath.compile("/metadata/versioning/versions/version");
        return VersionRetriever.parseNodeList((NodeList)expr.evaluate(doc, XPathConstants.NODESET));
    }

    @Override
    public VersionIdentifier call() throws Exception {
        final URL url = VersionRetriever.getMavenCentralUrl(this.groupId, this.artifactId);
        VersionRetriever.LOGGER.trace("Version check starting from {}.", url);
        try (final InputStream urlStream = url.openStream()) {
            return VersionRetriever.parseVersionString(urlStream);
        } finally {
            VersionRetriever.LOGGER.trace("Update check finished.");
        }
    }
}
