# Requires support for multi-stage builds, available in Docker 17.05 or later
# First build RoboZonky and unpack into the install directory...
FROM fedora:latest AS scratch
ENV SOURCE_DIRECTORY=/usr/src/robozonky \
    BINARY_DIRECTORY=/tmp/robozonky
COPY . $SOURCE_DIRECTORY
WORKDIR $SOURCE_DIRECTORY
RUN dnf -y install maven xz && mvn install -B -Dgpg.skip -DskipTests -Ddocker
RUN ROBOZONKY_VERSION=$(mvn -q \
            -Dexec.executable="echo" \
            -Dexec.args='${project.version}' \
            --non-recursive \
            org.codehaus.mojo:exec-maven-plugin:1.6.0:exec \
        ) \
    && ROBOZONKY_TAR_XZ=robozonky-distribution/robozonky-distribution-full/target/robozonky-distribution-full-$ROBOZONKY_VERSION.tar.xz \
    && mkdir -vp $BINARY_DIRECTORY \
    && tar -C $BINARY_DIRECTORY -xvf $ROBOZONKY_TAR_XZ \
    && chmod +x /tmp/robozonky/robozonky.sh

# ... then restart from a minimal image and copy built binary from previous stage
FROM adoptopenjdk/openjdk11:alpine-slim
LABEL maintainer="The RoboZonky Project (www.robozonky.cz)"
ENV INSTALL_DIRECTORY=/opt/robozonky \
     CONFIG_DIRECTORY=/etc/robozonky \
    WORKING_DIRECTORY=/var/robozonky \
    JMX_PORT=7091
# using different ENV as otherwise the above wouldn't be used
ENV JAVA_OPTS="$JAVA_OPTS \
    -Drobozonky.properties.file=$CONFIG_DIRECTORY/robozonky.properties \
    -Dlogback.configurationFile=$CONFIG_DIRECTORY/logback.xml \
    -Dcom.sun.management.jmxremote \
    -Dcom.sun.management.jmxremote.port=$JMX_PORT \
    -Dcom.sun.management.jmxremote.ssl=false \
    -Dcom.sun.management.jmxremote.authenticate=false \
    -Djmx.remote.x.notification.buffer.size=50"
COPY --from=scratch /tmp/robozonky $INSTALL_DIRECTORY
WORKDIR $WORKING_DIRECTORY
EXPOSE $JMX_PORT
ENTRYPOINT exec $INSTALL_DIRECTORY/robozonky.sh @$CONFIG_DIRECTORY/robozonky.cli
