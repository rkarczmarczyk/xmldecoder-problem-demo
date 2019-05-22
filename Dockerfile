FROM openjdk:11-slim

COPY target/xmldecoder-*.jar xmldecoder.jar
COPY src/main/resources/files_to_load /opt/files_to_load

ENTRYPOINT [ "java","-jar", "/xmldecoder.jar"]
