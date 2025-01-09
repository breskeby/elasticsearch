FROM alpine:3.21.0

# Install Java 8 SDK
RUN apk --no-cache add bash openjdk8

RUN addgroup -S elasticsearch && adduser -S elasticsearch -G elasticsearch
USER elasticsearch

# Set JAVA_HOME environment variable
ENV JAVA_HOME=/usr/lib/jvm/java-1.8-openjdk
ENV PATH="$JAVA_HOME/bin:${PATH}"

COPY --chown=elasticsearch:elasticsearch elasticsearch /opt/elasticsearch

ENV ELASTIC_CONTAINER=true

# Verify the Java installation
CMD ["/opt/elasticsearch/bin/elasticsearch"]
