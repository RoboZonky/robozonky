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
    && chmod +x $BINARY_DIRECTORY/robozonky.sh

# ... then build a minimalistic JRE using jlink ...
FROM adoptopenjdk/openjdk14:alpine AS jlink
ENV WORKING_DIRECTORY=/tmp/robozonky
COPY --from=scratch /tmp/robozonky $WORKING_DIRECTORY
COPY . .
RUN apk add binutils
RUN ROBOZONKY_EXECUTABLE=$(find $WORKING_DIRECTORY -name "robozonky-distribution-full*jar") \
    && .github/workflows/jlink.sh $ROBOZONKY_EXECUTABLE $WORKING_DIRECTORY/jre

# ... and finally restart from a minimal image and copy built binary from previous stage
FROM alpine:latest
LABEL maintainer="The RoboZonky Project (www.robozonky.cz)"
ENV INSTALL_DIRECTORY=/opt/robozonky \
     CONFIG_DIRECTORY=/etc/robozonky \
    WORKING_DIRECTORY=/var/robozonky \
                 LANG='en_US.UTF-8' \
             LANGUAGE='en_US:en' \
               LC_ALL='en_US.UTF-8'
COPY --from=jlink /tmp/robozonky $INSTALL_DIRECTORY
# Using different ENV as otherwise the ENV definitions above wouldn't be used.
ENV JAVA_OPTS="$JAVA_OPTS \
    -Drobozonky.properties.file=$CONFIG_DIRECTORY/robozonky.properties \
    -Dlog4j.configurationFile=$CONFIG_DIRECTORY/log4j2.xml"
# Copied from https://github.com/AdoptOpenJDK/openjdk-docker/blob/master/13/jre/alpine/Dockerfile.hotspot.releases.full.
RUN apk add --no-cache --virtual .build-deps curl binutils \
    && GLIBC_VER="2.30-r0" \
    && ALPINE_GLIBC_REPO="https://github.com/sgerrand/alpine-pkg-glibc/releases/download" \
    && GCC_LIBS_URL="https://archive.archlinux.org/packages/g/gcc-libs/gcc-libs-9.1.0-2-x86_64.pkg.tar.xz" \
    && GCC_LIBS_SHA256="91dba90f3c20d32fcf7f1dbe91523653018aa0b8d2230b00f822f6722804cf08" \
    && ZLIB_URL="https://archive.archlinux.org/packages/z/zlib/zlib-1%3A1.2.11-3-x86_64.pkg.tar.xz" \
    && ZLIB_SHA256=17aede0b9f8baa789c5aa3f358fbf8c68a5f1228c5e6cba1a5dd34102ef4d4e5 \
    && curl -LfsS https://alpine-pkgs.sgerrand.com/sgerrand.rsa.pub -o /etc/apk/keys/sgerrand.rsa.pub \
    && SGERRAND_RSA_SHA256="823b54589c93b02497f1ba4dc622eaef9c813e6b0f0ebbb2f771e32adf9f4ef2" \
    && echo "${SGERRAND_RSA_SHA256} */etc/apk/keys/sgerrand.rsa.pub" | sha256sum -c - \
    && curl -LfsS ${ALPINE_GLIBC_REPO}/${GLIBC_VER}/glibc-${GLIBC_VER}.apk > /tmp/glibc-${GLIBC_VER}.apk \
    && apk add --no-cache /tmp/glibc-${GLIBC_VER}.apk \
    && curl -LfsS ${ALPINE_GLIBC_REPO}/${GLIBC_VER}/glibc-bin-${GLIBC_VER}.apk > /tmp/glibc-bin-${GLIBC_VER}.apk \
    && apk add --no-cache /tmp/glibc-bin-${GLIBC_VER}.apk \
    && curl -Ls ${ALPINE_GLIBC_REPO}/${GLIBC_VER}/glibc-i18n-${GLIBC_VER}.apk > /tmp/glibc-i18n-${GLIBC_VER}.apk \
    && apk add --no-cache /tmp/glibc-i18n-${GLIBC_VER}.apk \
    && /usr/glibc-compat/bin/localedef --force --inputfile POSIX --charmap UTF-8 "$LANG" || true \
    && echo "export LANG=$LANG" > /etc/profile.d/locale.sh \
    && curl -LfsS ${GCC_LIBS_URL} -o /tmp/gcc-libs.tar.xz \
    && echo "${GCC_LIBS_SHA256} */tmp/gcc-libs.tar.xz" | sha256sum -c - \
    && mkdir /tmp/gcc \
    && tar -xf /tmp/gcc-libs.tar.xz -C /tmp/gcc \
    && mv /tmp/gcc/usr/lib/libgcc* /tmp/gcc/usr/lib/libstdc++* /usr/glibc-compat/lib \
    && strip /usr/glibc-compat/lib/libgcc_s.so.* /usr/glibc-compat/lib/libstdc++.so* \
    && curl -LfsS ${ZLIB_URL} -o /tmp/libz.tar.xz \
    && echo "${ZLIB_SHA256} */tmp/libz.tar.xz" | sha256sum -c - \
    && mkdir /tmp/libz \
    && tar -xf /tmp/libz.tar.xz -C /tmp/libz \
    && mv /tmp/libz/usr/lib/libz.so* /usr/glibc-compat/lib \
    && apk del --purge .build-deps glibc-i18n \
    && rm -rf /tmp/*.apk /tmp/gcc /tmp/gcc-libs.tar.xz /tmp/libz /tmp/libz.tar.xz /var/cache/apk/* \
# Except for this one, which is necessary to start the container with podman.
    && mkdir $WORKING_DIRECTORY \
# And this is just for debugging purposes.
    && ls -l -R $INSTALL_DIRECTORY
WORKDIR $WORKING_DIRECTORY
ENTRYPOINT $INSTALL_DIRECTORY/robozonky.sh @$CONFIG_DIRECTORY/robozonky.cli
