FROM docker.io/eclipse-temurin:23-jre

WORKDIR /opt/network-tester

ADD target/network-tester.jar /opt/network-tester/

ENTRYPOINT ["java", "-jar", "network-tester.jar"]
