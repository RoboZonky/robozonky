FROM openjdk:10-jdk-slim

ENV SOURCE_DIRECTORY=/usr/src/robozonky \
    INSTALL_DIRECTORY=/opt/RoboZonky \
    WORKING_DIRECTORY=/var/RoboZonky \
    CONFIG_DIRECTORY=/etc/RoboZonky

COPY . $SOURCE_DIRECTORY

# Build RoboZonky, unpack into the install directory, clean up to reduce image size
RUN cd $SOURCE_DIRECTORY \
    && apt-get update \
    && apt-get --no-install-recommends -y install maven xz-utils \
    && mvn clean install -Dgpg.skip -DskipTests -Ddocker \
    && ROBOZONKY_VERSION=$(mvn -q \
            -Dexec.executable="echo" \
            -Dexec.args='${project.version}' \
            --non-recursive \
            org.codehaus.mojo:exec-maven-plugin:1.6.0:exec \
        ) \
    && ROBOZONKY_TAR_XZ=robozonky-distribution/robozonky-distribution-full/target/robozonky-distribution-full-$ROBOZONKY_VERSION.tar.xz \
    && mkdir -vp $INSTALL_DIRECTORY \
    && tar -C $INSTALL_DIRECTORY -xvf $ROBOZONKY_TAR_XZ \
    && LOCAL_MAVEN_REPO=$(mvn -q \
            -Dexec.executable="echo" \
            -Dexec.args='${settings.localRepository}' \
            --non-recursive \
            org.codehaus.mojo:exec-maven-plugin:1.6.0:exec \
        ) \
    && apt-get -y remove --purge maven xz-utils \
    && apt-get -y autoremove \
    && apt-get -y clean \
    && rm -rf $LOCAL_MAVEN_REPO \
    && rm -rf $SOURCE_DIRECTORY \
    && rm -rf /var/lib/apt/lists/* \
    && mkdir -vp $WORKING_DIRECTORY

WORKDIR $WORKING_DIRECTORY

ENTRYPOINT JAVA_OPTS="$JAVA_OPTS -Xmx32m -Xss256k \
    -Drobozonky.properties.file=$CONFIG_DIRECTORY/robozonky.properties \
    -Dlogback.configurationFile=$CONFIG_DIRECTORY/logback.xml" \
    $INSTALL_DIRECTORY/robozonky.sh \
    @$CONFIG_DIRECTORY/robozonky.cli
