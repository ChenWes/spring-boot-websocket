FROM openjdk:8-jdk-alpine
MAINTAINER WesChen(chenxuhua0530@163.com)

VOLUME /tmp
COPY target/demo-0.0.1-SNAPSHOT.jar ./

#设置变量
ENV port=""

#公开端口
EXPOSE 8082
EXPOSE 12345

#设置启动命令
ENTRYPOINT ["sh","-c","java -jar demo-0.0.1-SNAPSHOT.jar "]