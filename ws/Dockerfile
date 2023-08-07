FROM amazoncorretto:17-alpine

EXPOSE 80

RUN mkdir /app

COPY target/*.jar /app/app.jar

ENTRYPOINT exec java $JAVA_OPTS -Djava.security.edg=file:/dev/./urandom -jar /app/app.jar
