FROM openjdk:alpine
WORKDIR /opt/docker
ADD target/docker/stage/opt /opt
RUN adduser -D -u 1000 drt-admin

RUN ["chown", "-R", "1000:1000", "."]

RUN apk --update add bash less
RUN rm -rf /var/cache/apk/*

USER 1000

ENTRYPOINT ["bin/drt-optimiser"]
