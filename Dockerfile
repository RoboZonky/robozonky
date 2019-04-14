# Requires support for multi-stage builds, available in Docker 17.05 or later
# First build RoboZonky and unpack into the install directory...
FROM maven:3-jdk-11 AS scratch
ENV SOURCE_DIRECTORY=/usr/src/robozonky \
    BINARY_DIRECTORY=/tmp/robozonky
COPY . $SOURCE_DIRECTORY
WORKDIR $SOURCE_DIRECTORY
RUN mvn install -B -Dgpg.skip -DskipTests -Ddocker
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
FROM adoptopenjdk/openjdk11:alpine-jre
LABEL maintainer="The RoboZonky Project (www.robozonky.cz)"
ENV INSTALL_DIRECTORY=/opt/robozonky \
     CONFIG_DIRECTORY=/etc/robozonky \
    WORKING_DIRECTORY=/var/robozonky
# using different ENV as otherwise the ENV definitions above wouldn't be used
ENV JAVA_OPTS="$JAVA_OPTS \
    -Drobozonky.properties.file=$CONFIG_DIRECTORY/robozonky.properties \
    -Dlog4j.configurationFile=$CONFIG_DIRECTORY/log4j2.xml"
COPY --from=scratch /tmp/robozonky $INSTALL_DIRECTORY
WORKDIR $WORKING_DIRECTORY
ENTRYPOINT $INSTALL_DIRECTORY/robozonky.sh @$CONFIG_DIRECTORY/robozonky.cli
