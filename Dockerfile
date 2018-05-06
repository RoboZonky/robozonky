FROM perlur/centos-base

ENV SERVICE_NAME "RoboZonky"
ENV ROBOZONKY_VERSION "4.4.0-SNAPSHOT"

COPY . /usr/src/robozonky
WORKDIR /usr/src/robozonky

RUN yum install -y maven bzip2
RUN ["mvn", "-X", "clean", "install", "-Dgpg.skip"]
# RUN mvn org.apache.maven.plugins:maven-help-plugin:2.1.1:evaluate -Dexpression=project.version | grep -v '\['
RUN mkdir -p /opt/RoboZonky && tar -xvf /usr/src/robozonky/robozonky-app/target/robozonky-app-${ROBOZONKY_VERSION}-dist.tar.bz2 -C /opt/RoboZonky
WORKDIR /opt/RoboZonky
# Remove RoboZonky source code, image is too large when we keep the source code
RUN rm -rf /usr/src/robozonky

VOLUME /etc/RoboZonky/
#ENTRYPOINT ["/usr/local/bin/docker-entrypoint.sh"]
CMD ["/opt/RoboZonky/robozonky.sh", "-c /etc/RoboZonky/robozonky-strategy.cfg"]