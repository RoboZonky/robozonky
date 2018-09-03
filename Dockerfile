# Requires support for multi-stage builds, available in Docker 17.05 or later
# First build RoboZonky and unpack into the install directory...
FROM fedora:latest AS builder
ENV SOURCE_DIRECTORY=/usr/src/robozonky \
    BINARY_DIRECTORY=/tmp/robozonky
COPY . $SOURCE_DIRECTORY
WORKDIR $SOURCE_DIRECTORY
RUN dnf -y install maven xz \
    && mvn clean install -T1C -B -Dgpg.skip -DskipTests -Ddocker
RUN ROBOZONKY_VERSION=$(mvn -q \
            -Dexec.executable="echo" \
            -Dexec.args='${project.version}' \
            --non-recursive \
            org.codehaus.mojo:exec-maven-plugin:1.6.0:exec \
        ) \
    && ROBOZONKY_TAR_XZ=robozonky-distribution/robozonky-distribution-full/target/robozonky-distribution-full-$ROBOZONKY_VERSION.tar.xz \
    && mkdir -vp $BINARY_DIRECTORY \
    && tar -C $BINARY_DIRECTORY -xvf $ROBOZONKY_TAR_XZ

# ... then restart from a minimal image, copy built binary from previous stage and install latest JRE.
FROM fedora:latest
LABEL maintainer="The RoboZonky Project (www.robozonky.cz)"
ENV INSTALL_DIRECTORY=/opt/robozonky \
     CONFIG_DIRECTORY=/etc/robozonky \
    WORKING_DIRECTORY=/var/robozonky
COPY --from=builder /tmp/robozonky $INSTALL_DIRECTORY
WORKDIR $WORKING_DIRECTORY
RUN dnf -y install java-openjdk-headless \
    && dnf clean all \
    && alternatives --set java  $(alternatives --display java |grep java-openjdk|grep -Eo '^[^ ]+') \
    && java -version
EXPOSE 7091
ENTRYPOINT JAVA_OPTS="$JAVA_OPTS \
    -Drobozonky.properties.file=$CONFIG_DIRECTORY/robozonky.properties \
    -Dlogback.configurationFile=$CONFIG_DIRECTORY/logback.xml \
    -Dcom.sun.management.jmxremote \
    -Dcom.sun.management.jmxremote.port=7091 \
    -Dcom.sun.management.jmxremote.ssl=false \
    -Dcom.sun.management.jmxremote.authenticate=false \
    -Djmx.remote.x.notification.buffer.size=50" \
    $INSTALL_DIRECTORY/robozonky.sh \
    @$CONFIG_DIRECTORY/robozonky.cli
