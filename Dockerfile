FROM eclipse-temurin:23-jre-alpine
LABEL authors="edward3h"

RUN mkdir "/config" && mkdir "/data"
COPY app/build/install/app /
ENV JAVA_OPTS="--enable-preview --add-exports java.base/jdk.internal.misc=ALL-UNNAMED"
WORKDIR /config
EXPOSE 8080
CMD ["/bin/app"]