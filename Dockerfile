# First build RoboZonky and unpack into the install directory...
FROM maven:3-jdk-11 AS scratch
ENV SOURCE_DIRECTORY=/usr/src/robozonky \
    BINARY_DIRECTORY=/tmp/robozonky
COPY . $SOURCE_DIRECTORY
WORKDIR $SOURCE_DIRECTORY
RUN mvn install -B -DskipTests -Ddocker
RUN ROBOZONKY_VERSION=$(mvn -q \
            -Dexec.executable="echo" \
            -Dexec.args='${project.version}' \
            --non-recursive \
            org.codehaus.mojo:exec-maven-plugin:1.6.0:exec \
        ) \
    && ROBOZONKY_NOARCH=robozonky-distribution/robozonky-distribution-full/target/robozonky-distribution-full-$ROBOZONKY_VERSION-noarch.zip \
    && mkdir -vp $BINARY_DIRECTORY \
    && unzip $ROBOZONKY_NOARCH -d $BINARY_DIRECTORY \
    && chmod +x $BINARY_DIRECTORY/robozonky.sh

# ... then build a minimalistic Java runtime using jlink ...
FROM adoptopenjdk/openjdk16:alpine AS jlink
ENV WORKING_DIRECTORY=/tmp/robozonky
COPY --from=scratch /tmp/robozonky $WORKING_DIRECTORY
COPY . .
RUN apk add --no-cache tzdata musl-locales musl-locales-lang binutils
RUN rm $(find $WORKING_DIRECTORY -name "robozonky-cli*jar")
RUN ROBOZONKY_EXECUTABLE=$(find $WORKING_DIRECTORY -name "robozonky-app*jar") \
    && .github/workflows/jlink.sh $ROBOZONKY_EXECUTABLE $WORKING_DIRECTORY/runtime

# ... and finally restart from a minimal image and copy built binary from previous stage
FROM alpine:latest
LABEL maintainer="The RoboZonky Project (www.robozonky.cz)"
ENV INSTALL_DIRECTORY=/opt/robozonky \
     CONFIG_DIRECTORY=/etc/robozonky \
    WORKING_DIRECTORY=/var/robozonky \
                 LANG='cs_CZ.UTF-8' \
             LANGUAGE='cs_CZ:cs' \
               LC_ALL='cs_CZ.UTF-8'
COPY --from=jlink /tmp/robozonky $INSTALL_DIRECTORY
# Using different ENV as otherwise the ENV definitions above wouldn't be used.
ENV JAVA_OPTS="$JAVA_OPTS \
    -Drobozonky.properties.file=$CONFIG_DIRECTORY/robozonky.properties \
    -Dlog4j.configurationFile=$CONFIG_DIRECTORY/log4j2.xml"
# And this is just for debugging purposes.
RUN mkdir $WORKING_DIRECTORY \
    && ls -l -R $INSTALL_DIRECTORY
WORKDIR $WORKING_DIRECTORY
ENTRYPOINT $INSTALL_DIRECTORY/robozonky.sh @$CONFIG_DIRECTORY/robozonky.cli
