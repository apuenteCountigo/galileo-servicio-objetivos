FROM openjdk:17-alpine
VOLUME /tmp
ADD ./servicio-objetivos.jar servicio-objetivos.jar
ENTRYPOINT ["java","-jar","/servicio-objetivos.jar"]
