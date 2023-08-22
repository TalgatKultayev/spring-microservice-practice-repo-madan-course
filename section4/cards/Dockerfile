FROM openjdk:17-alpine as build

COPY target/cards-0.0.1-SNAPSHOT.jar cards-0.0.1-SNAPSHOT.jar

ENTRYPOINT ["java", "-jar" , "/cards-0.0.1-SNAPSHOT.jar"]