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

package com.github.robozonky.app.version;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.StringJoiner;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import com.github.robozonky.common.async.Refreshable;
import com.github.robozonky.common.jobs.SimplePayload;
import com.github.robozonky.internal.api.Defaults;
import com.github.robozonky.internal.util.UrlUtil;
import io.vavr.control.Try;
import org.apache.commons.io.IOUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Retrieve latest released version from Maven Central. By default will check
 * https://repo1.maven.org/maven2/com/github/robozonky/robozonky/maven-metadata.xml.
 */
final class UpdateMonitor extends Refreshable<VersionIdentifier> implements SimplePayload {

    private static final String URL_SEPARATOR = "/";
    private static final Pattern PATTERN_DOT = Pattern.compile("\\Q.\\E");
    private static final Pattern PATTERN_STABLE_VERSION = Pattern.compile("\\A[1-9][0-9]*\\.[0-9]+\\.[0-9]+\\z");

    private final String groupId, artifactId, mavenCentralHostname;

    UpdateMonitor(final String server) {
        this(server, "com.github.robozonky", "robozonky"); // RoboZonky's parent POM
    }

    UpdateMonitor(final String server, final String groupId, final String artifactId) {
        this.mavenCentralHostname = server;
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.registerListener(new UpdateNotification());
    }

    public UpdateMonitor() {
        this("https://repo1.maven.org");
    }

    /**
     * Assemble the Maven Central metadata URL from the given groupId and artifactId.
     * @param groupId Group ID in question.
     * @param artifactId Artifact ID in question.
     * @param hostname Maven Central hostname, such as "https://repo1.maven.org"
     * @return Stream to read the Maven Central metadata from.
     * @throws IOException Network communications failure.
     */
    private static InputStream getMavenCentralData(final String groupId, final String artifactId,
                                                   final String hostname) throws IOException {
        final StringJoiner joiner = new StringJoiner(UpdateMonitor.URL_SEPARATOR)
                .add(hostname)
                .add("maven2");
        for (final String pkg : UpdateMonitor.PATTERN_DOT.split(groupId)) {
            joiner.add(pkg);
        }
        joiner.add(artifactId)
                .add("maven-metadata.xml");
        return UrlUtil.open(new URL(joiner.toString()));
    }

    private static boolean isStable(final String version) {
        return UpdateMonitor.PATTERN_STABLE_VERSION.matcher(version).find();
    }

    static String findFirstStable(final Set<String> versions) {
        return versions.stream()
                .filter(UpdateMonitor::isStable)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No stable version found."));
    }

    static VersionIdentifier parseNodeList(final NodeList nodeList) {
        final SortedSet<String> versions = IntStream.range(0, nodeList.getLength())
                .mapToObj(nodeList::item)
                .map(Node::getTextContent)
                .collect(Collectors.toCollection(() -> new TreeSet<>(new VersionComparator().reversed())));
        // find latest stable
        final String firstStable = UpdateMonitor.findFirstStable(versions);
        // and check if it is followed by any other unstable versions
        final String first = versions.first();
        if (Objects.equals(first, firstStable)) {
            return new VersionIdentifier(firstStable);
        } else {
            return new VersionIdentifier(firstStable, first);
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
    private static VersionIdentifier parseVersionString(final InputStream xml) throws IOException {
        try {
            final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            final DocumentBuilder builder = factory.newDocumentBuilder();
            final Document doc = builder.parse(xml);
            final XPathFactory xPathfactory = XPathFactory.newInstance();
            final XPath xpath = xPathfactory.newXPath();
            final XPathExpression expr = xpath.compile("/metadata/versioning/versions/version");
            return UpdateMonitor.parseNodeList((NodeList) expr.evaluate(doc, XPathConstants.NODESET));
        } catch (final SAXException | XPathExpressionException | ParserConfigurationException ex) {
            throw new IOException(ex);
        }
    }

    @Override
    protected String getLatestSource() {
        return Try.withResources(() -> UpdateMonitor.getMavenCentralData(this.groupId, this.artifactId,
                                                                         this.mavenCentralHostname))
                .of(s -> IOUtils.toString(s, Defaults.CHARSET))
                .getOrElseThrow((Function<Throwable, IllegalStateException>) IllegalStateException::new);
    }

    @Override
    protected Optional<VersionIdentifier> transform(final String source) {
        return Try.withResources(() -> new ByteArrayInputStream(source.getBytes(Defaults.CHARSET)))
                .of(s -> Optional.of(UpdateMonitor.parseVersionString(s)))
                .getOrElseGet(ex -> {
                    logger.debug("Failed parsing source.", ex);
                    return Optional.empty();
                });
    }
}
