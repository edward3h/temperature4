FROM eclipse-temurin:23-jre-alpine
LABEL authors="edward3h"

RUN mkdir "/config" && mkdir "/data"
COPY app/build/install/app /
ENV JAVA_OPTS=--enable-preview
WORKDIR /config
CMD ["/bin/app"]